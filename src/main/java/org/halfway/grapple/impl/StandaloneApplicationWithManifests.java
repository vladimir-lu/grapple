package org.halfway.grapple.impl;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import org.halfway.grapple.model.GrappleAsset;
import org.halfway.grapple.model.configuration.StandaloneApplicationTarget;
import org.halfway.grapple.model.manifest.GrappleManifest;
import org.halfway.grapple.model.manifest.StandaloneManifest;

import java.io.File;

/**
 * A standalone application target with the application manifest
 */
public class StandaloneApplicationWithManifests implements TargetWithManifests<StandaloneApplicationTarget> {

    private final StandaloneApplicationTarget target;
    private final StandaloneManifest manifest;

    public StandaloneApplicationWithManifests(StandaloneApplicationTarget target, StandaloneManifest manifest) {
        this.target = target;
        this.manifest = manifest;
    }

    @Override
    public long getTotalFileSize() {
        long size = 0;
        for (GrappleAsset file : manifest.getAssets()) {
            size += file.getSize();
        }
        return size;
    }

    @Override
    public StandaloneApplicationTarget getTarget() {
        return target;
    }

    @Override
    public ImmutableList<GrappleManifest> getManifests() {
        return ImmutableList.<GrappleManifest>of(manifest);
    }

    @Override
    public File getContentRoot(final GrappleManifest manifest) {
        Verify.verify(manifest == this.manifest, "manifest should be unique");
        return target.getContentRoot();
    }
}
