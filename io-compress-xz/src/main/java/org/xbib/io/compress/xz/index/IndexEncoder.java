package org.xbib.io.compress.xz.index;

import org.xbib.io.compress.xz.XZIOException;
import org.xbib.io.compress.xz.common.EncoderUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CheckedOutputStream;

public class IndexEncoder extends IndexBase {

    private final List<IndexRecord> records = new ArrayList<>();

    public IndexEncoder() {
        super(new XZIOException("XZ Stream or its Index has grown too big"));
    }

    public void add(long unpaddedSize, long uncompressedSize)
            throws XZIOException {
        super.add(unpaddedSize, uncompressedSize);
        records.add(new IndexRecord(unpaddedSize, uncompressedSize));
    }

    public void encode(OutputStream out) throws IOException {
        java.util.zip.CRC32 crc32 = new java.util.zip.CRC32();
        CheckedOutputStream outChecked = new CheckedOutputStream(out, crc32);

        // Index Indicator
        outChecked.write(0x00);

        // Number of Records
        EncoderUtil.encodeVLI(outChecked, recordCount);

        // List of Records
        for (IndexRecord record : records) {
            EncoderUtil.encodeVLI(outChecked, record.unpadded);
            EncoderUtil.encodeVLI(outChecked, record.uncompressed);
        }

        // Index Padding
        for (int i = getIndexPaddingSize(); i > 0; --i) {
            outChecked.write(0x00);
        }

        // CRC32
        long value = crc32.getValue();
        for (int i = 0; i < 4; ++i) {
            out.write((byte) (value >>> (i * 8)));
        }
    }
}
