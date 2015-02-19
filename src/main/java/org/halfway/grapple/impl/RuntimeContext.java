package org.halfway.grapple.impl;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import org.halfway.grapple.gui.GrappleGuiApi;
import org.halfway.grapple.model.configuration.LaunchTarget;
import org.halfway.grapple.util.DirectoryUpdateLock;

import java.io.File;
import java.net.URL;

public class RuntimeContext {

    private final GrappleGuiApi guiApi;

    private Optional<ImmutableMap<File, DirectoryUpdateLock>> dirLockMap = Optional.absent();
    private Optional<ImmutableMap<File, URL>> baseUrlMap = Optional.absent();
    private Optional<TargetWithManifests<LaunchTarget>> targetWithManifest = Optional.absent();

    public RuntimeContext(final GrappleGuiApi guiApi) {
        this.guiApi = guiApi;
    }

    public GrappleGuiApi getGuiApi() {
        return guiApi;
    }

    public ImmutableMap<File, DirectoryUpdateLock> getDirLockMap() {
        return dirLockMap.get();
    }

    public void setDirLockMap(final ImmutableMap<File, DirectoryUpdateLock> lockMap) {
        this.dirLockMap = Optional.of(lockMap);
    }

    public ImmutableMap<File, URL> getBaseUrlMap() {
        return baseUrlMap.get();
    }

    public void setBaseUrlMap(final ImmutableMap<File, URL> baseUrlMap) {
        this.baseUrlMap = Optional.of(baseUrlMap);
    }

    public TargetWithManifests<LaunchTarget> getTargetWithManifest() {
        return targetWithManifest.get();
    }

    @SuppressWarnings("unchecked")
    public <Target extends LaunchTarget> void setTargetWithManifest(final TargetWithManifests<Target> targetWithManifests) {
        this.targetWithManifest = Optional.of((TargetWithManifests<LaunchTarget>) targetWithManifests);
    }
}
