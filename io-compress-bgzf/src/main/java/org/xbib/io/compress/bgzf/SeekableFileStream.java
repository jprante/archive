package org.xbib.io.compress.bgzf;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

public class SeekableFileStream extends SeekableStream {

    /**
     * Collection of all open instances.  SeekableFileStream objects are usually open and kept open for the
     * duration of a session.  This collection supports a method to close them all.
     */
    private static final Collection<SeekableFileStream> allInstances = Collections.synchronizedCollection(new HashSet<>());

    File file;
    RandomAccessFile fis;

    public SeekableFileStream(final File file) throws FileNotFoundException {
        this.file = file;
        fis = new RandomAccessFile(file, "r");
        allInstances.add(this);
    }

    @Override
    public long length() {
        return file.length();
    }

    @Override
    public boolean eof() throws IOException {
        return fis.length() == fis.getFilePointer();
    }

    @Override
    public void seek(final long position) throws IOException {
        fis.seek(position);
    }

    @Override
    public long position() throws IOException {
        return fis.getChannel().position();
    }

    @Override
    public long skip(long n) throws IOException {
        long initPos = position();
        fis.getChannel().position(initPos + n);
        return position() - initPos;
    }

    @Override
    public int read(final byte[] buffer, final int offset, final int length) throws IOException {
        if (length < 0) {
            throw new IndexOutOfBoundsException();
        }
        int n = 0;
        while (n < length) {
            final int count = fis.read(buffer, offset + n, length - n);
            if (count < 0) {
              if (n > 0) {
                return n;
              } else {
                return count;
              }
            }
            n += count;
        }
        return n;

    }

    @Override
    public int read() throws IOException {
        return fis.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return fis.read(b);
    }

    @Override
    public String getSource() {
        return file.getAbsolutePath();
    }


    @Override
    public void close() throws IOException {
        allInstances.remove(this);
        fis.close();

    }

    public static synchronized void closeAllInstances() {
        Collection<SeekableFileStream> clonedInstances = new HashSet<>(allInstances);
        for (SeekableFileStream sfs : clonedInstances) {
            try {
                sfs.close();
            } catch (IOException e) {
                //
            }
        }
        allInstances.clear();
    }
}
