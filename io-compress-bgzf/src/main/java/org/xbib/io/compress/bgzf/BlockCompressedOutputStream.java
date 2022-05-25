package org.xbib.io.compress.bgzf;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

/**
 * Stream class for a file that is a series of gzip blocks (BGZF format). The caller just treats it as an
 * OutputStream, and under the covers a gzip block is written when the amount of uncompressed as-yet-unwritten
 * bytes reaches a threshold.
 *
 * The advantage of BGZF over conventional gzip is that BGZF allows for seeking without having to scan through
 * the entire file up to the position being sought.
 *
 * Note that the flush() method should not be called by client
 * unless you know what you're doing, because it forces a gzip block to be written even if the
 * number of buffered bytes has not reached threshold.  close(), on the other hand, must be called
 * when done writing in order to force the last gzip block to be written.
 *
 * @see <a href="http://samtools.sourceforge.net/SAM1.pdf">http://samtools.sourceforge.net/SAM1.pdf</a> for details of BGZF file format.
 */
public class BlockCompressedOutputStream extends OutputStream {

    private static int defaultCompressionLevel = BGZFStreamConstants.DEFAULT_COMPRESSION_LEVEL;
    private static DeflaterFactory defaultDeflaterFactory = new DeflaterFactory();

    public static void setDefaultCompressionLevel(final int compressionLevel) {
        if (compressionLevel < Deflater.NO_COMPRESSION || compressionLevel > Deflater.BEST_COMPRESSION) {
            throw new IllegalArgumentException("Invalid compression level: " + compressionLevel);
        }
        defaultCompressionLevel = compressionLevel;
    }

    public static int getDefaultCompressionLevel() {
        return defaultCompressionLevel;
    }

    /**
     * Sets the default {@link DeflaterFactory} that will be used for all instances unless specified otherwise in the constructor.
     * If this method is not called the default is a factory that will create the JDK {@link Deflater}.
     * @param deflaterFactory non-null default factory.
     */
    public static void setDefaultDeflaterFactory(final DeflaterFactory deflaterFactory) {
        if (deflaterFactory == null) {
            throw new IllegalArgumentException("null deflaterFactory");
        }
        defaultDeflaterFactory = deflaterFactory;
    }

    public static DeflaterFactory getDefaultDeflaterFactory() {
        return defaultDeflaterFactory;
    }

    private final BinaryCodec codec;
    private final byte[] uncompressedBuffer = new byte[BGZFStreamConstants.DEFAULT_UNCOMPRESSED_BLOCK_SIZE];
    private int numUncompressedBytes = 0;
    private final byte[] compressedBuffer =
            new byte[BGZFStreamConstants.MAX_COMPRESSED_BLOCK_SIZE -
                    BGZFStreamConstants.BLOCK_HEADER_LENGTH];
    private final Deflater deflater;

    // A second deflater is created for the very unlikely case where the regular deflation actually makes
    // things bigger, and the compressed block is too big.  It should be possible to downshift the
    // primary deflater to NO_COMPRESSION level, recompress, and then restore it to its original setting,
    // but in practice that doesn't work.
    // The motivation for deflating at NO_COMPRESSION level is that it will predictably produce compressed
    // output that is 10 bytes larger than the input, and the threshold at which a block is generated is such that
    // the size of tbe final gzip block will always be <= 64K.  This is preferred over the previous method,
    // which would attempt to compress up to 64K bytes, and if the resulting compressed block was too large,
    // try compressing fewer input bytes (aka "downshifting').  The problem with downshifting is that
    // getFilePointer might return an inaccurate value.
    // I assume (AW 29-Oct-2013) that there is no value in using hardware-assisted deflater for no-compression mode,
    // so just use JDK standard.
    private final Deflater noCompressionDeflater = new Deflater(Deflater.NO_COMPRESSION, true);
    private final CRC32 crc32 = new CRC32();
    private Path file = null;
    private long mBlockAddress = 0;

    /**
     * Uses default compression level, which is 5 unless changed by setCompressionLevel
     * Note: this constructor uses the default {@link DeflaterFactory}, see {@link #getDefaultDeflaterFactory()}.
     * Use {@link #BlockCompressedOutputStream(File, int, DeflaterFactory)} to specify a custom factory.
     */
    public BlockCompressedOutputStream(final String filename) throws FileNotFoundException {
        this(filename, defaultCompressionLevel);
    }

    /**
     * Uses default compression level, which is 5 unless changed by setCompressionLevel
     * Note: this constructor uses the default {@link DeflaterFactory}, see {@link #getDefaultDeflaterFactory()}.
     * Use {@link #BlockCompressedOutputStream(File, int, DeflaterFactory)} to specify a custom factory.
     */
    public BlockCompressedOutputStream(final File file) throws FileNotFoundException {
        this(file, defaultCompressionLevel);
    }

    public BlockCompressedOutputStream(final String filename, final int compressionLevel) throws FileNotFoundException {
        this(new File(filename), compressionLevel);
    }

    public BlockCompressedOutputStream(final File file, final int compressionLevel) throws FileNotFoundException {
        this(file, compressionLevel, defaultDeflaterFactory);
    }

    public BlockCompressedOutputStream(final File file, final int compressionLevel, final DeflaterFactory deflaterFactory) throws FileNotFoundException {
        this.file = file.toPath();
        codec = new BinaryCodec(file, true);
        deflater = deflaterFactory.makeDeflater(compressionLevel, true);
    }

    /**
     * Uses default compression level, which is 5 unless changed by setCompressionLevel
     * Note: this constructor uses the default {@link DeflaterFactory}, see {@link #getDefaultDeflaterFactory()}.
     * Use {@link #BlockCompressedOutputStream(OutputStream, File, int, DeflaterFactory)} to specify a custom factory.
     */
    public BlockCompressedOutputStream(final OutputStream os) {
        this(os, (File)null, defaultCompressionLevel);
    }

    /**
     * Uses default compression level, which is 5 unless changed by setCompressionLevel
     * Note: this constructor uses the default {@link DeflaterFactory}, see {@link #getDefaultDeflaterFactory()}.
     * Use {@link #BlockCompressedOutputStream(OutputStream, File, int, DeflaterFactory)} to specify a custom factory.
     *
     * @param file may be null
     */
    public BlockCompressedOutputStream(final OutputStream os, final Path file) {
        this(os, file, defaultCompressionLevel);
    }

    /**
     * Note: this constructor uses the default {@link DeflaterFactory}, see {@link #getDefaultDeflaterFactory()}.
     * Use {@link #BlockCompressedOutputStream(OutputStream, File, int, DeflaterFactory)} to specify a custom factory.
     */
    public BlockCompressedOutputStream(final OutputStream os, final File file, final int compressionLevel) {
        this(os, file, compressionLevel, defaultDeflaterFactory);
    }

    /**
     * Note: this constructor uses the default {@link DeflaterFactory}, see {@link #getDefaultDeflaterFactory()}.
     * Use {@link #BlockCompressedOutputStream(OutputStream, File, int, DeflaterFactory)} to specify a custom factory.
     */
    public BlockCompressedOutputStream(final OutputStream os, final Path file, final int compressionLevel) {
        this(os, file, compressionLevel, defaultDeflaterFactory);
    }

    /**
     * Creates the output stream.
     * @param os output stream to create a BlockCompressedOutputStream from
     * @param file file to which to write the output or null if not available
     * @param compressionLevel the compression level (0-9)
     * @param deflaterFactory custom factory to create deflaters (overrides the default)
     */
    public BlockCompressedOutputStream(final OutputStream os, final File file, final int compressionLevel, final DeflaterFactory deflaterFactory) {
        this(os, file != null ? file.toPath() : null, compressionLevel, deflaterFactory);
    }

    /**
     * Creates the output stream.
     * @param os output stream to create a BlockCompressedOutputStream from
     * @param file file to which to write the output or null if not available
     * @param compressionLevel the compression level (0-9)
     * @param deflaterFactory custom factory to create deflaters (overrides the default)
     */
    public BlockCompressedOutputStream(final OutputStream os, final Path file, final int compressionLevel, final DeflaterFactory deflaterFactory) {
        this.file = file;
        codec = new BinaryCodec(os);
        if (file != null) {
            codec.setOutputFileName(file.toAbsolutePath().toUri().toString());
        }
        deflater = deflaterFactory.makeDeflater(compressionLevel, true);
    }

    /**
     * @param output May or not already be a BlockCompressedOutputStream.
     * @return A BlockCompressedOutputStream, either by wrapping the given OutputStream, or by casting if it already
     *         is a BCOS.
     */
    public static BlockCompressedOutputStream maybeBgzfWrapOutputStream(OutputStream output) {
        if (!(output instanceof BlockCompressedOutputStream)) {
           return new BlockCompressedOutputStream(output);
        } else {
           return (BlockCompressedOutputStream)output;
        }
    }

    /**
     * Writes b.length bytes from the specified byte array to this output stream. The general contract for write(b)
     * is that it should have exactly the same effect as the call write(b, 0, b.length).
     * @param bytes the data
     */
    @Override
    public void write(final byte[] bytes) throws IOException {
        write(bytes, 0, bytes.length);
    }

    /**
     * Writes len bytes from the specified byte array starting at offset off to this output stream. The general
     * contract for write(b, off, len) is that some of the bytes in the array b are written to the output stream in order;
     * element b[off] is the first byte written and b[off+len-1] is the last byte written by this operation.
     *
     * @param bytes the data
     * @param startIndex the start offset in the data
     * @param numBytes the number of bytes to write
     */
    @Override
    public void write(final byte[] bytes, int startIndex, int numBytes) throws IOException {
        while (numBytes > 0) {
            final int bytesToWrite = Math.min(uncompressedBuffer.length - numUncompressedBytes, numBytes);
            System.arraycopy(bytes, startIndex, uncompressedBuffer, numUncompressedBytes, bytesToWrite);
            numUncompressedBytes += bytesToWrite;
            startIndex += bytesToWrite;
            numBytes -= bytesToWrite;
            if (numUncompressedBytes == uncompressedBuffer.length) {
                deflateBlock();
            }
        }
    }

    @Override
    public void write(final int b) throws IOException {
        uncompressedBuffer[numUncompressedBytes++] = (byte) b;
        if (numUncompressedBytes == uncompressedBuffer.length) deflateBlock();
    }

    /**
     * WARNING: flush() affects the output format, because it causes the current contents of uncompressedBuffer
     * to be compressed and written, even if it isn't full.  Unless you know what you're doing, don't call flush().
     * Instead, call close(), which will flush any unwritten data before closing the underlying stream.
     *
     */
    @Override
    public void flush() throws IOException {
        while (numUncompressedBytes > 0) {
            deflateBlock();
        }
        codec.getOutputStream().flush();
    }

    /**
     * close() must be called in order to flush any remaining buffered bytes.  An unclosed file will likely be
     * defective.
     *
     */
    @Override
    public void close() throws IOException {
        close(true);
    }

    public void close(final boolean writeTerminatorBlock) throws IOException {
        flush();
        // For debugging...
        // if (numberOfThrottleBacks > 0) {
        //     System.err.println("In BlockCompressedOutputStream, had to throttle back " + numberOfThrottleBacks +
        //                        " times for file " + codec.getOutputFileName());
        // }
        if (writeTerminatorBlock) {
            codec.writeBytes(BGZFStreamConstants.EMPTY_GZIP_BLOCK);
        }
        codec.close();

        // If a terminator block was written, ensure that it's there and valid
        if (writeTerminatorBlock) {
            // Can't re-open something that is not a regular file, e.g. a named pipe or an output stream
            if (this.file == null || !Files.isRegularFile(this.file)) return;
            if (BlockCompressedInputStream.checkTermination(this.file) !=
                    BlockCompressedInputStream.FileTermination.HAS_TERMINATOR_BLOCK) {
                throw new IOException("Terminator block not found after closing BGZF file " + this.file);
            }
        }
    }

    /** Encode virtual file pointer
     * Upper 48 bits is the byte offset into the compressed stream of a block.
     * Lower 16 bits is the byte offset into the uncompressed stream inside the block.
     */
    public long getFilePointer(){
        return BGZFFilePointerUtil.makeFilePointer(mBlockAddress, numUncompressedBytes);
    }

    public long getPosition() {
        return getFilePointer();
    }

    /**
     * Attempt to write the data in uncompressedBuffer to the underlying file in a gzip block.
     * If the entire uncompressedBuffer does not fit in the maximum allowed size, reduce the amount
     * of data to be compressed, and slide the excess down in uncompressedBuffer so it can be picked
     * up in the next deflate event.
     * @return size of gzip block that was written.
     */
    private int deflateBlock() throws IOException {
        if (numUncompressedBytes == 0) {
            return 0;
        }
        final int bytesToCompress = numUncompressedBytes;
        // Compress the input
        deflater.reset();
        deflater.setInput(uncompressedBuffer, 0, bytesToCompress);
        deflater.finish();
        int compressedSize = deflater.deflate(compressedBuffer, 0, compressedBuffer.length);

        // If it didn't all fit in compressedBuffer.length, set compression level to NO_COMPRESSION
        // and try again.  This should always fit.
        if (!deflater.finished()) {
            noCompressionDeflater.reset();
            noCompressionDeflater.setInput(uncompressedBuffer, 0, bytesToCompress);
            noCompressionDeflater.finish();
            compressedSize = noCompressionDeflater.deflate(compressedBuffer, 0, compressedBuffer.length);
            if (!noCompressionDeflater.finished()) {
                throw new IllegalStateException("unpossible");
            }
        }
        // Data compressed small enough, so write it out.
        crc32.reset();
        crc32.update(uncompressedBuffer, 0, bytesToCompress);

        final int totalBlockSize = writeGzipBlock(compressedSize, bytesToCompress, crc32.getValue());

        // Clear out from uncompressedBuffer the data that was written
        numUncompressedBytes = 0;
        mBlockAddress += totalBlockSize;
        return totalBlockSize;
    }

    /**
     * Writes the entire gzip block, assuming the compressed data is stored in compressedBuffer
     * @return  size of gzip block that was written.
     */
    private int writeGzipBlock(final int compressedSize, final int uncompressedSize, final long crc) throws IOException {
        // Init gzip header
        codec.writeByte(BGZFStreamConstants.GZIP_ID1);
        codec.writeByte(BGZFStreamConstants.GZIP_ID2);
        codec.writeByte(BGZFStreamConstants.GZIP_CM_DEFLATE);
        codec.writeByte(BGZFStreamConstants.GZIP_FLG);
        codec.writeInt(0); // Modification time
        codec.writeByte(BGZFStreamConstants.GZIP_XFL);
        codec.writeByte(BGZFStreamConstants.GZIP_OS_UNKNOWN);
        codec.writeShort(BGZFStreamConstants.GZIP_XLEN);
        codec.writeByte(BGZFStreamConstants.BGZF_ID1);
        codec.writeByte(BGZFStreamConstants.BGZF_ID2);
        codec.writeShort(BGZFStreamConstants.BGZF_LEN);
        final int totalBlockSize = compressedSize + BGZFStreamConstants.BLOCK_HEADER_LENGTH +
                BGZFStreamConstants.BLOCK_FOOTER_LENGTH;

        // I don't know why we store block size - 1, but that is what the spec says
        codec.writeShort((short)(totalBlockSize - 1));
        codec.writeBytes(compressedBuffer, 0, compressedSize);
        codec.writeInt((int)crc);
        codec.writeInt(uncompressedSize);
        return totalBlockSize;
    }
}
