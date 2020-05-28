package org.xbib.io.compress.xz;

/**
 *
 */
abstract class BCJOptions extends FilterOptions {
    private final int alignment;
    int startOffset = 0;

    BCJOptions(int alignment) {
        this.alignment = alignment;
    }

    public void setStartOffset(int startOffset)
            throws UnsupportedOptionsException {
        if ((startOffset & (alignment - 1)) != 0) {
            throw new UnsupportedOptionsException(
                    "Start offset must be a multiple of " + alignment);
        }

        this.startOffset = startOffset;
    }

    public int getStartOffset() {
        return startOffset;
    }

    public int getEncoderMemoryUsage() {
        return SimpleOutputStream.getMemoryUsage();
    }

    public int getDecoderMemoryUsage() {
        return SimpleInputStream.getMemoryUsage();
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            assert false;
            throw new RuntimeException();
        }
    }
}
