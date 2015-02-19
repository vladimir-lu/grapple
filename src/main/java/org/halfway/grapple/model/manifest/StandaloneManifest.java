package org.halfway.grapple.model.manifest;

import com.google.common.collect.ImmutableList;
import org.halfway.grapple.model.GrappleAsset;


public class StandaloneManifest implements GrappleManifest {

    private final ImmutableList<GrappleAsset> assets;
    private final ManifestHashAlgorithm manifestHashAlgorithm;

    public StandaloneManifest(ImmutableList<GrappleAsset> assets,
                              ManifestHashAlgorithm manifestHashAlgorithm) {
        this.assets = assets;
        this.manifestHashAlgorithm = manifestHashAlgorithm;
    }

    @Override
    public ManifestOrApplicationType getManifestType() {
        return ManifestOrApplicationType.std;
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
