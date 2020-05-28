package org.xbib.io.compress.xz;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Counts the number of bytes written to an output stream.
 * <p/>
 * The <code>finish</code> method does nothing.
 * This is <code>FinishableOutputStream</code> instead
 * of <code>OutputStream</code> solely because it allows
 * using this as the output stream for a chain of raw filters.
 */
class CountingOutputStream extends FinishableOutputStream {
    private final OutputStream out;
    private long size = 0;

    public CountingOutputStream(OutputStream out) {
        this.out = out;
    }

    public void write(int b) throws IOException {
        out.write(b);
        if (size >= 0) {
            ++size;
        }
    }

    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
        if (size >= 0) {
            size += len;
        }
    }

    public void flush() throws IOException {
        out.flush();
    }

    public void close() throws IOException {
        out.close();
    }

    public long getSize() {
        return size;
    }
}
