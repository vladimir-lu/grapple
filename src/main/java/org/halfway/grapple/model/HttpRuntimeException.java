package org.halfway.grapple.model;

/**
 * A {@link java.lang.RuntimeException} that should only be thrown upon the detection of a non
 * {@link java.net.HttpURLConnection#HTTP_OK} code
 */
public class HttpRuntimeException extends IORuntimeException {
    public HttpRuntimeException(String message, int responseCode) {
        super(message + " : responseCode=" + responseCode);
    }

}
