package org.xbib.io.compress.bgzf;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A wrapper class to provide buffered read access to a SeekableStream.  Just wrapping such a stream with
 * a BufferedInputStream will not work as it does not support seeking.  In this implementation a
 * seek call is delegated to the wrapped stream, and the buffer reset.
 */
public class SeekableBufferedStream extends SeekableStream {

    /** Little extension to buffered input stream to give access to the available bytes in the buffer. */
    private static class ExtBufferedInputStream extends BufferedInputStream {
        private ExtBufferedInputStream(final InputStream inputStream, final int i) {
            super(inputStream, i);
        }

        /** Returns the number of bytes that can be read from the buffer without reading more into the buffer. */
        int getBytesInBufferAvailable() {
            if (this.count == this.pos) return 0; // documented test for "is buffer empty"
            else return this.buf.length - this.pos;
        }
    }

    public static final int DEFAULT_BUFFER_SIZE = 512000;

    final private int bufferSize;
    final SeekableStream wrappedStream;
    ExtBufferedInputStream bufferedStream;
    long position;

    public SeekableBufferedStream(final SeekableStream stream, final int bufferSize) {
        this.bufferSize = bufferSize;
        this.wrappedStream = stream;
        this.position = 0;
        bufferedStream = new ExtBufferedInputStream(wrappedStream, bufferSize);
    }

    public SeekableBufferedStream(final SeekableStream stream) {
        this(stream, DEFAULT_BUFFER_SIZE);
    }

    @Override
    public long length() {
        return wrappedStream.length();
    }

    @Override
    public long skip(final long skipLength) throws IOException {
        if (skipLength < this.bufferedStream.getBytesInBufferAvailable()) {
            final long retval = this.bufferedStream.skip(skipLength);
            this.position += retval;
            return retval;
        } else {
            final long position = this.position + skipLength;
            seek(position);
            return skipLength;
        }
    }

    @Override
    public void seek(final long position) throws IOException {
        this.position = position;
        wrappedStream.seek(position);
        bufferedStream = new ExtBufferedInputStream(wrappedStream, bufferSize);
    }

    @Override
    public int read() throws IOException {
        int b = bufferedStream.read();
        position++;
        return b;
    }

    @Override
    public int read(final byte[] buffer, final int offset, final int length) throws IOException {
        final int nBytesRead = bufferedStream.read(buffer, offset, length);
        if (nBytesRead > 0) {
            position += nBytesRead;
        }
        return nBytesRead;
    }

    @Override
    public void close() throws IOException {
        wrappedStream.close();
    }

    @Override
    public boolean eof() throws IOException {
        return position >= wrappedStream.length();
    }

    @Override
    public String getSource() {
        return wrappedStream.getSource();
    }

    @Override
    public long position() throws IOException {
        return position;
    }
}
