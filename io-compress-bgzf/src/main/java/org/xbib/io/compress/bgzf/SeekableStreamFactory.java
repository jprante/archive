package org.xbib.io.compress.bgzf;

import java.io.File;
import java.io.IOException;

public class SeekableStreamFactory {

    public static SeekableStream getStreamFor(String path) throws IOException {
        return new SeekableFileStream(new File(path));
    }
}
