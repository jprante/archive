package org.xbib.io.codec;

import java.io.IOException;

/**
 * The Session interface is used for being opened, receive
 * operations, and being closed. Sessions must be opened before the first
 * operation and closed after the last operation.
 */
public interface Session<P extends StringPacket> {

    enum Mode {

        READ, WRITE, APPEND, CONTROL, DELETE;
    }

    /**
     * Open valve with a given input/output mode
     *
     * @throws java.io.IOException if valve can not be opened
     */
    void open(Mode mode) throws IOException;

    P newPacket();

    P read() throws IOException;

    void write(P packet) throws IOException;

    /**
     * Close valve
     *
     * @throws java.io.IOException if valve can not be closed
     */
    void close() throws IOException;

    /**
     * Checks if this session has been successfully opened.
     *
     * @return true if the session is open
     */
    boolean isOpen();
}
