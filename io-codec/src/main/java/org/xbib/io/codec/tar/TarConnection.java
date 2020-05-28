package org.xbib.io.codec.tar;

import org.xbib.io.codec.Connection;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Tar connection
 */
public class TarConnection extends URLConnection implements Connection<TarSession> {

    private TarSession session;

    private Path path;

    private OpenOption option;

    public TarConnection() {
        super(null);
    }

    /**
     * Constructs a URL connection to the specified URL. A connection to
     * the object referenced by the URL is not created.
     *
     * @param url the specified URL.
     */
    protected TarConnection(URL url) throws URISyntaxException {
        super(url);
        this.path = Paths.get(url.toURI().getSchemeSpecificPart());
        this.option = StandardOpenOption.READ;
    }

    @Override
    public void connect() throws IOException {
        this.session = createSession();
    }

    public void setPath(Path path, OpenOption option) {
        this.path = path;
        this.option = option;
    }

    public Path getPath() {
        return path;
    }

    @Override
    public TarSession createSession() throws IOException {
        TarSession session = new TarSession();
        session.setPath(path, option);
        return session;
    }

    @Override
    public void close() throws IOException {
        if (session != null) {
            session.close();
        }
    }
}
