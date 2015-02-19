package org.halfway.grapple.stage;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.*;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hasher;
import com.google.common.io.ByteSink;
import com.google.common.util.concurrent.*;
import org.halfway.grapple.gui.GrappleGuiApi;
import org.halfway.grapple.impl.*;
import org.halfway.grapple.model.GrappleAsset;
import org.halfway.grapple.model.GrappleFatalException;
import org.halfway.grapple.model.IORuntimeException;
import org.halfway.grapple.model.configuration.Configuration;
import org.halfway.grapple.model.configuration.LaunchTarget;
import org.halfway.grapple.model.manifest.GrappleManifest;
import org.halfway.grapple.util.*;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The update stage verifies and downloads files as necessary.
 */
public class UpdateStage implements LauncherStage {
    private static final Logger logger = Logger.getLogger(UpdateStage.class.getName());
    private static final Range<Integer> PROGRESS_RANGE = Range.closed(5, 95);
    private static final int PROGRESS_DELETE_EXTRA = PROGRESS_RANGE.lowerEndpoint() + 2;
    private static final int PROGRESS_MKDIRS = PROGRESS_RANGE.lowerEndpoint() + 4;
    private static final int PROGRESS_DOWNLOAD_START = PROGRESS_RANGE.lowerEndpoint() + 5;
    private static final ImmutableSet<String> IGNORE_DELETE = ImmutableSet.of(GrapplePropertiesManifest.PROPERTIES_FILE, DirectoryUpdateLock.NAME);
    private static final String THREAD_NAME_FORMAT = "update-stage-%1$s";
    private static final String SINGLE_THREAD_NAME_FORMAT = "update-callback-%1$s";

    private final Configuration configuration;
    private final UrlDownloader urlDownloader;
    private final ExecutorServiceBuilder executorServiceBuilder;
    private final ExecutorService singleThreadExecutor;

    public UpdateStage(final Configuration configuration, final UrlDownloader urlDownloader) {
        this.configuration = configuration;
        this.urlDownloader = urlDownloader;
        this.executorServiceBuilder = new ExecutorServiceBuilder()
                .withThreadFactoryBuilder(Optional.of(new ThreadFactoryBuilder().setNameFormat(THREAD_NAME_FORMAT)))
                .withPoolSize(configuration.getThreadPoolSize());
        this.singleThreadExecutor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat(SINGLE_THREAD_NAME_FORMAT)
                .build());
    }

    @Override
    public Range<Integer> progressRange() {
        return PROGRESS_RANGE;
    }


    private ImmutableMap<File, ImmutableMap<String, Long>> getFilesUnderContentRoots(final ImmutableList<File> contentRoots) {
        final ImmutableMap.Builder<File, ImmutableMap<String, Long>> map = ImmutableMap.builder();
        for (final File contentRoot : contentRoots) {
            final ImmutableMap.Builder<String, Long> fileSizeMap = ImmutableMap.builder();
            for (final File file : FileIO.findInDirectory(contentRoot)) {
                final String path = FileIO.relativize(contentRoot, file);
                final long size = file.length();
                fileSizeMap.put(path, size);
            }
            map.put(contentRoot, fileSizeMap.build());
        }
        return map.build();
    }

    private PercentageCounter guiPercentageCounter(final GrappleGuiApi guiApi, final long totalSize) {
        final long lower = PROGRESS_DOWNLOAD_START;
        final double scale = (double) (PROGRESS_RANGE.upperEndpoint() - lower) / 100.0d;
        final AtomicInteger currentProgress = new AtomicInteger(0);
        return new PercentageCounter(totalSize, new Function<Double, Object>() {
            @Override
            public synchronized Object apply(Double newPercentage) {
                final int newProgress = (int) (lower + Math.round(100.0d * scale * newPercentage));
                final int oldProgress = currentProgress.getAndSet(newProgress);
                if (oldProgress > newProgress) {
                    logger.warning("BUG: old progress percentage " + oldProgress + " > " + newProgress);
                } else {
                    guiApi.notifyProgress(newProgress, Optional.<String>absent());
                }
                return null;
            }
        });
    }

    private void update(final RuntimeContext context) {
        final TargetWithManifests<LaunchTarget> targetWithManifests = context.getTargetWithManifest();
        final ImmutableMap<File, URL> baseUrlMap = context.getBaseUrlMap();
        final long totalSize = targetWithManifests.getTotalFileSize();
        logger.info("Manifest(s) specify " + totalSize + " bytes in total");

        final ImmutableMap<File, GrappleManifest> manifestMap = buildContentRootManifestMap(targetWithManifests);
        final ImmutableMap<File, ImmutableMap<String, Long>> manifestSizeMap = buildManifestSizeMap(manifestMap);
        final ImmutableMap<File, ImmutableMap<String, Long>> currentSizeMap = getFilesUnderContentRoots(
                targetWithManifests.getTarget().getContentRoots());

        final ImmutableMap<File, MapDifference<String, Long>> manifestCurrentDifferenceMap = computeMapDifference(manifestSizeMap, currentSizeMap);

        context.getGuiApi().notifyProgress(PROGRESS_DELETE_EXTRA, Optional.<String>absent());
        deleteExtraFiles(manifestCurrentDifferenceMap);

        context.getGuiApi().notifyProgress(PROGRESS_MKDIRS, Optional.<String>absent());
        createMissingDirectories(manifestMap);

        context.getGuiApi().notifyProgress(PROGRESS_DOWNLOAD_START, Optional.<String>absent());
        verifyAndDownloadFiles(context.getGuiApi(), baseUrlMap, totalSize, manifestMap, manifestCurrentDifferenceMap);
    }

    private void verifyAndDownloadFiles(final GrappleGuiApi guiApi, final ImmutableMap<File, URL> baseUrlMap,
                                        final long totalSize, final ImmutableMap<File, GrappleManifest> manifestMap,
                                        final ImmutableMap<File, MapDifference<String, Long>> manifestCurrentDifferenceMap) {
        final PercentageCounter counter = guiPercentageCounter(guiApi, totalSize);
        final AtomicBoolean stepFailed = new AtomicBoolean(false);
        final ListeningExecutorService service = executorServiceBuilder.newListeningExecutorService();
        try {
            final ImmutableMap<File, ImmutableList<ListenableFuture<AssetVerificationResult>>> futureMap =
                    scheduleUpdateActions(service, manifestMap, manifestCurrentDifferenceMap, baseUrlMap);
            final ImmutableList.Builder<ListenableFuture<AssetVerificationResult>> allFuturesBuilder = ImmutableList.builder();
            for (final Map.Entry<File, ImmutableList<ListenableFuture<AssetVerificationResult>>> entry : futureMap.entrySet()) {
                final File contentRoot = entry.getKey();
                final ImmutableList<ListenableFuture<AssetVerificationResult>> resultFutures = entry.getValue();
                final FutureCallback<AssetVerificationResult> callback = makeVerificationCompleteCallback(counter, stepFailed, contentRoot);

                for (final ListenableFuture<AssetVerificationResult> resultFuture : resultFutures) {
                    Futures.addCallback(resultFuture, callback, singleThreadExecutor);
                    allFuturesBuilder.add(resultFuture);
                }
            }

            try {
                Futures.allAsList(allFuturesBuilder.build()).get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError("bug");
            } catch (ExecutionException e) {
                logger.log(Level.SEVERE, "Unknown error during update task", e);
                throw new GrappleFatalException("Update process failed for unknown reason");
            }
        } finally {
            if (!MoreExecutors.shutdownAndAwaitTermination(service, 1, TimeUnit.SECONDS)) {
                throw new GrappleFatalException("BUG: Unable to shut down executor service");
            }
        }

        if (stepFailed.get()) {
            throw new GrappleFatalException("Update process failed because one or more files failed verification");
        }
    }

    private FutureCallback<AssetVerificationResult> makeVerificationCompleteCallback(
            final PercentageCounter counter, final AtomicBoolean stepFailed, final File contentRoot) {
        return new
                FutureCallback<AssetVerificationResult>() {
                    @Override
                    public void onSuccess(final AssetVerificationResult result) {
                        switch (result.getOutcome()) {
                            case OK:
                                counter.addToTotal(result.getAsset().getSize());
                                break;
                            case HashDiffers:
                                logger.warning("Downloaded hash differs for " + result.getAsset().getPath() + " under " + contentRoot);
                                stepFailed.set(true);
                                break;
                            case SizeDiffers:
                                logger.warning("Downloaded size differs for " + result.getAsset().getPath() + " under " + contentRoot);
                                stepFailed.set(true);
                                break;
                            case MissingFile:
                                logger.warning("Asset " + result.getAsset().getPath() + " missing on server");
                                stepFailed.set(true);
                                break;
                        }
                    }

                    @Override
                    public void onFailure(final Throwable t) {
                        logger.log(Level.SEVERE, "Failed during update stage with unknown error", t);
                        stepFailed.set(true);
                    }
                };
    }

    private void createMissingDirectories(ImmutableMap<File, GrappleManifest> manifestMap) {
        for (final Map.Entry<File, GrappleManifest> entry : manifestMap.entrySet()) {
            final File contentRoot = entry.getKey();
            final GrappleManifest manifest = entry.getValue();
            for (final GrappleAsset asset : manifest.getAssets()) {
                final File file = new File(contentRoot, asset.getPath());
                final File parentDirectory = new File(file.getParent());
                if (!parentDirectory.exists()) {
                    if (!parentDirectory.mkdirs()) {
                        logger.warning("Failed to create directory " + parentDirectory);
                    }
                }
            }
        }
    }

    private ImmutableMap<File, ImmutableList<ListenableFuture<AssetVerificationResult>>> scheduleUpdateActions(
            final ListeningExecutorService service, final ImmutableMap<File, GrappleManifest> manifestMap,
            final ImmutableMap<File, MapDifference<String, Long>> fileMapDifferenceMap,
            final ImmutableMap<File, URL> baseUrlMap) {
        final ImmutableMap.Builder<File, ImmutableList<ListenableFuture<AssetVerificationResult>>> map = ImmutableMap.builder();
        for (final Map.Entry<File, GrappleManifest> entry : manifestMap.entrySet()) {
            final File contentRoot = entry.getKey();
            final GrappleManifest manifest = entry.getValue();
            final ManifestHasher hasher = ManifestHasher.fromAlgorithm(manifest.getHashAlgorithm());
            final MapDifference<String, Long> fileDifference = fileMapDifferenceMap.get(contentRoot);
            final URL baseUrl = baseUrlMap.get(contentRoot);
            final ImmutableList.Builder<ListenableFuture<AssetVerificationResult>> futuresBuilder = ImmutableList.builder();
            for (final GrappleAsset asset : manifest.getAssets()) {
                final ListenableFuture<AssetVerificationResult> resultFuture = service.submit(
                        newVerificationOrDownloadCallable(baseUrl, contentRoot, manifest, asset, hasher, fileDifference));
                futuresBuilder.add(resultFuture);
            }
            map.put(contentRoot, futuresBuilder.build());
        }
        return map.build();
    }

    private Callable<AssetVerificationResult> newVerificationOrDownloadCallable(
            final URL baseUrl, final File contentRoot, final GrappleManifest manifest, final GrappleAsset asset,
            final ManifestHasher manifestHasher, final MapDifference<String, Long> fileDifference) {
        final boolean sizeMatches = fileDifference.entriesInCommon().containsKey(asset.getPath());
        final File file = new File(contentRoot, asset.getPath());
        return new Callable<AssetVerificationResult>() {
            @Override
            public AssetVerificationResult call() throws Exception {
                if (sizeMatches) {
                    if (asset.getHash().equals(manifestHasher.hashFile(file))) {
                        return new AssetVerificationResult(manifest, asset, AssetVerificationResult.Outcome.OK);
                    } else {
                        logger.fine("H> " + asset.getPath());
                    }
                }
                if (file.exists()) {
                    logger.fine("S> " + asset.getPath());
                    if (!file.delete()) {
                        logger.warning("unable to delete " + file);
                    }
                } else {
                    logger.fine("X> " + asset.getPath());
                }

                if (!file.createNewFile()) {
                    logger.warning("created new file " + file);
                }

                final URL downloadUrl = urlDownloader.combinePath(baseUrl, asset.getPath());
                final Hasher hasher = manifestHasher.newHasher();
                final ByteSink hashedSink = FileIO.asHashedFileSink(hasher, file);
                final int responseCode = urlDownloader.httpGet(downloadUrl, hashedSink);
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    logger.warning("Unable to download file " + downloadUrl + " : HTTP " + responseCode);
                    if (!file.delete()) {
                        logger.warning("unable to delete " + file);
                    }
                    return new AssetVerificationResult(manifest, asset, AssetVerificationResult.Outcome.MissingFile);
                }

                final HashCode newHashCode = hasher.hash();
                if (asset.getHash().equals(newHashCode)) {
                    return new AssetVerificationResult(manifest, asset, AssetVerificationResult.Outcome.OK);
                } else {
                    return new AssetVerificationResult(manifest, asset, AssetVerificationResult.Outcome.HashDiffers);
                }
            }
        };
    }

    private void deleteExtraFiles(final ImmutableMap<File, MapDifference<String, Long>> manifestCurrentDifferenceMap) {
        for (final Map.Entry<File, MapDifference<String, Long>> entry : manifestCurrentDifferenceMap.entrySet()) {
            final File contentRoot = entry.getKey();
            final MapDifference<String, Long> difference = entry.getValue();
            for (final Map.Entry<String, Long> differenceEntry : difference.entriesOnlyOnRight().entrySet()) {
                final File file = new File(contentRoot, differenceEntry.getKey());
                if (!IGNORE_DELETE.contains(file.getName())) {
                    logger.info("Deleting extra file " + file + " of size " + differenceEntry.getValue());
                    if (!file.delete()) {
                        logger.warning("Unable to delete file " + file);
                    }
                }
            }
        }
    }

    private ImmutableMap<File, MapDifference<String, Long>> computeMapDifference(final ImmutableMap<File, ImmutableMap<String, Long>> manifestSizeMap,
                                                                                 final ImmutableMap<File, ImmutableMap<String, Long>> currentSizeMap) {
        final ImmutableMap.Builder<File, MapDifference<String, Long>> map = ImmutableMap.builder();
        for (final Map.Entry<File, ImmutableMap<String, Long>> manifestEntry : manifestSizeMap.entrySet()) {
            final File contentRoot = manifestEntry.getKey();
            final ImmutableMap<String, Long> manifestFileSizeMap = manifestEntry.getValue();
            final ImmutableMap<String, Long> currentFileSizeMap = currentSizeMap.get(contentRoot);
            map.put(contentRoot, Maps.difference(manifestFileSizeMap, currentFileSizeMap));
        }
        return map.build();
    }

    private ImmutableMap<File, ImmutableMap<String, Long>> buildManifestSizeMap(final ImmutableMap<File, GrappleManifest> manifestMap) {
        final ImmutableMap.Builder<File, ImmutableMap<String, Long>> map = ImmutableMap.builder();
        for (final Map.Entry<File, GrappleManifest> entry : manifestMap.entrySet()) {
            final ImmutableMap.Builder<String, Long> sizeMap = ImmutableMap.builder();
            final File contentRoot = entry.getKey();
            final GrappleManifest manifest = entry.getValue();
            for (final GrappleAsset asset : manifest.getAssets()) {
                sizeMap.put(asset.getPath(), asset.getSize());
            }
            map.put(contentRoot, sizeMap.build());
        }
        return map.build();
    }

    private ImmutableMap<File, GrappleManifest> buildContentRootManifestMap(final TargetWithManifests<LaunchTarget> targetWithManifests) {
        final ImmutableMap.Builder<File, GrappleManifest> map = ImmutableMap.builder();
        for (final GrappleManifest manifest : targetWithManifests.getManifests()) {
            map.put(targetWithManifests.getContentRoot(manifest), manifest);
        }
        return map.build();
    }

    @Override
    public void burn(final RuntimeContext context) {
        if (configuration.isOfflineMode()) {
            return;
        }
        final ImmutableMap<File, DirectoryUpdateLock> lockMap = context.getDirLockMap();
        context.getGuiApi().notifyProgress(progressRange().lowerEndpoint(), Optional.of("Updating application files..."));
        logger.info("Starting to update files in online mode");

        try {
            if (!DirectoryUpdateLock.lockAll(lockMap.values())) {
                throw new GrappleFatalException("Unable to lock directories for update");
            }
            update(context);
            logger.info("Updates complete");
        } finally {
            try {
                DirectoryUpdateLock.unlockAllAndDelete(lockMap.values());
            } catch (final IORuntimeException e) {
                logger.log(Level.SEVERE, "Unable to unlock all directories", e);
            }
        }
    }
}
