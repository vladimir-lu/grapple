package org.halfway.grapple.model.configuration;

import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Verify;

/**
 * Static configuration of the application that includes its launch target, the application name and several other
 * values that determine how it should be started.
 */
public class Configuration {
    private final String applicationName;
    private final LaunchTarget launchTarget;
    private final boolean offlineMode;
    private final Optional<Integer> threadPoolSize;
    private final boolean onWindows;

    /**
     * @param onWindows Controls whether special bugs that appear only on Windows must be taken into account. Currently
     *                  only affects {@link org.halfway.grapple.util.DirectoryUpdateLock}
     */
    public Configuration(final String applicationName, final LaunchTarget launchTarget, final boolean offlineMode,
                         final Optional<Integer> threadPoolSize, final boolean onWindows) {
        Verify.verifyNotNull(applicationName, "application name must not be null");
        Verify.verifyNotNull(launchTarget, "launch target must not be null");
        Verify.verifyNotNull(threadPoolSize, "thread pool size must not be null");

        this.applicationName = applicationName;
        this.launchTarget = launchTarget;
        this.offlineMode = offlineMode;
        this.threadPoolSize = threadPoolSize;
        this.onWindows = onWindows;
    }

    public boolean isOnWindows() {
        return onWindows;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public LaunchTarget getLaunchTarget() {
        return launchTarget;
    }

    public boolean isOfflineMode() {
        return offlineMode;
    }

    public Optional<Integer> getThreadPoolSize() {
        return threadPoolSize;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper("Configuration")
                .add("applicationName", applicationName)
                .add("launchTarget", launchTarget)
                .add("offlineMode", offlineMode)
                .add("threadPoolSize", threadPoolSize)
                .add("onWindows", onWindows)
                .toString();
    }
}
