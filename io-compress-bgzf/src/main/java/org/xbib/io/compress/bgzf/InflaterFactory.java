package org.xbib.io.compress.bgzf;

import java.util.zip.Inflater;

/**
 * Factory for {@link Inflater} objects used by {@link BlockGunzipper}.
 * This class may be extended to provide alternative inflaters (e.g., for improved performance).
 * The default implementation returns a JDK {@link Inflater}
 */
public class InflaterFactory {
    /**
     * Returns an inflater object that will be used when reading DEFLATE compressed files.
     * Subclasses may override to provide their own inflater implementation.
     * The default implementation returns a JDK {@link Inflater}
     * @param gzipCompatible if true then use GZIP compatible compression
     */
    public Inflater makeInflater(final boolean gzipCompatible) {
        return new Inflater(gzipCompatible);
    }
}
