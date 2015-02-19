package org.halfway.grapple.impl;

import com.google.common.base.MoreObjects;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Maps;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import com.google.common.io.Files;
import org.halfway.grapple.model.IORuntimeException;
import org.halfway.grapple.model.manifest.ManifestHashAlgorithm;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class that is able to check the integrity of manifests themselves and hash the assets inside the manifest
 */
public class ManifestHasher {
    private static final Logger logger = Logger.getLogger(ManifestHasher.class.getSimpleName());
    private static final long LOGGING_THRESHOLD_MS = 1 * 1000;

    private static final HashFunction SHA_256 = Hashing.sha256();
    private static final Charset UTF8 = Charset.forName("UTF-8");

    private final HashFunction hashFunction;

    private ManifestHasher(HashFunction hashFunction) {
        this.hashFunction = hashFunction;
    }

    public static ManifestHasher fromAlgorithm(final ManifestHashAlgorithm algorithm) {
        switch (algorithm) {
            case sha256:
                return new ManifestHasher(SHA_256);
            default:
                throw new IllegalArgumentException("Algorithm " + algorithm + " is not supported");
        }
    }

    /**
     * Verify the hash of the properties in the manifest to ensure the integrity of the manifest itself.
     * <p/>
     * To keep the hash consistent the map is sorted by key before computing the hash.
     * <p/>
     * The only key that is excluded from the hashing is the hash key
     * {@link org.halfway.grapple.impl.GrapplePropertiesManifest.Key#HASH_KEY}.
     *
     * @param manifestPropertiesMap
     * @return
     */
    public boolean verifyManifestProperties(final ImmutableMap<String, String> manifestPropertiesMap) {
        if (!manifestPropertiesMap.containsKey(GrapplePropertiesManifest.Key.HASH_KEY)) {
            throw new IllegalArgumentException("Manifest property map must contain the key " + GrapplePropertiesManifest.Key.HASH_KEY);
        }
        HashCode expected = hashManifestPropertiesExcludingHashKey(manifestPropertiesMap);
        HashCode got = HashCode.fromBytes(BaseEncoding.base16().lowerCase().decode(manifestPropertiesMap.get(GrapplePropertiesManifest.Key.HASH_KEY)));
        return expected.equals(got);
    }

    /**
     * @param manifestPropertiesMap
     * @return
     */
    public Map.Entry<String, String> hashManifestProperties(final ImmutableMap<String, String> manifestPropertiesMap) {
        if (manifestPropertiesMap.containsKey(GrapplePropertiesManifest.Key.HASH_KEY)) {
            throw new IllegalArgumentException("Manifest property map must not already contain the key " +
                    GrapplePropertiesManifest.Key.HASH_KEY);
        }
        final HashCode hashCode = hashManifestPropertiesExcludingHashKey(manifestPropertiesMap);
        return Maps.immutableEntry(GrapplePropertiesManifest.Key.HASH_KEY, hashCode.toString());
    }

    /**
     * @param file
     * @return
     */
    public HashCode hashFile(final File file) {
        try {
            final Stopwatch watch = Stopwatch.createStarted();
            HashCode code = Files.hash(file, hashFunction);
            watch.stop();
            final long took = watch.elapsed(TimeUnit.MILLISECONDS);
            logger.log(took > LOGGING_THRESHOLD_MS ? Level.FINE : Level.FINEST, "took " + watch.toString() + " to hash file " + file);
            return code;
        } catch (final IOException e) {
            throw new IORuntimeException("Failed to hash file " + file, e);
        }
    }

    private HashCode hashManifestPropertiesExcludingHashKey(final ImmutableMap<String, String> manifestPropertiesMap) {
        final Hasher hasher = hashFunction.newHasher();
        for (final Map.Entry<String, String> entry : ImmutableSortedMap.copyOf(manifestPropertiesMap).entrySet()) {
            if (GrapplePropertiesManifest.Key.HASH_KEY.equals(entry.getKey())) {
                continue;
            }
            hasher.putString(entry.getKey(), UTF8);
            hasher.putString(entry.getValue(), UTF8);
        }
        return hasher.hash();
    }

    public Hasher newHasher() {
        return hashFunction.newHasher();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper("ManifestHasher")
                .add("hashFunction", this.hashFunction)
                .toString();
    }
}
