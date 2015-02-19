package org.halfway.grapple.stage;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Range;
import org.halfway.grapple.impl.GrapplePropertiesManifest;
import org.halfway.grapple.impl.RuntimeContext;
import org.halfway.grapple.impl.TargetWithManifests;
import org.halfway.grapple.model.GrappleFatalException;
import org.halfway.grapple.model.IORuntimeException;
import org.halfway.grapple.model.configuration.LaunchTarget;
import org.halfway.grapple.model.manifest.GrappleManifest;
import org.halfway.grapple.util.FileIO;

import java.io.File;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Stage that will write out the manifest files to the local file system. The main use of this is so that if Grapple is
 * in offline mode it can just look at the manifests stored on disk.
 */
public class WriteManifestStage implements LauncherStage {
    private static final Logger logger = Logger.getLogger(WriteManifestStage.class.getName());
    private static final Range<Integer> PROGRESS_RANGE = Range.closed(95, 97);

    @Override
    public Range<Integer> progressRange() {
        return PROGRESS_RANGE;
    }

    @Override
    public void burn(final RuntimeContext context) {
        final TargetWithManifests<LaunchTarget> target = context.getTargetWithManifest();
        context.getGuiApi().notifyProgress(progressRange().lowerEndpoint(), Optional.of("Writing out manifests..."));
        writeManifests(target);
        context.getGuiApi().notifyProgress(progressRange().upperEndpoint(), Optional.<String>absent());
    }

    private void writeManifests(final TargetWithManifests<LaunchTarget> targetWithManifests) {
        for (final GrappleManifest manifest : targetWithManifests.getManifests()) {
            final File contentRoot = targetWithManifests.getContentRoot(manifest);
            final File manifestFile = new File(contentRoot, GrapplePropertiesManifest.PROPERTIES_FILE);
            final ImmutableMap<String, String> propertyMap = GrapplePropertiesManifest.toPropertiesMap().from(manifest).build();
            final Properties properties = new Properties();
            properties.putAll(propertyMap);
            try {
                FileIO.writeProperties(manifestFile, properties, "updated by " + WriteManifestStage.class.getSimpleName());
                logger.info("Wrote manifest " + manifestFile);
            } catch (IORuntimeException e) {
                logger.log(Level.SEVERE, "Unable to write out manifest properties file " + manifestFile, e);
                throw new GrappleFatalException("Error while writing properties file");
            }
        }
    }
}
