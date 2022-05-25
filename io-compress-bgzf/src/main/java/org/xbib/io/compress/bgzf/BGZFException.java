package org.xbib.io.compress.bgzf;

@SuppressWarnings("serial")
public class BGZFException extends RuntimeException {

    public BGZFException() {}

    public BGZFException(final String s) {
        super(s);
    }

    public BGZFException(final String s, final Throwable throwable) {
        super(s, throwable);
    }

    public BGZFException(final Throwable throwable) {
        super(throwable);
    }
}
