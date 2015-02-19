package org.halfway.grapple.impl;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableMap;
import org.halfway.grapple.model.GrappleAsset;
import org.halfway.grapple.model.manifest.GrappleManifest;
import org.halfway.grapple.model.manifest.JvmManifest;
import org.halfway.grapple.model.manifest.ManifestOrApplicationType;

import java.util.Map;

/**
 * A builder of a manifest properties map from either the components of a manifest or an existing
 * {@link org.halfway.grapple.model.manifest.GrappleManifest}
 */
public class PropertiesMapFromManifestBuilder {
    private final ImmutableMap.Builder<String, String> mapBuilder;
    private final ManifestHasher manifestHasher;
    private ManifestOrApplicationType applicationType = null;

    PropertiesMapFromManifestBuilder() {
        this.mapBuilder = new ImmutableMap.Builder<String, String>();
        this.manifestHasher = ManifestHasher.fromAlgorithm(GrapplePropertiesManifest.DEFAULT_HASH_ALGORITHM);
    }

    public PropertiesMapFromManifestBuilder type(final ManifestOrApplicationType applicationType) {
        if (this.applicationType != null) {
            throw new IllegalArgumentException("Unable to set application type more than once");
        }
        this.applicationType = applicationType;
        return this;
    }

    public PropertiesMapFromManifestBuilder withJavaPath(final String javaPath) {
        ensureApplicationType();
        if (applicationType != ManifestOrApplicationType.jvm) {
            throw new IllegalArgumentException("Java path is not applicable for type " + applicationType);
        }
        mapBuilder.put(GrapplePropertiesManifest.Key.JAVA_PATH, javaPath);
        return this;
    }

    public PropertiesMapFromManifestBuilder putApplicationFiles(final Iterable<GrappleAsset> assets) {
        for (final GrappleAsset asset : assets) {
            String key = GrapplePropertiesManifest.Format.FILE_KEY_PREFIX + asset.getPath();
            String encodedHash = asset.getHash().toString();
            String value = GrapplePropertiesManifest.Format.HASH_JOINER.join(asset.getSize(), encodedHash);
            mapBuilder.put(key, value);
        }
        return this;
    }

    public PropertiesMapFromManifestBuilder from(final GrappleManifest manifest) {
        type(manifest.getManifestType());
        switch (manifest.getManifestType()) {
            case jvm:
                final JvmManifest jvmManifest = (JvmManifest) manifest;
                withJavaPath(jvmManifest.getJavaPath());
                break;
            case std:
                break;
            default:
                throw new UnsupportedOperationException("Unknown application type " + manifest.getManifestType());
        }
        putApplicationFiles(manifest.getAssets());
        return this;
    }

    public ImmutableMap<String, String> build() {
        putVersionProperties();
        Map.Entry<String, String> hashEntry = manifestHasher.hashManifestProperties(mapBuilder.build());
        return mapBuilder.put(hashEntry).build();
    }

    private void ensureApplicationType() {
        Verify.verifyNotNull(applicationType, "Application type must be set");
    }

    private void putVersionProperties() {
        ensureApplicationType();
        mapBuilder.put(GrapplePropertiesManifest.Key.VERSION, GrapplePropertiesManifest.Format.VERSION + "");
        mapBuilder.put(GrapplePropertiesManifest.Key.HASH_ALGORITHM, GrapplePropertiesManifest.DEFAULT_HASH_ALGORITHM.name());
        mapBuilder.put(GrapplePropertiesManifest.Key.APPLICATION_TYPE, applicationType.name());
    }

    public ManifestHasher getManifestHasher() {
        return manifestHasher;
    }
}
