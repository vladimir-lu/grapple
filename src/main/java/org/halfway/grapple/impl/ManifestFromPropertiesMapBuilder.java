package org.halfway.grapple.impl;

import com.google.common.base.Optional;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.hash.HashCode;
import com.google.common.io.BaseEncoding;
import org.halfway.grapple.model.GrappleAsset;
import org.halfway.grapple.model.manifest.*;

import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Builder of {@link org.halfway.grapple.model.manifest.GrappleManifest} from a {@link java.util.Properties} file
 */
public class ManifestFromPropertiesMapBuilder {
    private ImmutableMap<String, String> manifestPropertiesMap = null;

    ManifestFromPropertiesMapBuilder() {
        // no-op
    }

    private static JvmManifest newJvmManifest(final ImmutableMap<String, String> manifestPropertiesMap,
                                              final ImmutableList<GrappleAsset> assets,
                                              final ManifestHashAlgorithm hashAlgorithm) {
        final Optional<String> javaPath = manifestPropertiesMap.containsKey(GrapplePropertiesManifest.Key.JAVA_PATH) ?
                Optional.of(manifestPropertiesMap.get(GrapplePropertiesManifest.Key.JAVA_PATH)) : Optional.<String>absent();
        return new JvmManifest(assets, hashAlgorithm, javaPath);
    }

    private static StandaloneManifest newStdManifest(@SuppressWarnings("unused") final ImmutableMap<String, String> manifestPropertiesMap,
                                                     final ImmutableList<GrappleAsset> assets,
                                                     final ManifestHashAlgorithm hashAlgorithm) {
        return new StandaloneManifest(assets, hashAlgorithm);
    }

    public ManifestFromPropertiesMapBuilder map(final ImmutableMap<String, String> manifestPropertiesMap) {
        Verify.verify(this.manifestPropertiesMap == null, "Manifest properties map must not be set more than once");
        this.manifestPropertiesMap = manifestPropertiesMap;
        return this;
    }

    public ManifestFromPropertiesMapBuilder properties(final Properties properties) {
        return map(Maps.fromProperties(properties));
    }

    public GrappleManifest build() {
        ensurePropertiesMap();
        verifyHeaders();
        final ManifestHashAlgorithm hashAlgorithm = verifyHeadersHash();
        final ManifestOrApplicationType applicationType = verifyApplicationType();
        final ImmutableList<GrappleAsset> assets = assetsFromPropertiesMap();
        switch (applicationType) {
            case jvm:
                return newJvmManifest(manifestPropertiesMap, assets, hashAlgorithm);
            case std:
                return newStdManifest(manifestPropertiesMap, assets, hashAlgorithm);
            default:
                throw new ManifestVerificationException("Unknown application type '" + applicationType + "'");
        }
    }

    private void ensurePropertiesMap() {
        Verify.verifyNotNull(manifestPropertiesMap, "Manifest properties map must be set");
    }

    private void verifyHeaders() {
        ensurePropertiesMap();
        if (!manifestPropertiesMap.containsKey(GrapplePropertiesManifest.Key.VERSION)) {
            throw new ManifestVerificationException("Version key '" + GrapplePropertiesManifest.Key.VERSION + "' missing from manifest");
        } else if (!manifestPropertiesMap.containsKey(GrapplePropertiesManifest.Key.HASH_ALGORITHM)) {
            throw new ManifestVerificationException("Hash algorithm key '" + GrapplePropertiesManifest.Key.HASH_ALGORITHM + "' missing from manifest");
        } else if (!(GrapplePropertiesManifest.Format.VERSION + "").equals(manifestPropertiesMap.get(GrapplePropertiesManifest.Key.VERSION))) {
            throw new ManifestVerificationException("Version '" + manifestPropertiesMap.get(GrapplePropertiesManifest.Key.VERSION) + "' is not supported");
        }
    }

    private ManifestHashAlgorithm verifyHeadersHash() {
        final ManifestHashAlgorithm hashAlgorithm;
        try {
            hashAlgorithm = ManifestHashAlgorithm.valueOf(manifestPropertiesMap.get(GrapplePropertiesManifest.Key.HASH_ALGORITHM));
        } catch (final IllegalArgumentException ae) {
            throw new ManifestVerificationException("Hash algorithm " + manifestPropertiesMap.get(GrapplePropertiesManifest.Key.HASH_ALGORITHM) + " is not supported");
        }
        if (!ManifestHasher.fromAlgorithm(hashAlgorithm).verifyManifestProperties(manifestPropertiesMap)) {
            throw new ManifestVerificationException("Manifest properties had invalid hash");
        }
        return hashAlgorithm;
    }

    private ManifestOrApplicationType verifyApplicationType() {
        final ManifestOrApplicationType applicationType;
        try {
            applicationType = ManifestOrApplicationType.valueOf(manifestPropertiesMap.get(GrapplePropertiesManifest.Key.APPLICATION_TYPE));
        } catch (final IllegalArgumentException ae) {
            throw new ManifestVerificationException("Application type '" + manifestPropertiesMap.get(GrapplePropertiesManifest.Key.APPLICATION_TYPE) + "' is not supported");
        }
        return applicationType;
    }

    private ImmutableList<GrappleAsset> assetsFromPropertiesMap() {
        ensurePropertiesMap();
        final ImmutableList.Builder<GrappleAsset> assets = ImmutableList.builder();
        for (final Map.Entry<String, String> entry : manifestPropertiesMap.entrySet()) {
            if (entry.getKey().startsWith(GrapplePropertiesManifest.Format.FILE_KEY_PREFIX)) {
                final String path = entry.getKey().substring(GrapplePropertiesManifest.Format.FILE_KEY_PREFIX.length());
                final List<String> splitSizeHash = GrapplePropertiesManifest.Format.HASH_SPLITTER.splitToList(entry.getValue());

                if (splitSizeHash.size() != 2) {
                    throw new ManifestVerificationException("The entry " + path + " = '" + entry.getValue() + "' is in an incorrect format");
                }

                final int size = Integer.parseInt(splitSizeHash.get(0));
                final HashCode hash = HashCode.fromBytes(BaseEncoding.base16().lowerCase().decode(splitSizeHash.get(1)));

                assets.add(new GrappleAsset(path, size, hash));
            }
        }
        return assets.build();
    }
}
