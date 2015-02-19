package org.halfway.grapple.util;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.hash.Hasher;
import com.google.common.io.ByteSink;
import com.google.common.io.Files;
import org.halfway.grapple.model.IORuntimeException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilities for working with files to complement {@link com.google.common.io.Files}
 */
public class FileIO {
    private static final Logger logger = Logger.getLogger(FileIO.class.getSimpleName());

    private static void verifyCanWriteFile(final File file) {
        Verify.verify(!file.isDirectory(), "'%s' is a directory, not a file", file);
    }

    /**
     * Reads a {@link java.util.Properties} from a file.
     *
     * @param propertiesFile The file to read from
     * @return The properties loaded from the file
     * @throws org.halfway.grapple.model.IORuntimeException if an {@link java.io.IOException} occurs
     */
    public static Properties readProperties(final File propertiesFile) {
        Verify.verify(propertiesFile.isFile(), "'%s' is not a file", propertiesFile);
        final Properties properties = new Properties();
        InputStream inputStream = null;
        try {
            inputStream = Files.asByteSource(propertiesFile).openStream();
            properties.load(inputStream);
        } catch (final IOException e) {
            throw new IORuntimeException("Failed to load properties", e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (final IOException e) {
                    logger.log(Level.SEVERE, "Unable to close input stream", e);
                }
            }
        }
        return properties;
    }

    /**
     * Writes a {@link java.util.Properties} into a file.
     *
     * @param file       The file to write to
     * @param properties The properties to write
     * @param comments   The comments section of the properties to write
     * @throws org.halfway.grapple.model.IORuntimeException if an {@link java.io.IOException} occurs
     */
    public static void writeProperties(final File file, final Properties properties, final String comments) {
        verifyCanWriteFile(file);
        OutputStream outputStream = null;
        try {
            outputStream = Files.asByteSink(file).openStream();
            properties.store(outputStream, comments);
        } catch (final IOException e) {
            throw new IORuntimeException("Failed to write properties", e);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (final IOException e) {
                    logger.log(Level.SEVERE, "Unable to close output stream", e);
                }
            }
        }
    }

    /**
     * Finds all files in the directory and subdirectories of the directory.
     *
     * @param directory The directory to list files in
     * @return The list of all files found
     */
    public static ImmutableList<File> findInDirectory(final File directory) {
        Verify.verify(directory.isDirectory(), "'%s' is not a directory", directory);
        final List<File> files = Lists.newArrayList();
        final Stack<File> directories = new Stack<File>();
        directories.push(directory);

        while (directories.size() > 0) {
            final File currentDirectory = directories.pop();
            final File[] inDirectory = Verify.verifyNotNull(currentDirectory.listFiles(),
                    "Bug: '%s' should be a directory but was null", currentDirectory);

            for (final File file : inDirectory) {
                if (file.isDirectory()) {
                    directories.push(file);
                } else {
                    files.add(file);
                }
            }
        }
        return ImmutableList.copyOf(files);
    }

    /**
     * Create a directory lock that is going to be used for updating the directory
     *
     * @param directory The directory to create the update lock in
     * @return The directory lock
     */
    public static DirectoryUpdateLock directoryUpdateLock(final File directory, final boolean onWindows) {
        return new DirectoryUpdateLock(directory, onWindows);
    }

    /**
     * Create a relative path going from a root to a file.
     * <p/>
     * Implemented here because java.nio.files.Path#relativize is a 1.7 feature
     *
     * @param root The directory root
     * @param file The file inside the directory root
     * @return The relative path as a string
     * @throws org.halfway.grapple.model.IORuntimeException if there was a problem relativizing
     */
    public static String relativize(final File root, final File file) {
        Verify.verify(root.isDirectory(), "root '%s' is not a directory", root);
        Verify.verify(file.isFile(), "file '%s' is not a file", file);

        final String absoluteRoot = root.getAbsolutePath();
        final String absoluteFile = file.getAbsolutePath();

        if (!absoluteFile.startsWith(absoluteRoot)) {
            throw new IORuntimeException("Unable to relativize path between " + absoluteRoot +
                    " and " + absoluteFile);
        }
        return absoluteFile.substring(absoluteRoot.length() + 1, absoluteFile.length());
    }

    /**
     * Create a new file sink that will update the hash code when the file is written to
     *
     * @param hasher The hasher instance to update
     * @param file   The file that will be used as the sink
     * @return A new byte sink
     */
    public static ByteSink asHashedFileSink(final Hasher hasher, final File file) {
        verifyCanWriteFile(file);
        return new AsHashedByteSink(hasher, Files.asByteSink(file));
    }

    /**
     * Transform a file into a URL
     *
     * @param file The file to transform to a URL
     * @return The URL representing the path to the file
     * @throws org.halfway.grapple.model.IORuntimeException when the transformation fails because the URL is malformed
     */
    public static URL toUrl(final File file) {
        try {
            return file.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new IORuntimeException("Unable to transform file to url", e);
        }
    }

    /**
     * Check if a directory is empty. A directory is empty when it does not exist
     *
     * @param directory The file representing the directory
     * @return true if the directory does not exist or contains zero files
     * @throws com.google.common.base.VerifyException if the file passed exists but is not a directory
     */
    public static boolean isEmpty(final File directory) {
        if (!directory.exists()) {
            return true;
        }
        Verify.verify(directory.isDirectory(), "directory must be a directory");
        return directory.list().length == 0;
    }
}
