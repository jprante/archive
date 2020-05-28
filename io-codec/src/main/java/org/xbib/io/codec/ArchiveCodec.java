package org.xbib.io.codec;

import org.xbib.io.archive.stream.ArchiveInputStream;
import org.xbib.io.archive.stream.ArchiveOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * An archive codec defines the session and the input or output stream that are
 * used for reading or writing to an archive.
 *
 * @param <S> the archive session type
 * @param <I> the archive input stream type
 * @param <O> the archive output type
 */
public interface ArchiveCodec<S extends ArchiveSession, I extends ArchiveInputStream, O extends ArchiveOutputStream> {

    /**
     * Returns the name of this archive codec ("cpio", "tar", "zip")
     *
     * @return the name
     */
    String getName();

    /**
     * Creates a new archive session with a progress watcher.
     *
     * @param watcher the progress watcher
     * @return the new archive session
     */
    S newSession(BytesProgressWatcher watcher);

    /**
     * Creates a new archive input stream
     *
     * @param in the input stream for the archive input stream
     * @return the archive input stream
     * @throws IOException if archive input stream can not be created
     */
    I createArchiveInputStream(InputStream in) throws IOException;

    /**
     * Creates a new archive output stream
     *
     * @param out the output stream for the archive output stream
     * @return the archive output stream
     * @throws IOException if archive output stream can not be created
     */
    O createArchiveOutputStream(OutputStream out) throws IOException;

}