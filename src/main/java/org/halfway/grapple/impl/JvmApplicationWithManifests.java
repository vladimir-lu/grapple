package org.halfway.grapple.impl;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.halfway.grapple.model.GrappleAsset;
import org.halfway.grapple.model.configuration.JvmApplicationTarget;
import org.halfway.grapple.model.manifest.GrappleManifest;
import org.halfway.grapple.model.manifest.JvmManifest;
import org.halfway.grapple.model.manifest.StandaloneManifest;

import java.io.File;

/**
 * A JVM application target containing the jvm and the application manifests
 */
public class JvmApplicationWithManifests implements TargetWithManifests<JvmApplicationTarget> {

    private final JvmApplicationTarget target;
    private final StandaloneManifest standaloneManifest;
    private final JvmManifest jvmManifest;
    public JvmApplicationWithManifests(final JvmApplicationTarget target, final StandaloneManifest standaloneManifest,
                                       final JvmManifest jvmManifest) {
        Verify.verifyNotNull(target, "target must not be null");
        Verify.verifyNotNull(standaloneManifest, "standalone manifest must not be null");
        Verify.verifyNotNull(jvmManifest, "jvm manifest must not be null");

        this.target = target;
        this.standaloneManifest = standaloneManifest;
        this.jvmManifest = jvmManifest;
    }

    public StandaloneManifest getStandaloneManifest() {
        return standaloneManifest;
    }

    @Override
    public long getTotalFileSize() {
        long size = 0;
        for (GrappleAsset file : Iterables.concat(standaloneManifest.getAssets(), jvmManifest.getAssets())) {
            size += file.getSize();
        }
        return size;
    }

    @Override
    public JvmApplicationTarget getTarget() {
        return target;
    }

    @Override
    public ImmutableList<GrappleManifest> getManifests() {
        return ImmutableList.of(standaloneManifest, jvmManifest);
    }

    public JvmManifest getJvmManifest() {
        return jvmManifest;
    }

    @Override
    public File getContentRoot(final GrappleManifest manifest) {
        if (standaloneManifest.equals(manifest)) {
            return target.getContentRoot();
        } else if (jvmManifest.equals(manifest)) {
            return target.getJvmContentRoot();
        } else {
            throw new IllegalArgumentException("Unknown manifest " + manifest);
        }
    }
}
