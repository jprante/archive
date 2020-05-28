package org.xbib.io.codec.zip;

import org.xbib.io.codec.ArchiveSession;
import org.xbib.io.archive.zip.ZipArchiveInputStream;
import org.xbib.io.archive.zip.ZipArchiveOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class ZipSession extends ArchiveSession<ZipArchiveInputStream, ZipArchiveOutputStream> {

    private final static String SUFFIX = "zip";

    private ZipArchiveInputStream in;

    private ZipArchiveOutputStream out;

    protected String getSuffix() {
        return SUFFIX;
    }

    protected void open(InputStream in) {
        this.in = new ZipArchiveInputStream(in);
    }

    protected void open(OutputStream out) {
        this.out = new ZipArchiveOutputStream(out);
    }

    public ZipArchiveInputStream getInputStream() {
        return in;
    }

    public ZipArchiveOutputStream getOutputStream() {
        return out;
    }

}
