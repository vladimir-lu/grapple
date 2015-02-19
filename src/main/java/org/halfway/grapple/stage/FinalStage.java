package org.halfway.grapple.stage;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Range;
import org.halfway.grapple.impl.JvmApplicationWithManifests;
import org.halfway.grapple.impl.RuntimeContext;
import org.halfway.grapple.impl.StandaloneApplicationWithManifests;
import org.halfway.grapple.impl.TargetWithManifests;
import org.halfway.grapple.model.GrappleAsset;
import org.halfway.grapple.model.GrappleFatalException;
import org.halfway.grapple.model.IORuntimeException;
import org.halfway.grapple.model.configuration.Configuration;
import org.halfway.grapple.model.configuration.JvmApplicationTarget;
import org.halfway.grapple.model.configuration.LaunchTarget;
import org.halfway.grapple.model.manifest.GrappleManifest;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The final stage is responsible for actually starting the application that was configured.
 */
public class FinalStage implements LauncherStage {
    private static final Logger logger = Logger.getLogger(FinalStage.class.getName());
    private static final Range<Integer> PROGRESS_RANGE = Range.closed(97, 100);
    private static final Joiner CLASSPATH_JOINER = Joiner.on(':');

    private final Configuration configuration;

    public FinalStage(final Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public Range<Integer> progressRange() {
        return PROGRESS_RANGE;
    }


    @Override
    public void burn(final RuntimeContext context) {
        final TargetWithManifests target = context.getTargetWithManifest();
        context.getGuiApi().notifyProgress(progressRange().lowerEndpoint(), Optional.of("Launching application..."));
        launch(target);
        context.getGuiApi().notifyProgress(progressRange().upperEndpoint(), Optional.<String>absent());
    }

    private void launch(final TargetWithManifests target) {
        if (target instanceof JvmApplicationWithManifests) {
            launchJvm((JvmApplicationWithManifests) target);
        } else if (target instanceof StandaloneApplicationWithManifests) {
            launchStandalone((StandaloneApplicationWithManifests) target);
        } else {
            throw new UnsupportedOperationException("Unknown launch target " + target);
        }
    }

    private void launchStandalone(final StandaloneApplicationWithManifests target) {
        logger.info("Launching standalone target " + target);
        throw new GrappleFatalException("FIXME implement");
    }

    private void launchJvm(final JvmApplicationWithManifests targetWithManifests) {
        logger.info("Launching jvm target " + targetWithManifests.getTarget());
        final ImmutableList.Builder<String> commandBuilder = ImmutableList.builder();
        final ImmutableMap.Builder<String, String> environment = ImmutableMap.builder();

        final JvmApplicationTarget target = targetWithManifests.getTarget();
        environment.put("JAVA_HOME", target.getJvmContentRoot().getAbsolutePath());
        final File java = new File(target.getJvmContentRoot(), targetWithManifests.getJvmManifest().getJavaPath());
        if (!java.canExecute()) {
            if (!java.setExecutable(true, false)) {
                throw new GrappleFatalException("Cannot execute " + java.getAbsolutePath());
            }
        }

        commandBuilder.add(java.getAbsolutePath());
        commandBuilder.addAll(target.getJvmArguments());
        addClassPath(commandBuilder, targetWithManifests.getStandaloneManifest());
        commandBuilder.add(target.getMainClass());
        commandBuilder.addAll(target.getArguments());

        final Process jvm = exec(commandBuilder.build(), environment.build(), targetWithManifests.getTarget().getContentRoot());
        // FIXME - check process output
        try {
            Thread.sleep(500);
            final int exitCode = jvm.exitValue();
            if (exitCode != 0) {
                throw new GrappleFatalException("Application exited with non-zero code " + exitCode);
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("bug");
        } catch (final IllegalThreadStateException e) {
            logger.log(Level.FINE, "application not exited yet - done", e);
        }
    }

    private void addClassPath(final ImmutableList.Builder<String> commandBuilder, final GrappleManifest manifest) {
        // FIXME - this needs to be an option in org.halfway.grapple.model.configuration.Configuration
        final ImmutableList.Builder<String> entriesBuilder = ImmutableList.builder();
        for (final GrappleAsset asset : manifest.getAssets()) {
            if (asset.getPath().endsWith(".jar")) {
                entriesBuilder.add(asset.getPath());
            }
        }
        final ImmutableList<String> entries = entriesBuilder.build();
        if (entries.size() > 0) {
            commandBuilder.add("-cp");
            commandBuilder.add(CLASSPATH_JOINER.join(entries));
        }
    }

    private Process exec(final ImmutableList<String> commandAndArguments, final ImmutableMap<String, String> environment,
                         final File cwd) {
        logger.info("Executing " + commandAndArguments + " with environment " + environment + " in " + cwd);

        final ProcessBuilder builder = new ProcessBuilder(commandAndArguments);
        builder.environment().clear();
        builder.environment().putAll(environment);
        try {
            return builder.directory(cwd).start();
        } catch (IOException e) {
            throw new IORuntimeException("Unable to execute process", e);
        }
    }
}
