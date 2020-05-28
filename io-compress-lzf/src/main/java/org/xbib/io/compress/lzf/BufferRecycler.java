package org.xbib.io.compress.lzf;

import java.lang.ref.SoftReference;

/**
 * Simple helper class to encapsulate details of basic buffer
 * recycling scheme, which helps a lot (as per profiling) for
 * smaller encoding cases.
 */
public final class BufferRecycler {

    private static final int MIN_ENCODING_BUFFER = 4000;

    private static final  int MIN_OUTPUT_BUFFER = 8000;

    /**
     * This <code>ThreadLocal</code> contains a {@link java.lang.ref.SoftReference}
     * to a {@link BufferRecycler} used to provide a low-cost
     * buffer recycling for buffers we need for encoding, decoding.
     */
    private final static ThreadLocal<SoftReference<BufferRecycler>> recyclerRef = new ThreadLocal<>();


    private byte[] inputBuffer;

    private byte[] outputBuffer;

    private byte[] decodingBuffer;

    private byte[] encodingBuffer;

    private int[] encodingHash;

    /**
     * Accessor to get thread-local recycler instance
     */
    public static BufferRecycler instance() {
        SoftReference<BufferRecycler> ref = recyclerRef.get();
        BufferRecycler br = (ref == null) ? null : ref.get();
        if (br == null) {
            br = new BufferRecycler();
            recyclerRef.set(new SoftReference<>(br));
        }
        return br;
    }

    public byte[] allocEncodingBuffer(int minSize) {
        byte[] buf = encodingBuffer;
        if (buf == null || buf.length < minSize) {
            buf = new byte[Math.max(minSize, MIN_ENCODING_BUFFER)];
        } else {
            encodingBuffer = null;
        }
        return buf;
    }

    public void releaseEncodeBuffer(byte[] buffer) {
        if (encodingBuffer == null || buffer.length > encodingBuffer.length) {
            encodingBuffer = buffer;
        }
    }

    public byte[] allocOutputBuffer(int minSize) {
        byte[] buf = outputBuffer;
        if (buf == null || buf.length < minSize) {
            buf = new byte[Math.max(minSize, MIN_OUTPUT_BUFFER)];
        } else {
            outputBuffer = null;
        }
        return buf;
    }

    public void releaseOutputBuffer(byte[] buffer) {
        if (outputBuffer == null || (buffer != null && buffer.length > outputBuffer.length)) {
            outputBuffer = buffer;
        }
    }

    public int[] allocEncodingHash(int suggestedSize) {
        int[] buf = encodingHash;
        if (buf == null || buf.length < suggestedSize) {
            buf = new int[suggestedSize];
        } else {
            encodingHash = null;
        }
        return buf;
    }

    public void releaseEncodingHash(int[] buffer) {
        if (encodingHash == null || (buffer != null && buffer.length > encodingHash.length)) {
            encodingHash = buffer;
        }
    }

    public byte[] allocInputBuffer(int minSize) {
        byte[] buf = inputBuffer;
        if (buf == null || buf.length < minSize) {
            buf = new byte[Math.max(minSize, MIN_OUTPUT_BUFFER)];
        } else {
            inputBuffer = null;
        }
        return buf;
    }

    public void releaseInputBuffer(byte[] buffer) {
        if (inputBuffer == null || (buffer != null && buffer.length > inputBuffer.length)) {
            inputBuffer = buffer;
        }
    }

    public byte[] allocDecodeBuffer(int size) {
        byte[] buf = decodingBuffer;
        if (buf == null || buf.length < size) {
            buf = new byte[size];
        } else {
            decodingBuffer = null;
        }
        return buf;
    }

    public void releaseDecodeBuffer(byte[] buffer) {
        if (decodingBuffer == null || (buffer != null && buffer.length > decodingBuffer.length)) {
            decodingBuffer = buffer;
        }
    }

}
