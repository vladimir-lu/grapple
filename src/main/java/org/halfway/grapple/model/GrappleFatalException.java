package org.halfway.grapple.model;

import com.google.common.base.Verify;

/**
 * A fatal exception which leads to application exit and whose message is display to the user
 */
public class GrappleFatalException extends RuntimeException {

    public GrappleFatalException(final String message) {
        super(message);
        Verify.verify(message != null, "message must not be null");
    }

}
