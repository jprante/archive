package org.xbib.io.compress.lzf;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Helper class used to store LZF encoded segments (compressed and
 * non-compressed) that can be sequenced to produce LZF files/streams.
 */
public class LZFChunk {

    /**
     * Maximum length of literal run for LZF encoding.
     */
    static final int MAX_LITERAL = 1 << 5; // 32
    // Chunk length is limited by 2-byte length indicator, to 64k
    static final int MAX_CHUNK_LEN = 0xFFFF;
    /**
     * Header can be either 7 bytes (compressed) or 5 bytes (uncompressed) long
     */
    static final int MAX_HEADER_LEN = 7;
    static final byte BYTE_Z = 'Z';
    static final byte BYTE_V = 'V';
    static final int BLOCK_TYPE_NON_COMPRESSED = 0;
    static final int BLOCK_TYPE_COMPRESSED = 1;
    private final byte[] data;
    private LZFChunk next;

    private LZFChunk(byte[] data) {
        this.data = data;
    }

    /**
     * Factory method for constructing compressed chunk
     */
    public static LZFChunk createCompressed(int origLen, byte[] encData, int encPtr, int encLen) {
        byte[] result = new byte[encLen + 7];
        result[0] = BYTE_Z;
        result[1] = BYTE_V;
        result[2] = BLOCK_TYPE_COMPRESSED;
        result[3] = (byte) (encLen >> 8);
        result[4] = (byte) encLen;
        result[5] = (byte) (origLen >> 8);
        result[6] = (byte) origLen;
        System.arraycopy(encData, encPtr, result, 7, encLen);
        return new LZFChunk(result);
    }

    public static int appendCompressedHeader(int origLen, int encLen, byte[] headerBuffer, int offset)
            throws IOException {
        headerBuffer[offset++] = BYTE_Z;
        headerBuffer[offset++] = BYTE_V;
        headerBuffer[offset++] = BLOCK_TYPE_COMPRESSED;
        headerBuffer[offset++] = (byte) (encLen >> 8);
        headerBuffer[offset++] = (byte) encLen;
        headerBuffer[offset++] = (byte) (origLen >> 8);
        headerBuffer[offset++] = (byte) origLen;
        return offset;
    }

    public static void writeCompressedHeader(int origLen, int encLen, OutputStream out, byte[] headerBuffer)
            throws IOException {
        headerBuffer[0] = BYTE_Z;
        headerBuffer[1] = BYTE_V;
        headerBuffer[2] = BLOCK_TYPE_COMPRESSED;
        headerBuffer[3] = (byte) (encLen >> 8);
        headerBuffer[4] = (byte) encLen;
        headerBuffer[5] = (byte) (origLen >> 8);
        headerBuffer[6] = (byte) origLen;
        out.write(headerBuffer, 0, 7);
    }

    /**
     * Factory method for constructing compressed chunk
     */
    public static LZFChunk createNonCompressed(byte[] plainData, int ptr, int len) {
        byte[] result = new byte[len + 5];
        result[0] = BYTE_Z;
        result[1] = BYTE_V;
        result[2] = BLOCK_TYPE_NON_COMPRESSED;
        result[3] = (byte) (len >> 8);
        result[4] = (byte) len;
        System.arraycopy(plainData, ptr, result, 5, len);
        return new LZFChunk(result);
    }

    public static int appendNonCompressedHeader(int len, byte[] headerBuffer, int offset)
            throws IOException {
        headerBuffer[offset++] = BYTE_Z;
        headerBuffer[offset++] = BYTE_V;
        headerBuffer[offset++] = BLOCK_TYPE_NON_COMPRESSED;
        headerBuffer[offset++] = (byte) (len >> 8);
        headerBuffer[offset++] = (byte) len;
        return offset;
    }

    public static void writeNonCompressedHeader(int len, OutputStream out, byte[] headerBuffer)
            throws IOException {
        headerBuffer[0] = BYTE_Z;
        headerBuffer[1] = BYTE_V;
        headerBuffer[2] = BLOCK_TYPE_NON_COMPRESSED;
        headerBuffer[3] = (byte) (len >> 8);
        headerBuffer[4] = (byte) len;
        out.write(headerBuffer, 0, 5);
    }

    public void setNext(LZFChunk next) {
        this.next = next;
    }

    public LZFChunk next() {
        return next;
    }

    public int length() {
        return data.length;
    }

    public byte[] getData() {
        return data;
    }

    public int copyTo(byte[] dst, int ptr) {
        int len = data.length;
        System.arraycopy(data, 0, dst, ptr, len);
        return ptr + len;
    }
}
