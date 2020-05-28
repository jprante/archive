package org.xbib.io.codec;

import java.io.Closeable;
import java.io.IOException;

/**
 * A Connection is an access to a resource via a scheme or a protocol.
 * Each connection can serve multiple sessions in parallel.
 */
public interface Connection<S extends Session<StringPacket>> extends Closeable {

    /**
     * Create a new session on this connection
     *
     * @return the session
     * @throws java.io.IOException if the session can not be created
     */
    S createSession() throws IOException;
}
