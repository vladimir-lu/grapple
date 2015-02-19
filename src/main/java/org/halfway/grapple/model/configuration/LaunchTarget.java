package org.halfway.grapple.model.configuration;

import com.google.common.collect.ImmutableList;

import java.io.File;
import java.net.URL;

/**
 * A launch target contains the identity of an application; the contents of the application are contained in the
 * {@link org.halfway.grapple.model.manifest.GrappleManifest}
 */
public interface LaunchTarget {
    /**
     * Get the root directory of installation
     */
    File getContentRoot();

    /**
     * Get the list of (HTTP) URLs that can be used to retrieve the manifest and assets from
     */
    ImmutableList<URL> getBaseUrlList();

    /**
     * Get all the content roots of the target
     */
    ImmutableList<File> getContentRoots();
}
