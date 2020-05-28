package org.xbib.io.compress.xz;

import java.io.IOException;
import java.io.InputStream;

/**
 * Base class for filter-specific options classes.
 */
public abstract class FilterOptions implements Cloneable {
    public static int getEncoderMemoryUsage(FilterOptions[] options) {
        int m = 0;
        for (FilterOptions option : options) {
            m += option.getEncoderMemoryUsage();
        }
        return m;
    }

    public static int getDecoderMemoryUsage(FilterOptions[] options) {
        int m = 0;
        for (FilterOptions option : options) {
            m += option.getDecoderMemoryUsage();
        }
        return m;
    }

    /**
     * Gets how much memory the encoder will need with these options.
     */
    public abstract int getEncoderMemoryUsage();

    /**
     * Gets a raw (no XZ headers) encoder output stream using these options.
     * Raw streams are an advanced feature. In most cases you want to store
     * the compressed data in the .xz container format instead of using
     * a raw stream. To use this filter in a .xz file, pass this object
     * to XZOutputStream.
     */
    public abstract FinishableOutputStream getOutputStream(
            FinishableOutputStream out);

    /**
     * Gets how much memory the decoder will need to decompress the data
     * that was encoded with these options.
     */
    public abstract int getDecoderMemoryUsage();

    /**
     * Gets a raw (no XZ headers) decoder input stream using these options.
     */
    public abstract InputStream getInputStream(InputStream in)
            throws IOException;

    abstract FilterEncoder getFilterEncoder();

    FilterOptions() {
    }
}
