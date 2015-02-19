package org.halfway.grapple.model.manifest;

import com.google.common.collect.ImmutableList;
import org.halfway.grapple.model.GrappleAsset;

/**
 * A manifest describes the content of the application; the {@link org.halfway.grapple.model.configuration.LaunchTarget}
 * the identity
 */
public interface GrappleManifest {

    /**
     * Return the type of manifest
     */
    ManifestOrApplicationType getManifestType();

    /**
     * Return the list of assets included in the manifest
     */
    ImmutableList<GrappleAsset> getAssets();

    /**
     * Return the hashing algorithm used to generate/verify the contents of the assets
     */
    ManifestHashAlgorithm getHashAlgorithm();
}
