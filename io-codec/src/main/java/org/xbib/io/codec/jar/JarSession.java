package org.xbib.io.codec.jar;

import org.xbib.io.archive.jar.JarArchiveEntry;
import org.xbib.io.codec.ArchiveSession;
import org.xbib.io.archive.jar.JarArchiveInputStream;
import org.xbib.io.archive.jar.JarArchiveOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class JarSession extends ArchiveSession<JarArchiveEntry, JarArchiveInputStream, JarArchiveOutputStream> {

    private final static String SUFFIX = "jar";

    private JarArchiveInputStream in;

    private JarArchiveOutputStream out;

    protected String getSuffix() {
        return SUFFIX;
    }

    protected void open(InputStream in) {
        this.in = new JarArchiveInputStream(in);
    }

    protected void open(OutputStream out) {
        this.out = new JarArchiveOutputStream(out);
    }

    public JarArchiveInputStream getInputStream() {
        return in;
    }

    public JarArchiveOutputStream getOutputStream() {
        return out;
    }

}
