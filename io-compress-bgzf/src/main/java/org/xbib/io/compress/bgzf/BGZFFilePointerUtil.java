
package org.xbib.io.compress.bgzf;

public class BGZFFilePointerUtil {

    private static final int SHIFT_AMOUNT = 16;
    private static final int OFFSET_MASK = 0xffff;
    private static final long ADDRESS_MASK = 0xFFFFFFFFFFFFL;

    public static final long MAX_BLOCK_ADDRESS = ADDRESS_MASK;
    public static final int MAX_OFFSET = OFFSET_MASK;

    public static int compare(final long vfp1, final long vfp2) {
        if (vfp1 == vfp2) return 0;
        // When treating as unsigned, negative number is > positive.
        if (vfp1 < 0 && vfp2 >= 0) return 1;
        if (vfp1 >= 0 && vfp2 < 0) return -1;
        // Either both negative or both non-negative, so regular comparison works.
        if (vfp1 < vfp2) return -1;
        return 1; // vfp1 > vfp2
    }

    /**
     * @return true if vfp2 points to somewhere in the same BGZF block, or the one immediately
     *         following vfp1's BGZF block.
     */
    public static boolean areInSameOrAdjacentBlocks(final long vfp1, final long vfp2) {
        final long block1 = getBlockAddress(vfp1);
        final long block2 = getBlockAddress(vfp2);
        return (block1 == block2 || block1 + 1 == block2);
    }

    /**
     * @param blockAddress File offset of start of BGZF block.
     * @param blockOffset Offset into uncompressed block.
     * @return Virtual file pointer that embodies the input parameters.
     */
    static long makeFilePointer(final long blockAddress, final int blockOffset) {
        if (blockOffset < 0) {
            throw new IllegalArgumentException("Negative blockOffset " + blockOffset
                + " not allowed.");
        }
        if (blockAddress < 0) {
            throw new IllegalArgumentException("Negative blockAddress " + blockAddress
                + " not allowed.");
        }
        if (blockOffset > MAX_OFFSET) {
            throw new IllegalArgumentException("blockOffset " + blockOffset + " too large.");
        }
        if (blockAddress > MAX_BLOCK_ADDRESS) {
            throw new IllegalArgumentException("blockAddress " + blockAddress + " too large.");
        }
        return blockAddress << SHIFT_AMOUNT | blockOffset;
    }

    public static long getBlockAddress(final long virtualFilePointer) {
        return (virtualFilePointer >> SHIFT_AMOUNT) & ADDRESS_MASK;
    }

    public static int getBlockOffset(final long virtualFilePointer) {
        return (int)(virtualFilePointer & OFFSET_MASK);
    }

    public static String asString(final long vfp) {
        return String.format("%d(0x%x): (block address: %d, offset: %d)", vfp, vfp, getBlockAddress(vfp), getBlockOffset(vfp));
    }
}
