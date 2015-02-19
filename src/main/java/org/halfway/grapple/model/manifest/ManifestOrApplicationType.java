package org.halfway.grapple.model.manifest;

/**
 * Enumeration for both the type of manifest ({@link org.halfway.grapple.model.manifest.GrappleManifest}) and the
 * type of launch target ({@link org.halfway.grapple.model.configuration.LaunchTarget}.
 * </p>
 * Note that even though the constants are the same, a jvm application contains two manifests and only one of these is a
 * jvm type manifest.
 */
public enum ManifestOrApplicationType {
    jvm,
    std
}
