package org.xbib.io.codec.cpio;

import org.xbib.io.codec.CustomURLStreamHandler;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

/**
 *
 */
public class CpioURLStreamHandler extends CustomURLStreamHandler {

    @Override
    public String getName() {
        return "cpio";
    }

    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        try {
            return new CpioConnection(u);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }
}
