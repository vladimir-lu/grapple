package org.halfway.grapple.util;

import com.google.common.io.ByteSink;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A somewhat unclean way to ensure that the byte sink always returns the same output stream.
 */
public class SingletonByteSink extends ByteSink {

    private final OutputStream outputStream;

    public SingletonByteSink(final OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    @Override
    public OutputStream openStream() throws IOException {
        return this.outputStream;
    }
}
