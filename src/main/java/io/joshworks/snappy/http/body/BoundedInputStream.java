package io.joshworks.snappy.http.body;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Counts bytes read from the wrapped stream and fails once the configured limit is exceeded.
 */
final class BoundedInputStream extends FilterInputStream {

    private final long maxBytes;
    private long count;

    BoundedInputStream(InputStream in, long maxBytes) {
        super(in);
        this.maxBytes = maxBytes;
    }

    @Override
    public int read() throws IOException {
        int value = super.read();
        if (value != -1) {
            increment(1);
        }
        return value;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int read = super.read(b, off, len);
        if (read > 0) {
            increment(read);
        }
        return read;
    }

    private void increment(long bytes) throws IOException {
        count += bytes;
        if (count > maxBytes) {
            throw new RequestBodyTooLargeException(maxBytes);
        }
    }
}
