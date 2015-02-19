package org.halfway.grapple.stage;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.google.common.util.concurrent.*;
import org.halfway.grapple.impl.*;
import org.halfway.grapple.model.GrappleFatalException;
import org.halfway.grapple.model.configuration.Configuration;
import org.halfway.grapple.model.configuration.LaunchTarget;
import org.halfway.grapple.model.manifest.GrappleManifest;
import org.halfway.grapple.util.ExecutorServiceBuilder;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Stage responsible for verifying the contents of the manifest without contacting any external HTTP endpoints.
 * <p/>
 * This stage will fail if:
 * <ol>
 * <li>Any of the application assets fail to verify with the manifest</li>
 * </ol>
 */
public class OfflineVerifyStage implements LauncherStage {
    private static final Logger logger = Logger.getLogger(UpdateStage.class.getName());
    private static final Range<Integer> PROGRESS_RANGE = Range.closed(5, 95);
    private static final String THREAD_NAME_FORMAT = "verify-stage-%1$s";

    private final Configuration configuration;
    private final ExecutorServiceBuilder executorServiceBuilder;

    public OfflineVerifyStage(final Configuration configuration) {
        this.configuration = configuration;
        this.executorServiceBuilder = new ExecutorServiceBuilder()
                .withThreadFactoryBuilder(Optional.of(new ThreadFactoryBuilder().setNameFormat(THREAD_NAME_FORMAT)))
                .withPoolSize(configuration.getThreadPoolSize());
    }

    @Override
    public Range<Integer> progressRange() {
        return PROGRESS_RANGE;
    }

    @Override
    public void burn(final RuntimeContext context) {
        if (!configuration.isOfflineMode()) {
            return;
        }
        logger.info("Starting to verify files in offline mode");
        context.getGuiApi().notifyProgress(progressRange().lowerEndpoint(), Optional.of("Verifying in offline mode..."));
        final TargetWithManifests<LaunchTarget> targetWithManifests = context.getTargetWithManifest();
        verifyTarget(targetWithManifests);
        context.getGuiApi().notifyProgress(progressRange().upperEndpoint(), Optional.<String>absent());
    }

    private void verifyTarget(final TargetWithManifests<LaunchTarget> targetWithManifests) {
        final ListeningExecutorService service = executorServiceBuilder.newListeningExecutorService();
        final ImmutableList.Builder<ListenableFuture<AssetVerificationResult>> allFutures = ImmutableList.builder();
        try {
            for (final GrappleManifest manifest : targetWithManifests.getManifests()) {
                final File contentRoot = targetWithManifests.getContentRoot(manifest);
                final ManifestHasher manifestHasher = ManifestHasher.fromAlgorithm(manifest.getHashAlgorithm());
                final ManifestAssetVerifier manifestAssetVerifier = new ManifestAssetVerifier(manifest, contentRoot, manifestHasher);
                final ImmutableList<ListenableFuture<AssetVerificationResult>> resultFutures = manifestAssetVerifier.verifyAll(service);
                allFutures.addAll(resultFutures);
            }
            ensureNoVerificationFailures(targetWithManifests, allFutures.build());
        } finally {
            if (!MoreExecutors.shutdownAndAwaitTermination(service, 1, TimeUnit.SECONDS)) {
                logger.warning("Executor service failed to terminate");
            }
        }
    }

    private void ensureNoVerificationFailures(final TargetWithManifests<LaunchTarget> targetWithManifests,
                                              final ImmutableList<ListenableFuture<AssetVerificationResult>> allFutures) {
        boolean failed = false;
        final ListenableFuture<List<AssetVerificationResult>> future = Futures.allAsList(allFutures);
        try {
            final List<AssetVerificationResult> results = future.get();
            for (final AssetVerificationResult result : results) {
                if (result.getOutcome() != AssetVerificationResult.Outcome.OK) {
                    failed = true;
                    final File contentRoot = targetWithManifests.getContentRoot(result.getManifest());
                    logger.warning("Asset " + result.getAsset() + " in content root " + contentRoot +
                            " failed to verify with outcome " + result.getOutcome());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("bug");
        } catch (ExecutionException e) {
            logger.log(Level.SEVERE, "Unknown exception during file verification", e);
            throw new GrappleFatalException("Unknown error during verification");
        }

        if (failed) {
            throw new GrappleFatalException("One or more files failed to verify");
        }
    }
}
