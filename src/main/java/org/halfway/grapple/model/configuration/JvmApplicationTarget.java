package org.halfway.grapple.model.configuration;

import com.google.common.base.MoreObjects;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;

import java.io.File;
import java.net.URL;

/**
 * A launch target for a JVM application.
 */
public class JvmApplicationTarget implements LaunchTarget {

    private final File contentRoot;
    private final ImmutableList<URL> baseUrlList;
    private final String mainClass;
    private final ImmutableList<String> arguments;
    private final File jvmContentRoot;
    private final ImmutableList<URL> jvmBaseUrlList;
    private final ImmutableList<String> jvmArguments;

    public JvmApplicationTarget(final File contentRoot, final ImmutableList<URL> baseUrlList, final String mainClass,
                                final ImmutableList<String> arguments, final File jvmContentRoot,
                                final ImmutableList<URL> jvmBaseUrlList, final ImmutableList<String> jvmArguments) {
        Verify.verifyNotNull(contentRoot, "content root must not be null");
        Verify.verifyNotNull(baseUrlList, "base url list must not be null");
        Verify.verifyNotNull(mainClass, "main class must not be null");
        Verify.verifyNotNull(arguments, "arguments must not be null");
        Verify.verifyNotNull(jvmContentRoot, "jvm content root must not be null");
        Verify.verifyNotNull(contentRoot, "jvm arguments must not be null");

        this.contentRoot = contentRoot;
        this.baseUrlList = baseUrlList;
        this.mainClass = mainClass;
        this.arguments = arguments;
        this.jvmContentRoot = jvmContentRoot;
        this.jvmBaseUrlList = jvmBaseUrlList;
        this.jvmArguments = jvmArguments;
    }

    @Override
    public File getContentRoot() {
        return contentRoot;
    }

    @Override
    public ImmutableList<URL> getBaseUrlList() {
        return baseUrlList;
    }

    public String getMainClass() {
        return mainClass;
    }

    @Override
    public ImmutableList<File> getContentRoots() {
        return ImmutableList.of(contentRoot, jvmContentRoot);
    }

    public File getJvmContentRoot() {
        return jvmContentRoot;
    }

    public ImmutableList<String> getArguments() {
        return arguments;
    }

    public ImmutableList<URL> getJvmBaseUrlList() {
        return jvmBaseUrlList;
    }

    public ImmutableList<String> getJvmArguments() {
        return jvmArguments;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper("JvmApplicationTarget")
                .add("contentRoot", contentRoot)
                .add("baseUrlList", baseUrlList)
                .add("mainClass", mainClass)
                .add("arguments", arguments)
                .add("jvmContentRoot", jvmContentRoot)
                .add("jvmBaseUrlList", jvmBaseUrlList)
                .add("jvmArguments", jvmArguments)
                .toString();
    }
}
