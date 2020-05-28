package org.xbib.io.codec.ar;

import org.xbib.io.codec.ArchiveSession;
import org.xbib.io.archive.ar.ArArchiveInputStream;
import org.xbib.io.archive.ar.ArArchiveOutputStream;
import org.xbib.io.codec.Packet;
import org.xbib.io.codec.Session;
import org.xbib.io.codec.StringPacket;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Ar Session
 */
public class ArSession extends ArchiveSession<ArArchiveInputStream, ArArchiveOutputStream>
        implements Session<StringPacket> {

    private final static String SUFFIX = "ar";

    private ArArchiveInputStream in;

    private ArArchiveOutputStream out;

    protected String getSuffix() {
        return SUFFIX;
    }

    protected void open(InputStream in) {
        this.in = new ArArchiveInputStream(in);
    }

    protected void open(OutputStream out) {
        this.out = new ArArchiveOutputStream(out);
    }

    public ArArchiveInputStream getInputStream() {
        return in;
    }

    public ArArchiveOutputStream getOutputStream() {
        return out;
    }
}
