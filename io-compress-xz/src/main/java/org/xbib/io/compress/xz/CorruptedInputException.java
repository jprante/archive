package org.xbib.io.compress.xz;

/**
 * Thrown when the compressed input data is corrupt.
 * However, it is possible that some or all of the data
 * already read from the input stream was corrupt too.
 */
public class CorruptedInputException extends XZIOException {
    private static final long serialVersionUID = 3L;

    /**
     * Creates a new CorruptedInputException with
     * the default error detail message.
     */
    public CorruptedInputException() {
        super("Compressed data is corrupt");
    }

    /**
     * Creates a new CorruptedInputException with
     * the specified error detail message.
     *
     * @param s error detail message
     */
    public CorruptedInputException(String s) {
        super(s);
    }
}
