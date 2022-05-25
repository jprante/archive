package org.xbib.io.compress.bgzf;

@SuppressWarnings("serial")
public class BGZFFormatException extends BGZFException {

    public BGZFFormatException() {}

    public BGZFFormatException(final String s) {
        super(s);
    }

    public BGZFFormatException(final String s, final Throwable throwable) {
        super(s, throwable);
    }

    public BGZFFormatException(final Throwable throwable) {
        super(throwable);
    }
}
