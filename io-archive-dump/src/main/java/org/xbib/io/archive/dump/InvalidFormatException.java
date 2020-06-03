package org.xbib.io.archive.dump;

/**
 * Invalid Format Exception. There was an error decoding a
 * tape segment header.
 */
@SuppressWarnings("serial")
public class InvalidFormatException extends DumpArchiveException {

    protected long offset;

    public InvalidFormatException() {
        super("there was an error decoding a tape segment");
    }

    public long getOffset() {
        return offset;
    }
}
