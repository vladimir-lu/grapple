package org.halfway.grapple.model.manifest;

/**
 * Exception that occurs when a part of the Grapple manifest fails to verify. The message is user-facing.
 */
public class ManifestVerificationException extends RuntimeException {

    public ManifestVerificationException(final String message) {
        super(message);
    }

}
