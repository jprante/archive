package org.xbib.io.compress.bgzf;

import java.util.zip.Deflater;

/**
 * Factory for {@link Deflater} objects used by {@link BlockCompressedOutputStream}.
 * This class may be extended to provide alternative deflaters (e.g., for improved performance).
 */
public class DeflaterFactory {

    public DeflaterFactory() {
        //Note: made explicit constructor to make searching for references easier
    }

    /**
     * Returns a deflater object that will be used when writing BAM files.
     * Subclasses may override to provide their own deflater implementation.
     * @param compressionLevel the compression level (0-9)
     * @param gzipCompatible if true then use GZIP compatible compression
     */
    public Deflater makeDeflater(final int compressionLevel, final boolean gzipCompatible) {
        return new Deflater(compressionLevel, gzipCompatible);
    }
}
