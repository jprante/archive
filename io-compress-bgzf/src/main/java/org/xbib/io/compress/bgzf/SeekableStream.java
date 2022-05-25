package org.xbib.io.compress.bgzf;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public abstract class SeekableStream extends InputStream {

    public abstract long length();

    public abstract long position() throws IOException;

    public abstract void seek(long position) throws IOException;

    @Override
    public abstract int read(byte[] buffer, int offset, int length) throws IOException;

    @Override
    public abstract void close() throws IOException;

    public abstract boolean eof() throws IOException;

    /**
     * @return String representation of source (e.g. URL, file path, etc.), or null if not available.
     */
    public abstract String getSource();

    /**
     * Read enough bytes to fill the input buffer.
     * @param b byte array
     * @throws EOFException If EOF is reached before buffer is filled
     */
    public void readFully(byte[] b) throws IOException {
        int len = b.length;
        int n = 0;
        while (n < len) {
            int count = read(b, n, len - n);
            if (count < 0){
                throw new EOFException();
            }
            n += count;
        }
    }
}
