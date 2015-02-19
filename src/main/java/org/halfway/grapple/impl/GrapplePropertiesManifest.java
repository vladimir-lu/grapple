package org.halfway.grapple.impl;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import org.halfway.grapple.model.manifest.ManifestHashAlgorithm;

/**
 * Static entry point to manipulating manifests. Creates builders that go between the physical {@link java.util.Properties}
 * manifest format to the domain model {@link org.halfway.grapple.model.manifest.GrappleManifest}
 */
public class GrapplePropertiesManifest {

    public static final ManifestHashAlgorithm DEFAULT_HASH_ALGORITHM = ManifestHashAlgorithm.sha256;
    public static final String PROPERTIES_FILE = "grapple.properties";

    private GrapplePropertiesManifest() {
        // no-op
    }

    /**
     * Create a builder that goes from a {@link org.halfway.grapple.model.manifest.GrappleManifest} to a
     * {@link java.util.Properties} instance
     */
    public static PropertiesMapFromManifestBuilder toPropertiesMap() {
        return new PropertiesMapFromManifestBuilder();
    }

    /**
     * Create a builder that will create a {@link org.halfway.grapple.model.manifest.GrappleManifest} from a
     * {@link java.util.Properties} instance
     *
     * @return The builder
     */
    public static ManifestFromPropertiesMapBuilder fromPropertiesMap() {
        return new ManifestFromPropertiesMapBuilder();
    }

    /**
     * Manifest format constants and parsing constants
     */
    public static class Format {
        /**
         * Version of manifest format supported
         */
        public static final int VERSION = 1;
        /**
         * Separator of the `size:hash` entry in the manifest property value
         */
        public static final char HASH_SEPARATION_CHAR = ':';
        public static final Joiner HASH_JOINER = Joiner.on(HASH_SEPARATION_CHAR);
        public static final Splitter HASH_SPLITTER = Splitter.on(HASH_SEPARATION_CHAR).trimResults().limit(2);
        /**
         * Prefix of property keys that specify size+hash for the manifest assets
         */
        public static final String FILE_KEY_PREFIX = "file.";
    }

    /**
     * Keys present in the manifest
     */
    public static class Key {
        /**
         * Manifest version
         *
         * @see {@link org.halfway.grapple.impl.GrapplePropertiesManifest.Format#VERSION} for the version number supported
         */
        public static final String VERSION = "version";
        /**
         * Application type
         *
         * @see {@link org.halfway.grapple.model.manifest.ManifestOrApplicationType} for the names supported
         */
        public static final String APPLICATION_TYPE = "type";
        /**
         * Hash algorithm used in the manifest and asset verification
         *
         * @see {@link org.halfway.grapple.model.manifest.ManifestHashAlgorithm} for the algorithms supported
         */
        public static final String HASH_ALGORITHM = "hash.algorithm";
        /**
         * Hash of the manifest itself.
         *
         * @see {@link org.halfway.grapple.impl.ManifestHasher#verifyManifestProperties(com.google.common.collect.ImmutableMap)}
         * for currently supported values
         */
        public static final String HASH_KEY = "hash";

        /**
         * (optional; mandatory for {@link org.halfway.grapple.model.manifest.ManifestOrApplicationType#jvm} type)
         * The path to the `java` executable used when launching a jvm application
         */
        public static final String JAVA_PATH = "java.path";
    }

}
