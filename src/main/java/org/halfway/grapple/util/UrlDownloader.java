package org.halfway.grapple.util;

import com.google.common.base.Optional;
import com.google.common.base.Verify;
import com.google.common.io.ByteSink;
import com.google.common.io.CharSink;
import com.google.common.io.CharStreams;
import org.halfway.grapple.model.IORuntimeException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A utility class for downloading content using HTTP
 */
public class UrlDownloader {
    private static final String UTF8 = "UTF-8";
    private static final Logger logger = Logger.getLogger(UrlDownloader.class.getSimpleName());

    public UrlDownloader() {
        // no-op
    }

    private HttpURLConnection needHttpConnection(final URL url, final URLConnection connection) {
        Verify.verify(connection instanceof HttpURLConnection,
                "connection for url '%s' should be a http connection", url);
        return (HttpURLConnection) connection;
    }

    /**
     * @see <a href="https://docs.oracle.com/javase/6/docs/technotes/guides/net/http-keepalive.html">HTTP Persistent Connections</a>
     */
    public int httpGet(final URL url, final ByteSink sink, final CharSink errorSink) {
        Verify.verifyNotNull(url, "url must not be null");
        Verify.verifyNotNull(sink, "sink must not be null");
        Verify.verifyNotNull(errorSink, "error sink must not be null");

        HttpURLConnection connection = null;
        try {
            connection = needHttpConnection(url, url.openConnection());
            final InputStream inputStream = connection.getInputStream();
            final int responseCode = connection.getResponseCode();
            sink.writeFrom(inputStream);
            inputStream.close();
            return responseCode;
        } catch (final IOException e) {
            try {
                if (connection == null) {
                    throw new IORuntimeException("Unable to create connection to url " + url, e);
                }
                final int responseCode = connection.getResponseCode();
                final InputStream errorStream = connection.getErrorStream();
                final Optional<String> encoding = Optional.fromNullable(connection.getContentEncoding());
                errorSink.writeFrom(new InputStreamReader(errorStream, encoding.or(UTF8)));
                errorStream.close();
                return responseCode;
            } catch (final IOException ex) {
                logger.log(Level.SEVERE, "IO exception while handling HTTP GET error case", e);
                throw new IORuntimeException("Unknown error while processing another error", ex);
            }
        }
    }

    /**
     * Combine the path of the given base and the new location
     *
     * @param base         Path must be a valid URL base
     * @param pathFromBase The path of the final url from the base
     * @return The combined url.
     */
    public URL combinePath(final URL base, final String pathFromBase) {
        final URL url;
        try {
            // the default behavior does not work
            url = new URL(base, base.getPath() + "/" + pathFromBase);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Malformed url when combining " + base + " with " + pathFromBase, e);
        }
        return url;
    }

    public int httpGet(final URL url, final ByteSink sink) {
        return httpGet(url, sink, NullCharSink.instance);
    }

    private static class NullCharSink extends CharSink {
        private static final NullCharSink instance = new NullCharSink();

        @Override
        public Writer openStream() throws IOException {
            return CharStreams.nullWriter();
        }
    }

}
