package org.halfway.grapple.impl;

import com.google.common.collect.ImmutableList;
import org.halfway.grapple.model.configuration.LaunchTarget;
import org.halfway.grapple.model.manifest.GrappleManifest;

import java.io.File;

/**
 * A utility interface that combined the {@link org.halfway.grapple.model.configuration.LaunchTarget} with the manifests
 * that the target requires to launch.
 *
 * @param <Target> The concrete type of the target, which is a subclass of {@link org.halfway.grapple.model.configuration.LaunchTarget}
 */
public interface TargetWithManifests<Target extends LaunchTarget> {
    /**
     * Get the launch target
     */
    Target getTarget();

    /**
     * Get the list of manifests.
     * <p/>
     * Currently, only {@link org.halfway.grapple.model.manifest.ManifestOrApplicationType#jvm} has more than one manifest.
     */
    ImmutableList<GrappleManifest> getManifests();

    /**
     * The the total size of all the assets in each manifest in bytes
     */
    long getTotalFileSize();

    /**
     * Get the concrete content root for a manifest in the file system
     */
    File getContentRoot(GrappleManifest manifest);
}
