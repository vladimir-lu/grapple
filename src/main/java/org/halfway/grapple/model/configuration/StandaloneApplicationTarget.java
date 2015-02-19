package org.halfway.grapple.model.configuration;

import com.google.common.base.MoreObjects;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;

import java.io.File;
import java.net.URL;

/**
 * A launch target for a standalone application.
 */
public class StandaloneApplicationTarget implements LaunchTarget {
    private final File contentRoot;
    private final ImmutableList<URL> baseUrlList;
    private final String command;
    private final ImmutableList<String> arguments;

    public StandaloneApplicationTarget(final File contentRoot, final ImmutableList<URL> baseUrlList, final String command,
                                       final ImmutableList<String> arguments) {
        Verify.verifyNotNull(contentRoot, "content root must not be null");
        Verify.verifyNotNull(baseUrlList, "base url list must not be null");
        Verify.verifyNotNull(command, "command must not be null");
        Verify.verifyNotNull(arguments, "arguments must not be null");

        this.contentRoot = contentRoot;
        this.baseUrlList = baseUrlList;
        this.command = command;
        this.arguments = arguments;
    }

    @Override
    public File getContentRoot() {
        return contentRoot;
    }

    @Override
    public ImmutableList<URL> getBaseUrlList() {
        return baseUrlList;
    }

    public ImmutableList<String> getArguments() {
        return arguments;
    }

    @Override
    public ImmutableList<File> getContentRoots() {
        return ImmutableList.of(contentRoot);
    }

    public String getCommand() {
        return command;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper("StandaloneApplicationTarget")
                .add("contentRoot", contentRoot)
                .add("baseUrlList", baseUrlList)
                .add("command", command)
                .add("arguments", arguments)
                .toString();
    }
}
