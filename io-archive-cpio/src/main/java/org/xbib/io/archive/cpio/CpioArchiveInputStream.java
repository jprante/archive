package org.xbib.io.archive.cpio;

import org.xbib.io.archive.entry.ArchiveEntry;
import org.xbib.io.archive.stream.ArchiveInputStream;
import org.xbib.io.archive.util.ArchiveUtils;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * CPIOArchiveInputStream is a stream for reading cpio streams. All formats of
 * cpio are supported (old ascii, old binary, new portable format and the new
 * portable format with crc).
 * The stream can be read by extracting a cpio entry (containing all
 * informations about a entry) and afterwards reading from the stream the file
 * specified by the entry.
 * <pre><code>
 * CPIOArchiveInputStream cpioIn = new CPIOArchiveInputStream(
 *         new FileInputStream(new File(&quot;test.cpio&quot;)));
 * CPIOArchiveEntry cpioEntry;
 * while ((cpioEntry = cpioIn.getNextEntry()) != null) {
 *     System.out.println(cpioEntry.getName());
 *     int tmp;
 *     StringBuilder buf = new StringBuilder();
 *     while ((tmp = cpIn.read()) != -1) {
 *         buf.append((char) tmp);
 *     }
 *     System.out.println(buf.toString());
 * }
 * cpioIn.close();
 * </code></pre>
 * Note: This implementation should be compatible to cpio 2.5
 */

public class CpioArchiveInputStream extends ArchiveInputStream implements CpioConstants {

    private boolean closed = false;

    private CpioArchiveEntry entry;

    private long entryBytesRead = 0;

    private boolean entryEOF = false;

    private final byte tmpbuf[] = new byte[4096];

    private long crc = 0;

    private final InputStream in;

    /**
     * Construct the cpio input stream
     *
     * @param in The cpio stream
     */
    public CpioArchiveInputStream(final InputStream in) {
        this.in = in;
    }

    /**
     * Returns 0 after EOF has reached for the current entry data, otherwise
     * always return 1.
     * Programs should not count on this method to return the actual number of
     * bytes that could be read without blocking.
     *
     * @return 1 before EOF and 0 after EOF has reached for current entry.
     * @throws java.io.IOException if an I/O error has occurred or if a CPIO file error has
     *                             occurred
     */
    @Override
    public int available() throws IOException {
        ensureOpen();
        if (this.entryEOF) {
            return 0;
        }
        return 1;
    }

    /**
     * Closes the CPIO input stream.
     *
     * @throws java.io.IOException if an I/O error has occurred
     */
    @Override
    public void close() throws IOException {
        if (!this.closed) {
            in.close();
            this.closed = true;
        }
    }

    /**
     * Closes the current CPIO entry and positions the stream for reading the
     * next entry.
     *
     * @throws java.io.IOException if an I/O error has occurred or if a CPIO file error has
     *                             occurred
     */
    private void closeEntry() throws IOException {
        ensureOpen();
        while (read(this.tmpbuf, 0, this.tmpbuf.length) != -1) {
            // do nothing
        }

        this.entryEOF = true;
    }

    /**
     * Check to make sure that this stream has not been closed
     *
     * @throws java.io.IOException if the stream is already closed
     */
    private void ensureOpen() throws IOException {
        if (this.closed) {
            throw new IOException("stream closed");
        }
    }

    /**
     * Reads the next CPIO file entry and positions stream at the beginning of
     * the entry data.
     *
     * @return the CPIOArchiveEntry just read
     * @throws java.io.IOException if an I/O error has occurred or if a CPIO file error has
     *                             occurred
     */
    public CpioArchiveEntry getNextCPIOEntry() throws IOException {
        ensureOpen();
        if (this.entry != null) {
            closeEntry();
        }
        byte magic[] = new byte[2];
        readFully(magic, 0, magic.length);
        if (CpioUtil.byteArray2long(magic, false) == MAGIC_OLD_BINARY) {
            this.entry = readOldBinaryEntry(false);
        } else if (CpioUtil.byteArray2long(magic, true) == MAGIC_OLD_BINARY) {
            this.entry = readOldBinaryEntry(true);
        } else {
            byte more_magic[] = new byte[4];
            readFully(more_magic, 0, more_magic.length);
            byte tmp[] = new byte[6];
            System.arraycopy(magic, 0, tmp, 0, magic.length);
            System.arraycopy(more_magic, 0, tmp, magic.length,
                    more_magic.length);
            String magicString = ArchiveUtils.toAsciiString(tmp);
            if (magicString.equals(MAGIC_NEW)) {
                this.entry = readNewEntry(false);
            } else if (magicString.equals(MAGIC_NEW_CRC)) {
                this.entry = readNewEntry(true);
            } else if (magicString.equals(MAGIC_OLD_ASCII)) {
                this.entry = readOldAsciiEntry();
            } else {
                throw new IOException("Unknown magic [" + magicString + "]");
            }
        }

        this.entryBytesRead = 0;
        this.entryEOF = false;
        this.crc = 0;

        if (this.entry.getName().equals(CPIO_TRAILER)) {
            this.entryEOF = true;
            return null;
        }
        return this.entry;
    }

    private void skip(int bytes) throws IOException {
        final byte[] buff = new byte[4]; // Cannot be more than 3 bytes
        if (bytes > 0) {
            readFully(buff, 0, bytes);
        }
    }

    /**
     * Reads from the current CPIO entry into an array of bytes. Blocks until
     * some input is available.
     *
     * @param b   the buffer into which the data is read
     * @param off the start offset of the data
     * @param len the maximum number of bytes read
     * @return the actual number of bytes read, or -1 if the end of the entry is
     * reached
     * @throws java.io.IOException if an I/O error has occurred or if a CPIO file error has
     *                             occurred
     */
    @Override
    public int read(final byte[] b, final int off, final int len)
            throws IOException {
        ensureOpen();
        if (off < 0 || len < 0 || off > b.length - len) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }

        if (this.entry == null || this.entryEOF) {
            return -1;
        }
        if (this.entryBytesRead == this.entry.getEntrySize()) {
            skip(entry.getDataPadCount());
            this.entryEOF = true;
            if (this.entry.getFormat() == FORMAT_NEW_CRC
                    && this.crc != this.entry.getChksum()) {
                throw new IOException("CRC Error");
            }
            return -1; // EOF for this entry
        }
        int tmplength = (int) Math.min(len, this.entry.getEntrySize()
                - this.entryBytesRead);
        if (tmplength < 0) {
            return -1;
        }

        int tmpread = readFully(b, off, tmplength);
        if (this.entry.getFormat() == FORMAT_NEW_CRC) {
            for (int pos = 0; pos < tmpread; pos++) {
                this.crc += b[pos] & 0xFF;
            }
        }
        this.entryBytesRead += tmpread;

        return tmpread;
    }

    private int readFully(final byte[] b, final int off, final int len)
            throws IOException {
        if (len < 0) {
            throw new IndexOutOfBoundsException();
        }
        int n = 0;
        while (n < len) {
            int count = this.in.read(b, off + n, len - n);
            if (count < 0) {
                throw new EOFException();
            }
            n += count;
        }
        return n;
    }

    private long readBinaryLong(final int length, final boolean swapHalfWord)
            throws IOException {
        byte tmp[] = new byte[length];
        readFully(tmp, 0, tmp.length);
        return CpioUtil.byteArray2long(tmp, swapHalfWord);
    }

    private long readAsciiLong(final int length, final int radix)
            throws IOException {
        byte tmpBuffer[] = new byte[length];
        readFully(tmpBuffer, 0, tmpBuffer.length);
        return Long.parseLong(ArchiveUtils.toAsciiString(tmpBuffer), radix);
    }

    private CpioArchiveEntry readNewEntry(final boolean hasCrc)
            throws IOException {
        CpioArchiveEntry ret;
        if (hasCrc) {
            ret = new CpioArchiveEntry(FORMAT_NEW_CRC);
        } else {
            ret = new CpioArchiveEntry(FORMAT_NEW);
        }

        ret.setInode(readAsciiLong(8, 16));
        long mode = readAsciiLong(8, 16);
        if (mode != 0) { // mode is initialised to 0
            ret.setMode(mode);
        }
        ret.setUID(readAsciiLong(8, 16));
        ret.setGID(readAsciiLong(8, 16));
        ret.setNumberOfLinks(readAsciiLong(8, 16));
        ret.setTime(readAsciiLong(8, 16));
        ret.setEntrySize(readAsciiLong(8, 16));
        ret.setDeviceMaj(readAsciiLong(8, 16));
        ret.setDeviceMin(readAsciiLong(8, 16));
        ret.setRemoteDeviceMaj(readAsciiLong(8, 16));
        ret.setRemoteDeviceMin(readAsciiLong(8, 16));
        long namesize = readAsciiLong(8, 16);
        ret.setChksum(readAsciiLong(8, 16));
        String name = readCString((int) namesize);
        ret.setName(name);
        if (mode == 0 && !name.equals(CPIO_TRAILER)) {
            throw new IOException("Mode 0 only allowed in the trailer. Found entry name: " + name);
        }
        skip(ret.getHeaderPadCount());

        return ret;
    }

    private CpioArchiveEntry readOldAsciiEntry() throws IOException {
        CpioArchiveEntry ret = new CpioArchiveEntry(FORMAT_OLD_ASCII);

        ret.setDevice(readAsciiLong(6, 8));
        ret.setInode(readAsciiLong(6, 8));
        final long mode = readAsciiLong(6, 8);
        if (mode != 0) {
            ret.setMode(mode);
        }
        ret.setUID(readAsciiLong(6, 8));
        ret.setGID(readAsciiLong(6, 8));
        ret.setNumberOfLinks(readAsciiLong(6, 8));
        ret.setRemoteDevice(readAsciiLong(6, 8));
        ret.setTime(readAsciiLong(11, 8));
        long namesize = readAsciiLong(6, 8);
        ret.setEntrySize(readAsciiLong(11, 8));
        final String name = readCString((int) namesize);
        ret.setName(name);
        if (mode == 0 && !name.equals(CPIO_TRAILER)) {
            throw new IOException("Mode 0 only allowed in the trailer. Found entry: " + name);
        }

        return ret;
    }

    private CpioArchiveEntry readOldBinaryEntry(final boolean swapHalfWord)
            throws IOException {
        CpioArchiveEntry ret = new CpioArchiveEntry(FORMAT_OLD_BINARY);

        ret.setDevice(readBinaryLong(2, swapHalfWord));
        ret.setInode(readBinaryLong(2, swapHalfWord));
        final long mode = readBinaryLong(2, swapHalfWord);
        if (mode != 0) {
            ret.setMode(mode);
        }
        ret.setUID(readBinaryLong(2, swapHalfWord));
        ret.setGID(readBinaryLong(2, swapHalfWord));
        ret.setNumberOfLinks(readBinaryLong(2, swapHalfWord));
        ret.setRemoteDevice(readBinaryLong(2, swapHalfWord));
        ret.setTime(readBinaryLong(4, swapHalfWord));
        long namesize = readBinaryLong(2, swapHalfWord);
        ret.setEntrySize(readBinaryLong(4, swapHalfWord));
        final String name = readCString((int) namesize);
        ret.setName(name);
        if (mode == 0 && !name.equals(CPIO_TRAILER)) {
            throw new IOException("Mode 0 only allowed in the trailer. Found entry: " + name);
        }
        skip(ret.getHeaderPadCount());

        return ret;
    }

    private String readCString(final int length) throws IOException {
        byte[] tmpBuffer = new byte[length];
        readFully(tmpBuffer, 0, tmpBuffer.length);
        return new String(tmpBuffer, 0, tmpBuffer.length - 1);
    }

    /**
     * Skips specified number of bytes in the current CPIO entry.
     *
     * @param n the number of bytes to skip
     * @return the actual number of bytes skipped
     * @throws java.io.IOException      if an I/O error has occurred
     * @throws IllegalArgumentException if n &lt; 0
     */
    @Override
    public long skip(final long n) throws IOException {
        if (n < 0) {
            throw new IllegalArgumentException("negative skip length");
        }
        ensureOpen();
        int max = (int) Math.min(n, Integer.MAX_VALUE);
        int total = 0;

        while (total < max) {
            int len = max - total;
            if (len > this.tmpbuf.length) {
                len = this.tmpbuf.length;
            }
            len = read(this.tmpbuf, 0, len);
            if (len == -1) {
                this.entryEOF = true;
                break;
            }
            total += len;
        }
        return total;
    }

    @Override
    public ArchiveEntry getNextEntry() throws IOException {
        return getNextCPIOEntry();
    }

}
