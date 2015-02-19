package org.halfway.grapple.util;

import com.google.common.base.Verify;
import org.halfway.grapple.model.IORuntimeException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

/**
 * A lock for a directory that will exclude any other Grapple instances from trying to write to the directory while
 * it is locked.
 */
public class DirectoryUpdateLock {
    /**
     * Name of the update lock on the file system.
     */
    public static final String NAME = "grapple.lock";
    /**
     * The suffix of the temporary directory rename
     */
    public static final String RENAME_SUFFIX = ".1";

    private final File directory;
    private final File file;
    private final FileChannel lockFileChannel;
    private final boolean onWindows;
    private FileLock lock;

    /**
     * @param onWindows Flag that enables directory renaming as a test to check if the directory is in use. The primary
     *                  use-case is Windows, which does not support easy checking if something is in use and certainly
     *                  does not support modifying an application's underlying files while it is running.
     */
    DirectoryUpdateLock(final File directory, final boolean onWindows) {
        Verify.verify(directory.isDirectory(), "'%s' is not a directory", directory);
        this.directory = directory;
        this.file = new File(directory, NAME);

        if (onWindows) {
            if (file.isFile() && !file.delete()) {
                throw new IORuntimeException("Unable to remove lock file " + file + " before renaming");
            }
            renameDirectoryOrFail();
        }

        try {
            lockFileChannel = new RandomAccessFile(file, "rw").getChannel();
        } catch (final FileNotFoundException e) {
            throw new IORuntimeException("File not found", e);
        }
        this.onWindows = onWindows;
    }

    /**
     * Lock all the directory locks passed in
     *
     * @param updateLocks The directory locks
     * @return True if the locks could all be locked, false if one of the lock operations failed
     */
    public static boolean lockAll(final Iterable<DirectoryUpdateLock> updateLocks) {
        boolean result = true;
        for (final DirectoryUpdateLock lock : updateLocks) {
            result &= lock.lock();
        }
        return result;
    }

    public static void unlockAllAndDelete(final Iterable<DirectoryUpdateLock> updateLocks) {
        for (final DirectoryUpdateLock lock : updateLocks) {
            lock.unlock();
            lock.delete();
        }
    }


    /**
     * The directory already exists but the `rename` directory may not. There are a number of cases:
     * <p/>
     * <ol>
     * <li>The renamed directory is a file - this is an error</li>
     * <li>The renamed directory is not empty and the directory is empty - this renames the directory back</li>
     * <li>The renamed directory is empty - this deletes the renamed directory and proceeds with renaming</li>
     * <li>The renamed directory is not empty and the directory is not empty - this is an error</li>
     * <li>The renamed directory does not exist and the directory exists - this renames the directory twice</li>
     * </ol>
     */
    private void renameDirectoryOrFail() {
        final File renamedDirectory = new File(directory.getParent(), directory.getName() + RENAME_SUFFIX);
        if (renamedDirectory.isFile()) {
            throw new IORuntimeException("Rename directory " + renamedDirectory + " is a file");
        } else if (renamedDirectory.isDirectory()) {
            if (FileIO.isEmpty(directory)) {
                if (!directory.delete()) {
                    throw new IORuntimeException("Deleting directory " + directory + " failed");
                }
                if (!renamedDirectory.renameTo(directory)) {
                    throw new IORuntimeException("Renaming directory " + renamedDirectory + " back to " + directory + " failed");
                }
                return;
            } else if (FileIO.isEmpty(renamedDirectory)) {
                if (!renamedDirectory.delete()) {
                    throw new IORuntimeException("Deleting rename directory " + renamedDirectory + " failed");
                }
            } else {
                throw new IORuntimeException("Both " + directory + " and " + renamedDirectory + " are non-empty");
            }
        }

        if (!directory.renameTo(renamedDirectory)) {
            throw new IORuntimeException("Renaming directory " + directory + " to " + renamedDirectory + " failed");
        }
        if (!renamedDirectory.renameTo(directory)) {
            throw new IORuntimeException("Renaming directory " + renamedDirectory + " back to " + directory + " failed");
        }
    }

    /**
     * Attempt to lock the directory
     *
     * @return True if the directory was locked, false otherwise
     */
    public synchronized boolean lock() {
        if (lock != null) {
            return true;
        }

        try {
            lock = lockFileChannel.tryLock();
        } catch (final java.io.IOException e) {
            throw new IORuntimeException("Could not lock the lock file", e);
        }
        return lock != null;
    }

    public synchronized void unlock() {
        if (lock == null) {
            return;
        }

        try {
            lock.release();
        } catch (final java.io.IOException e) {
            throw new IORuntimeException("Could not release the lock file", e);
        }
        lock = null;
    }

    public synchronized boolean delete() {
        if (lock != null && lock.isValid()) {
            throw new IllegalStateException("Lock must not be valid before deletion");
        }
        try {
            lockFileChannel.close();
        } catch (IOException e) {
            throw new IORuntimeException("Could not close lock channel", e);
        }
        return file.delete();
    }

}
