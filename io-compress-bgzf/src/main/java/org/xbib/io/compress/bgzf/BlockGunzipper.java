package org.xbib.io.compress.bgzf;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * For decompressing GZIP blocks that are already loaded into a byte[].
 * The main advantage is that this object can be used over and over again to decompress many blocks,
 * whereas a new GZIPInputStream and ByteArrayInputStream would otherwise need to be created for each
 * block to be decompressed.
 *
 * This code requires that the GZIP header conform to the GZIP blocks written to BAM files, with
 * a specific subfield and no other optional stuff.
 */
public class BlockGunzipper {
    private static InflaterFactory defaultInflaterFactory = new InflaterFactory();
    private final Inflater inflater;
    private final CRC32 crc32 = new CRC32();
    private boolean checkCrcs = false;
    BlockGunzipper() {
        inflater = defaultInflaterFactory.makeInflater(true); // GZIP mode
    }

    /**
     * Create a BlockGunzipper using the provided inflaterFactory
     * @param inflaterFactory
     */
    BlockGunzipper(InflaterFactory inflaterFactory) {
        inflater = inflaterFactory.makeInflater(true); // GZIP mode
    }

    /**
     * Sets the default {@link InflaterFactory} that will be used for all instances unless specified otherwise in the constructor.
     * If this method is not called the default is a factory that will create the JDK {@link Inflater}.
     * @param inflaterFactory non-null default factory.
     */
    public static void setDefaultInflaterFactory(final InflaterFactory inflaterFactory) {
        if (inflaterFactory == null) {
            throw new IllegalArgumentException("null inflaterFactory");
        }
        defaultInflaterFactory = inflaterFactory;
    }

    public static InflaterFactory getDefaultInflaterFactory() {
        return defaultInflaterFactory;
    }
    /** Allows the caller to decide whether or not to check CRCs on when uncompressing blocks. */
    public void setCheckCrcs(final boolean check) {
        this.checkCrcs = check;
    }

    /**
     * Decompress GZIP-compressed data
     * @param uncompressedBlock must be big enough to hold decompressed output.
     * @param compressedBlock compressed data starting at offset 0
     * @param compressedLength size of compressed data, possibly less than the size of the buffer.
     */
    void unzipBlock(byte[] uncompressedBlock, byte[] compressedBlock, int compressedLength) {
        try {
            ByteBuffer byteBuffer = ByteBuffer.wrap(compressedBlock, 0, compressedLength);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

            // Validate GZIP header
            if (byteBuffer.get() != BGZFStreamConstants.GZIP_ID1 ||
                    byteBuffer.get() != (byte)BGZFStreamConstants.GZIP_ID2 ||
                    byteBuffer.get() != BGZFStreamConstants.GZIP_CM_DEFLATE ||
                    byteBuffer.get() != BGZFStreamConstants.GZIP_FLG
                    ) {
                throw new BGZFFormatException("Invalid GZIP header");
            }
            // Skip MTIME, XFL, OS fields
            byteBuffer.position(byteBuffer.position() + 6);
            if (byteBuffer.getShort() != BGZFStreamConstants.GZIP_XLEN) {
                throw new BGZFFormatException("Invalid GZIP header");
            }
            // Skip blocksize subfield intro
            byteBuffer.position(byteBuffer.position() + 4);
            // Read ushort
            final int totalBlockSize = (byteBuffer.getShort() & 0xffff) + 1;
            if (totalBlockSize != compressedLength) {
                throw new BGZFFormatException("GZIP blocksize disagreement");
            }

            // Read expected size and CRD from end of GZIP block
            final int deflatedSize = compressedLength - BGZFStreamConstants.BLOCK_HEADER_LENGTH - BGZFStreamConstants.BLOCK_FOOTER_LENGTH;
            byteBuffer.position(byteBuffer.position() + deflatedSize);
            int expectedCrc = byteBuffer.getInt();
            int uncompressedSize = byteBuffer.getInt();
            inflater.reset();

            // Decompress
            inflater.setInput(compressedBlock, BGZFStreamConstants.BLOCK_HEADER_LENGTH, deflatedSize);
            final int inflatedBytes = inflater.inflate(uncompressedBlock, 0, uncompressedSize);
            if (inflatedBytes != uncompressedSize) {
                throw new BGZFFormatException("Did not inflate expected amount");
            }

            // Validate CRC if so desired
            if (this.checkCrcs) {
                crc32.reset();
                crc32.update(uncompressedBlock, 0, uncompressedSize);
                final long crc = crc32.getValue();
                if ((int)crc != expectedCrc) {
                    throw new BGZFFormatException("CRC mismatch");
                }
            }
        } catch (DataFormatException e) {
            throw new BGZFException(e);
        }
    }
}
