package org.xbib.io.codec.bgzf;

import org.xbib.io.codec.StreamCodec;
import org.xbib.io.compress.bgzf.BlockCompressedInputStream;
import org.xbib.io.compress.bgzf.BlockCompressedOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BzgfStreamCodec implements StreamCodec<BlockCompressedInputStream, BlockCompressedOutputStream> {

    @Override
    public String getName() {
        return "bgzf";
    }

    @Override
    public BlockCompressedInputStream decode(InputStream in) throws IOException {
        return new BlockCompressedInputStream(in);
    }

    @Override
    public BlockCompressedInputStream decode(InputStream in, int bufsize) throws IOException {
        return new BlockCompressedInputStream(in);
    }

    @Override
    public BlockCompressedOutputStream encode(OutputStream out) throws IOException {
        return new BlockCompressedOutputStream(out);
    }

    @Override
    public BlockCompressedOutputStream encode(OutputStream out, int bufsize) throws IOException {
        return new BlockCompressedOutputStream(out);
    }
}
