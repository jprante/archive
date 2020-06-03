package org.xbib.io.codec;

import org.xbib.io.archive.entry.ArchiveEntry;
import org.xbib.io.archive.stream.ArchiveInputStream;
import org.xbib.io.archive.stream.ArchiveOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Date;
import java.util.Set;

/**
 * Archive session.
 */
public abstract class ArchiveSession<E extends ArchiveEntry, I extends ArchiveInputStream<E>, O extends ArchiveOutputStream<E>>
        implements Session<StringPacket> {

    private final static int DEFAULT_INPUT_BUFSIZE = 65536;

    protected int bufferSize = DEFAULT_INPUT_BUFSIZE;

    private boolean isOpen;

    private Path path;

    private OpenOption option;

    protected ArchiveSession() {
    }

    public void setPath(Path path, OpenOption option) {
        this.path = path;
        this.option = option;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    @Override
    public synchronized void open(Session.Mode mode) throws IOException {
        if (isOpen) {
            return;
        }
        switch (mode) {
            case READ: {
                InputStream in = newInputStream(path, option);
                open(in);
                this.isOpen = getInputStream() != null;
                break;
            }
            case WRITE: {
                OutputStream out = newOutputStream(path, option);
                open(out);
                this.isOpen = getOutputStream() != null;
                break;
            }
        }
    }

    @Override
    public StringPacket newPacket() {
        return new StringPacket();
    }

    @Override
    public synchronized StringPacket read() throws IOException {
        if (!isOpen()) {
            throw new IOException("not open");
        }
        if (getInputStream() == null) {
            throw new IOException("no input stream found");
        }
        ArchiveEntry entry = getInputStream().getNextEntry();
        if (entry == null) {
            return null;
        }
        StringPacket packet = newPacket();
        String name = entry.getName();
        packet.name(name);
        int size = (int)entry.getEntrySize();
        byte[] b = new byte[size];
        getInputStream().read(b, 0, size);
        packet.packet(new String(b));
        return packet;
    }

    @Override
    public synchronized void write(StringPacket packet) throws IOException {
        if (!isOpen()) {
            throw new IOException("not open");
        }
        if (getOutputStream() == null) {
            throw new IOException("no output stream found");
        }
        if (packet == null || packet.toString() == null) {
            throw new IOException("no packet to write");
        }
        byte[] buf = packet.toString().getBytes();
        if (buf.length > 0) {
            String name = packet.name();
            E entry = getOutputStream().newArchiveEntry();
            entry.setName(name);
            entry.setLastModified(new Date());
            entry.setEntrySize(buf.length);
            getOutputStream().putArchiveEntry(entry);
            getOutputStream().write(buf);
            getOutputStream().closeArchiveEntry();
        }
    }

    @Override
    public synchronized void close() throws IOException {
        if (!isOpen) {
            return;
        }
        if (getOutputStream() != null) {
            getOutputStream().close();
        }
        if (getInputStream() != null) {
            getInputStream().close();
        }
        this.isOpen = false;
    }

    @Override
    public boolean isOpen() {
        return isOpen;
    }

    public boolean canOpen(URI uri) {
        return canOpen(uri, getSuffix(), true);
    }

    public static boolean canOpen(URI uri, String suffix, boolean withCodecs) {
        final String scheme = uri.getScheme();
        final String part = uri.getSchemeSpecificPart();
        if (scheme.equals(suffix) ||
                (scheme.equals("file") && part.endsWith("." + suffix.toLowerCase())) ||
                (scheme.equals("file") && part.endsWith("." + suffix.toUpperCase()))) {
            return true;
        }
        if (withCodecs) {
            Set<String> codecs = StreamCodecService.getCodecs();
            for (String codec : codecs) {
                String s = "." + suffix + "." + codec;
                if (part.endsWith(s) || part.endsWith(s.toLowerCase()) || part.endsWith(s.toUpperCase())) {
                    return true;
                }
            }
        }
        return false;
    }

    protected abstract String getSuffix();

    protected abstract void open(InputStream in) throws IOException;

    protected abstract void open(OutputStream in) throws IOException;

    protected abstract I getInputStream();

    protected abstract O getOutputStream();

    /**
     * Helper method for creating the FileInputStream
     *
     * @param path the path
     * @return an InputStream
     * @throws java.io.IOException if existence or access rights do not suffice
     */
    public static InputStream newInputStream(Path path, OpenOption option) throws IOException {
        if (path == null) {
            throw new IOException("no path given");
        }
        String part = path.toUri().getSchemeSpecificPart();
        if (Files.isReadable(path) && Files.isRegularFile(path)) {
            InputStream in = Files.newInputStream(path, option);
            Set<String> codecs = StreamCodecService.getCodecs();
            for (String codec : codecs) {
                String s = "." + codec;
                if (part.endsWith(s.toLowerCase()) || part.endsWith(s.toUpperCase())) {
                    in = StreamCodecService.getInstance().getCodec(codec).decode(in);
                }
            }
            return in;
        } else {
            throw new IOException("can't open for input, check existence or access rights: " + path);
        }
    }

    /**
     * Helper method for creating the FileOutputStream. Creates the directory if
     * it does not exist.
     *
     * @throws java.io.IOException if existence or access rights do not suffice
     */
    public static OutputStream newOutputStream(Path path, OpenOption option) throws IOException {
        String part = path.toUri().getSchemeSpecificPart();
        OutputStream out = Files.newOutputStream(path, option);
        Set<String> codecs = StreamCodecService.getCodecs();
        for (String codec : codecs) {
            String s = "." + codec;
            if (part.endsWith(s.toLowerCase()) || part.endsWith(s.toUpperCase())) {
                out = StreamCodecService.getInstance().getCodec(codec).encode(out);
            }
        }
        return out;
    }

}
