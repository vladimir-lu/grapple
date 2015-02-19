package org.halfway.grapple;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.StandardSystemProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.halfway.grapple.impl.*;
import org.halfway.grapple.model.GrappleAsset;
import org.halfway.grapple.model.manifest.GrappleManifest;
import org.halfway.grapple.model.manifest.ManifestOrApplicationType;
import org.halfway.grapple.util.ExecutorServiceBuilder;
import org.halfway.grapple.util.FileIO;
import org.halfway.grapple.util.Logging;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Utility to create and verify grapple properties manifest files.
 *
 * @see org.halfway.grapple.ManifestTool#main(String[]) for usage
 */
public class ManifestTool {
    private static final Logger logger = Logger.getLogger(ManifestTool.class.getSimpleName());
    /**
     * Unset if an exception is required instead of a call to {@link java.lang.System#exit(int)} upon failure
     */
    private final boolean realExit;

    public ManifestTool(final ImmutableList<String> args, final boolean realExit) {
        this.realExit = realExit;

        if (args.size() == 0 || "-h".equals(args.get(0))) {
            helpAndExit(Optional.<String>absent());
        }

        final ActionType actionType;
        try {
            actionType = ActionType.valueOf(args.get(0));
        } catch (final IllegalArgumentException ae) {
            helpAndExit(Optional.of("Error: action '" + args.get(0) + "' is invalid"));
            throw new AssertionError("bug");
        }

        switch (actionType) {
            case create:
                runCreateAction(args.subList(1, args.size()));
                break;
            case verify:
                runVerifyAction(args.subList(1, args.size()));
                break;
            default:
                throw new UnsupportedOperationException("Unknown action type " + actionType);
        }
    }

    /**
     * Usage:
     * manifest create jvm /path/to/content bin/java.exe
     * manifest create std /path/to/content
     * manifest verify /path/to/content
     * manifest -h
     * <p/>
     * Options:
     * -h      Show this screen
     */
    public static void main(final String[] args) {
        Logging.initialize();
        new ManifestTool(ImmutableList.copyOf(args), true);
    }

    private void exitWithError(final String message) {
        System.err.print(message);
        if (realExit) {
            System.exit(1);
        } else {
            throw new RuntimeException("exit");
        }
    }

    private void exitWithError(final String message, final Throwable t) {
        System.err.print(message + ":" + t.getMessage());
        if (realExit) {
            System.exit(1);
        } else {
            throw new RuntimeException("exit", t);
        }
    }

    private void runCreateAction(final ImmutableList<String> args) {
        if (args.size() < 2) {
            helpAndExit(Optional.of("Error: please pass all arguments to create action"));
        }
        final String type = args.get(0);
        final String pathToContent = args.get(1);

        final ManifestOrApplicationType applicationType;
        try {
            applicationType = ManifestOrApplicationType.valueOf(type);
        } catch (final IllegalArgumentException ae) {
            helpAndExit(Optional.of("Error: application type '" + type + "' is invalid"));
            throw new AssertionError("bug");
        }

        final ExecutorServiceBuilder executorServiceBuilder = new ExecutorServiceBuilder();
        final File contentPath = new File(pathToContent);
        final File propertiesManifestFile = ensurePropertiesManifestExists(contentPath, false);

        switch (applicationType) {
            case jvm:
                if (args.size() != 3) {
                    helpAndExit(Optional.of("Error: please specify the path to java"));
                }
                final String javaPath = args.get(2);
                createManifestForJvm(propertiesManifestFile, contentPath, javaPath, executorServiceBuilder);
                break;
            case std:
                createManifestForStandalone(propertiesManifestFile, contentPath, executorServiceBuilder);
                break;
            default:
                throw new UnsupportedOperationException("Unknown application type " + applicationType);
        }
    }

    private void runVerifyAction(final ImmutableList<String> args) {
        if (args.size() != 1) {
            helpAndExit(Optional.of("Error: please pass path to verify action"));
        }

        final File contentRoot = new File(args.get(0));
        if (!contentRoot.isDirectory()) {
            exitWithError("Error: '" + contentRoot + "' is not a directory");
        }
        final ExecutorServiceBuilder executorServiceBuilder = new ExecutorServiceBuilder();
        final File propertiesManifest = ensurePropertiesManifestExists(contentRoot, true);
        final GrappleManifest grappleManifest = GrapplePropertiesManifest
                .fromPropertiesMap()
                .properties(FileIO.readProperties(propertiesManifest))
                .build();
        final ManifestHasher manifestHasher = ManifestHasher.fromAlgorithm(grappleManifest.getHashAlgorithm());
        final ManifestAssetVerifier manifestAssetVerifier = new ManifestAssetVerifier(grappleManifest, contentRoot, manifestHasher);
        final ImmutableList<AssetVerificationResult> results = verifyAssets(manifestAssetVerifier, executorServiceBuilder);
        boolean fail = false;
        for (final AssetVerificationResult result : results) {
            if (result.getOutcome() != AssetVerificationResult.Outcome.OK) {
                fail = true;
                System.err.println(result.getAsset().getPath() + " failed to verify with outcome: " + result.getOutcome());
            }
        }
        if (fail) {
            exitWithError("Error: one or more files failed to verify");
        }
    }

    private void createManifestForJvm(final File manifestFile, final File contentRoot, final String javaPath,
                                      final ExecutorServiceBuilder executorServiceBuilder) {
        final File java = new File(contentRoot, javaPath);
        if (!java.isFile()) {
            exitWithError("Error: java is not a file: " + java);
        }
        final ImmutableMap<String, String> manifestPropertiesMap = newManifestBuilderFromRoot(ManifestOrApplicationType.jvm,
                contentRoot, executorServiceBuilder)
                .withJavaPath(javaPath)
                .build();
        writeManifestProperties(manifestFile, manifestPropertiesMap);
    }

    private void createManifestForStandalone(final File manifestFile, final File contentRoot,
                                             final ExecutorServiceBuilder executorServiceBuilder) {
        final ImmutableMap<String, String> manifestProperties = newManifestBuilderFromRoot(ManifestOrApplicationType.std,
                contentRoot, executorServiceBuilder)
                .build();
        writeManifestProperties(manifestFile, manifestProperties);
    }

    private void waitOneSecondForStop(final ListeningExecutorService service) {
        if (!MoreExecutors.shutdownAndAwaitTermination(service, 1, TimeUnit.SECONDS)) {
            logger.severe("Unable to shutdown executor service");
        }
    }

    private File ensurePropertiesManifestExists(final File contentRoot, final boolean shouldExist) {
        final File manifestFile = new File(contentRoot, GrapplePropertiesManifest.PROPERTIES_FILE);
        if (!shouldExist && manifestFile.exists()) {
            exitWithError("Error: The manifest file '" + manifestFile + "' already exists. Delete it and try again.");
        } else if (shouldExist && !manifestFile.exists()) {
            exitWithError("Error: The manifest file '" + manifestFile + "' does not exist");
        }
        return manifestFile;
    }

    private Callable<GrappleAsset> newFileHashComputation(final ManifestHasher manifestHasher, final File contentRoot,
                                                          final File file) {
        return new Callable<GrappleAsset>() {
            @Override
            public GrappleAsset call() throws Exception {
                return new GrappleAsset(FileIO.relativize(contentRoot, file), file.length(), manifestHasher.hashFile(file));
            }
        };
    }

    private ImmutableList<GrappleAsset> computeFileHashes(final File contentRoot, final PropertiesMapFromManifestBuilder builder,
                                                          final ExecutorServiceBuilder executorServiceBuilder) {
        final ListeningExecutorService service = executorServiceBuilder.newListeningExecutorService();
        try {
            final Iterable<File> allFiles = FileIO.findInDirectory(contentRoot);
            final Iterable<Callable<GrappleAsset>> hashComputations = Iterables.transform(allFiles,
                    new Function<File, Callable<GrappleAsset>>() {
                        @Override
                        public Callable<GrappleAsset> apply(final File file) {
                            return newFileHashComputation(builder.getManifestHasher(), contentRoot, file);
                        }
                    });

            final ListenableFuture<List<GrappleAsset>> futureFileList = Futures.allAsList(
                    ImmutableList.copyOf(ExecutorServiceBuilder.submitTasks(service, hashComputations.iterator())));

            final List<GrappleAsset> fileList;
            try {
                fileList = futureFileList.get();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError("bug");
            } catch (final ExecutionException e) {
                throw new RuntimeException("Unknown error while trying to hash files in " + contentRoot, e);
            }
            return ImmutableList.copyOf(fileList);
        } finally {
            waitOneSecondForStop(service);
        }
    }

    private ImmutableList<AssetVerificationResult> verifyAssets(final ManifestAssetVerifier verifier,
                                                                final ExecutorServiceBuilder executorServiceBuilder) {
        final ListeningExecutorService service = executorServiceBuilder.newListeningExecutorService();
        try {
            final ListenableFuture<List<AssetVerificationResult>> future = Futures.allAsList(verifier.verifyAll(service));
            try {
                final List<AssetVerificationResult> results = future.get();
                return ImmutableList.copyOf(results);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError("bug");
            } catch (final ExecutionException e) {
                throw new RuntimeException("Unknown error while trying to verify files using " + verifier, e);
            }
        } finally {
            waitOneSecondForStop(service);
        }
    }

    private PropertiesMapFromManifestBuilder newManifestBuilderFromRoot(
            final ManifestOrApplicationType applicationType, final File contentRoot, final ExecutorServiceBuilder executorServiceBuilder) {
        PropertiesMapFromManifestBuilder propertiesMapFromManifestBuilder = GrapplePropertiesManifest.
                toPropertiesMap().
                type(applicationType);
        return propertiesMapFromManifestBuilder.
                putApplicationFiles(computeFileHashes(contentRoot, propertiesMapFromManifestBuilder, executorServiceBuilder));
    }

    private String generateManifestComments() {
        String hostNameApproximation;
        try {
            hostNameApproximation = InetAddress.getLocalHost().getHostName();
        } catch (final UnknownHostException e) {
            hostNameApproximation = "localhost";
        }
        return "Auto-generated by " + StandardSystemProperty.USER_NAME.value() + " on host " + hostNameApproximation +
                " (" + StandardSystemProperty.OS_NAME.value() + ") by " + getClass().getSimpleName();
    }

    private void writeManifestProperties(final File manifestFile, final ImmutableMap<String, String> manifestProperties) {
        final Properties properties = new Properties();
        properties.putAll(manifestProperties);
        FileIO.writeProperties(manifestFile, properties, generateManifestComments());
        System.out.println(manifestFile.getAbsolutePath());
    }


    private void helpAndExit(Optional<String> message) {
        final String usage = "Usage:\n" +
                "   manifest create jvm /path/to/content bin/java.exe\n" +
                "   manifest create std /path/to/content\n" +
                "   manifest verify /path/to/content\n" +
                "   manifest -h\n" +
                "\n" +
                " Options:\n" +
                "   -h          Show this screen";
        if (message.isPresent()) {
            System.err.println(message.get());
        }
        exitWithError(usage);
    }

    private static enum ActionType {
        create,
        verify,
    }

}
