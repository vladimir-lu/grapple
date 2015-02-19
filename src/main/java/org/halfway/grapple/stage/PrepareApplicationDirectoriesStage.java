package org.halfway.grapple.stage;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Range;
import org.halfway.grapple.impl.RuntimeContext;
import org.halfway.grapple.model.IORuntimeException;
import org.halfway.grapple.model.configuration.Configuration;
import org.halfway.grapple.util.DirectoryUpdateLock;
import org.halfway.grapple.util.FileIO;

import java.io.File;

/**
 * Initial launching stage that is responsible for creating content root directories and preparing the directory locks
 * that will be used during the update stage.
 * <p/>
 * This stage will fail if:
 * <ol>
 * <li>Content root directories cannot be created</li>
 * <li>On Windows only, if the content root directories are in use</li>
 * </ol>
 */
public class PrepareApplicationDirectoriesStage implements LauncherStage {
    private static final Range<Integer> PROGRESS_RANGE = Range.closed(0, 2);

    private final Configuration configuration;

    public PrepareApplicationDirectoriesStage(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public Range<Integer> progressRange() {
        return PROGRESS_RANGE;
    }

    private void ensureContentRootIsNotAFile(File contentRoot) {
        if (contentRoot.isFile()) {
            throw new IORuntimeException("Content root was a file but must be a directory", contentRoot);
        }
    }

    private void ensureContentRootExists(File contentRoot) {
        if (!contentRoot.isDirectory()) {
            if (!contentRoot.mkdirs()) {
                throw new IORuntimeException("Failed to create all necessary directories leading up to", contentRoot);
            }
        }
    }

    @Override
    public void burn(RuntimeContext context) {
        context.getGuiApi().notifyProgress(progressRange().lowerEndpoint(), Optional.of("Preparing application directories..."));
        final ImmutableMap.Builder<File, DirectoryUpdateLock> lockMap = ImmutableMap.builder();
        for (final File contentRoot : configuration.getLaunchTarget().getContentRoots()) {
            ensureContentRootIsNotAFile(contentRoot);
            ensureContentRootExists(contentRoot);
            lockMap.put(contentRoot, FileIO.directoryUpdateLock(contentRoot, configuration.isOnWindows()));
        }

        context.setDirLockMap(lockMap.build());
        context.getGuiApi().notifyProgress(progressRange().upperEndpoint(), Optional.<String>absent());
    }
}
