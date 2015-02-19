package org.halfway.grapple.model.manifest;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import org.halfway.grapple.model.GrappleAsset;

/**
 * Manifest for a Java Virtual Machine. Crucially, includes the path to the executable that should be considered as
 * 'java' when launching applications.
 */
public class JvmManifest implements GrappleManifest {
    private static final String DEFAULT_JAVA_PATH = "bin/java";
    private final ImmutableList<GrappleAsset> assets;
    private final ManifestHashAlgorithm manifestHashAlgorithm;
    private final String javaPath;

    public JvmManifest(ImmutableList<GrappleAsset> assets, ManifestHashAlgorithm manifestHashAlgorithm,
                       Optional<String> javaPath) {
        this.assets = assets;
        this.manifestHashAlgorithm = manifestHashAlgorithm;
        this.javaPath = javaPath.or(DEFAULT_JAVA_PATH);

        checkJavaPathExists();
    }

    private void checkJavaPathExists() {
        for (final GrappleAsset asset : assets) {
            if (javaPath.equals(asset.getPath())) {
                return;
            }
        }
        throw new IllegalArgumentException("Unable to find the 'java' referenced by '" + javaPath + "'");
    }

    public String getJavaPath() {
        return javaPath;
    }

    @Override
    public ManifestOrApplicationType getManifestType() {
        return ManifestOrApplicationType.jvm;
    }

    @Override
    public ImmutableList<GrappleAsset> getAssets() {
        return assets;
    }

    @Override
    public ManifestHashAlgorithm getHashAlgorithm() {
        return manifestHashAlgorithm;
    }
}
