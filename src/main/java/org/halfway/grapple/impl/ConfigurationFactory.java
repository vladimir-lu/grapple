package org.halfway.grapple.impl;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.base.StandardSystemProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.halfway.grapple.model.configuration.Configuration;
import org.halfway.grapple.model.configuration.JvmApplicationTarget;
import org.halfway.grapple.model.configuration.LaunchTarget;
import org.halfway.grapple.model.configuration.StandaloneApplicationTarget;
import org.halfway.grapple.model.manifest.ManifestOrApplicationType;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * Standard creator of Grapple configuration. The current implementation uses system properties exclusively for
 * configuration
 */
public class ConfigurationFactory {
    /**
     * Default number of lines to include in the backtrace scrollback
     */
    public static final int DEFAULT_SCROLLBACK = 10000;
    private static final Splitter URL_SPLITTER = Splitter.on('|').trimResults().omitEmptyStrings();

    /**
     * FIXME does not handle arguments with spaces in them
     */
    private static final Splitter ARGS_SPLITTER = Splitter.on(' ');

    private ConfigurationFactory() {
        // no-op
    }

    private static String getRequiredSystemProperty(String propertyKey) {
        String value = System.getProperty(propertyKey);
        if (value == null) {
            throw new IllegalArgumentException("The system property '" + propertyKey + "' was not set");
        }
        return value;
    }

    private static Optional<String> getOptionalSystemProperty(final String propertyKey) {
        return Optional.fromNullable(System.getProperty(propertyKey));
    }

    private static ImmutableList<String> splitArguments(final Optional<String> argumentString) {
        if (!argumentString.isPresent()) {
            return ImmutableList.of();
        } else {
            return ImmutableList.copyOf(ARGS_SPLITTER.splitToList(argumentString.get()));
        }
    }

    private static ImmutableList<URL> getUrlListSystemProperty(final String propertyKey) {
        final String value = getRequiredSystemProperty(propertyKey);
        final List<URL> urlList = Lists.newArrayList();
        for (final String url : URL_SPLITTER.split(value)) {
            try {
                urlList.add(new URL(url));
            } catch (final MalformedURLException e) {
                throw new IllegalArgumentException("The system property '" + propertyKey + "' contained a malformed url", e);
            }
        }
        return ImmutableList.copyOf(urlList);
    }

    private static boolean isWindowsMode() {
        final String osName = StandardSystemProperty.OS_NAME.value();
        return osName == null || osName.toLowerCase().contains("windows");
    }

    /**
     * Create a Grapple configuration from system properties.
     *
     * @return A fully built configuration
     * @throws java.lang.IllegalArgumentException      if some required properties were not set
     * @throws java.lang.UnsupportedOperationException in the unlikely case that some enum values could not be resolved
     * @see {@link org.halfway.grapple.impl.ConfigurationFactory.Key} for the system properties that are current used
     */
    public static Configuration fromSystemProperties() {
        final ManifestOrApplicationType applicationType = ManifestOrApplicationType.valueOf(getOptionalSystemProperty(Key.APPLICATION_TYPE).
                or(ManifestOrApplicationType.jvm.name()));
        final String applicationName = getRequiredSystemProperty(Key.APPLICATION_NAME);
        final File contentRoot = new File(getRequiredSystemProperty(Key.APPLICATION_CONTENT_ROOT));
        final ImmutableList<String> applicationArguments = splitArguments(getOptionalSystemProperty(Key.APPLICATION_ARGS));
        final ImmutableList<URL> applicationUrlList = getUrlListSystemProperty(Key.APPLICATION_BASE_URL_LIST);
        final boolean offlineMode = Boolean.getBoolean(Key.OFFLINE_MODE);
        final Optional<Integer> threadPoolSize = Optional.fromNullable(Integer.getInteger(Key.THREAD_POOL_SIZE));
        final boolean windowsMode = isWindowsMode();
        final LaunchTarget launchTarget = launchTargetFromSystemProperties(applicationType, contentRoot,
                applicationArguments, applicationUrlList);
        return new Configuration(applicationName, launchTarget, offlineMode, threadPoolSize, windowsMode);
    }

    /**
     * Exposed here because this is needed to configure logging and logging should be configured before even the
     * {@link org.halfway.grapple.model.configuration.Configuration} is fully built
     *
     * @return The number of lines in the backtrace scrollback
     */
    public static int getBacktraceScrollback() {
        return Integer.getInteger(Key.BACKTRACE_SCROLLBACK, DEFAULT_SCROLLBACK);
    }

    private static LaunchTarget launchTargetFromSystemProperties(final ManifestOrApplicationType applicationType,
                                                                 final File applicationHome,
                                                                 final ImmutableList<String> applicationArguments,
                                                                 final ImmutableList<URL> applicationUrlList) {
        switch (applicationType) {
            case jvm:
                final File jvmHome = new File(getRequiredSystemProperty(Key.JVM_ROOT));
                final String mainClass = getRequiredSystemProperty(Key.JVM_APPLICATION_MAIN_CLASS);
                final ImmutableList<String> jvmArguments = splitArguments(getOptionalSystemProperty(Key.JVM_ARGUMENTS));
                final ImmutableList<URL> jvmUrlList = getUrlListSystemProperty(Key.JVM_BASE_URL_LIST);
                return new JvmApplicationTarget(applicationHome, applicationUrlList, mainClass, applicationArguments,
                        jvmHome, jvmUrlList, jvmArguments);
            case std:
                final String command = getRequiredSystemProperty(Key.STANDALONE_APPLICATION_COMMAND);
                return new StandaloneApplicationTarget(applicationHome, applicationUrlList, command, applicationArguments);
            default:
                throw new UnsupportedOperationException("Unknown application type " + applicationType);
        }
    }

    /**
     * System property keys used to make a Grapple configuration.
     */
    public static class Key {
        /**
         * The type of the application. Valid values are the names of
         * {@link org.halfway.grapple.model.manifest.ManifestOrApplicationType}
         */
        public static final String APPLICATION_TYPE = "grapple.application.type";

        /**
         * The content root of the application to launch
         */
        public static final String APPLICATION_CONTENT_ROOT = "grapple.application.root";

        /**
         * The name of the application to launch. The name is used for display purposes only
         */
        public static final String APPLICATION_NAME = "grapple.application.name";

        /**
         * Arguments for the application to launch.
         */
        public static final String APPLICATION_ARGS = "grapple.application.args";

        /**
         * List of base URLs to use for downloading updates and manifests.
         * <p/>
         * The URLs are split based on the character '|'
         */
        public static final String APPLICATION_BASE_URL_LIST = "grapple.application.urls";

        /**
         * (Optional) number of lines to include in the scrollback should the launching process fail.
         * <p/>
         * Will default to {@link org.halfway.grapple.impl.ConfigurationFactory#DEFAULT_SCROLLBACK}
         */
        public static final String BACKTRACE_SCROLLBACK = "grapple.gui.scrollback";

        /**
         * (Optional) boolean property that when set will launch the application in offline mode, expecting all assets
         * in the manifest to be downloaded.
         */
        public static final String OFFLINE_MODE = "grapple.option.offline";

        /**
         * (Optional) integer property that when set will constrain the default unbounded thread pool size that is used
         * for update and verification.
         */
        public static final String THREAD_POOL_SIZE = "grapple.option.thread-pool.size";

        /**
         * The content root of the JVM itself. This is a separate content root from the main application
         * </p>
         * Applies to {@link org.halfway.grapple.model.configuration.JvmApplicationTarget} only
         */
        public static final String JVM_ROOT = "grapple.application.jvm.root";

        /**
         * The main class of the JVM application
         * </p>
         * Applies to {@link org.halfway.grapple.model.configuration.JvmApplicationTarget} only
         */
        public static final String JVM_APPLICATION_MAIN_CLASS = "grapple.application.jvm.main-class";

        /**
         * The arguments to pass to the JVM itself
         * </p>
         * Applies to {@link org.halfway.grapple.model.configuration.JvmApplicationTarget} only
         */
        public static final String JVM_ARGUMENTS = "grapple.application.jvm.args";

        /**
         * List of base URLs to use for downloading updates and manifests.
         * </p>
         * Applies to {@link org.halfway.grapple.model.configuration.JvmApplicationTarget} only
         *
         * @see #APPLICATION_BASE_URL_LIST
         */
        public static final String JVM_BASE_URL_LIST = "grapple.application.jvm.urls";

        /**
         * The command to launch when the application is standalone
         * </p>
         * Applies to {@link org.halfway.grapple.model.configuration.StandaloneApplicationTarget} only
         */
        public static final String STANDALONE_APPLICATION_COMMAND = "grapple.application.std.command";
    }
}
