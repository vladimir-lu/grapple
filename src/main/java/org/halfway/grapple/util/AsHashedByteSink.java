package org.halfway.grapple.util;

import com.google.common.hash.Hasher;
import com.google.common.io.ByteSink;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A byte sink that wraps another byte sink and a hasher, and multiplexes the writes into the hasher and the output stream
 * generated by the passed-in byte sink
 */
public class AsHashedByteSink extends ByteSink {

    private final Hasher hasher;
    private final ByteSink byteSink;

    AsHashedByteSink(final Hasher hasher, final ByteSink byteSink) {
        this.hasher = hasher;
        this.byteSink = byteSink;
    }

    @Override
    public OutputStream openStream() throws IOException {
        return new HashingOutputStream(hasher, byteSink.openStream());
    }

    static class HashingOutputStream extends OutputStream {
        private final Hasher hasher;
        private final OutputStream outputStream;

        HashingOutputStream(final Hasher hasher, final OutputStream outputStream) {
            this.hasher = hasher;
            this.outputStream = outputStream;
        }

        @Override
        public void write(int b) throws IOException {
            this.outputStream.write(b);
            this.hasher.putInt(b);
        }

        @Override
        public void write(byte[] b) throws IOException {
            this.outputStream.write(b);
            this.hasher.putBytes(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            this.outputStream.write(b, off, len);
            this.hasher.putBytes(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            this.outputStream.flush();
        }

        @Override
        public void close() throws IOException {
            this.outputStream.close();
        }
    }
}