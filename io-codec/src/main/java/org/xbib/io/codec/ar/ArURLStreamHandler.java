package org.xbib.io.codec.ar;

import org.xbib.io.codec.CustomURLStreamHandler;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

/**
 *
 */
public class ArURLStreamHandler extends CustomURLStreamHandler {

    @Override
    public String getName() {
        return "ar";
    }

    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        try {
            return new ArConnection(u);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }
}
