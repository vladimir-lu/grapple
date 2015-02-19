package org.halfway.grapple.model;

import java.io.File;

/**
 * A {@link java.lang.RuntimeException} wrapper around {@link java.io.IOException}
 */
public class IORuntimeException extends RuntimeException {

    public IORuntimeException(final String message) {
        super(message);
    }

    public IORuntimeException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public IORuntimeException(final String message, final File file) {
        super(message + " : " + file.getAbsolutePath());
    }

}
