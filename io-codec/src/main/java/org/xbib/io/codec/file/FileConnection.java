/*
 * Licensed to Jörg Prante and xbib under one or more contributor 
 * license agreements. See the NOTICE.txt file distributed with this work
 * for additional information regarding copyright ownership.
 *
 * Copyright (C) 2012 Jörg Prante and xbib
 * 
 * This program is free software; you can redistribute it and/or modify 
 * it under the terms of the GNU Affero General Public License as published 
 * by the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the 
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License 
 * along with this program; if not, see http://www.gnu.org/licenses 
 * or write to the Free Software Foundation, Inc., 51 Franklin Street, 
 * Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * The interactive user interfaces in modified source and object code 
 * versions of this program must display Appropriate Legal Notices, 
 * as required under Section 5 of the GNU Affero General Public License.
 * 
 * In accordance with Section 7(b) of the GNU Affero General Public 
 * License, these Appropriate Legal Notices must retain the display of the 
 * "Powered by xbib" logo. If the display of the logo is not reasonably 
 * feasible for technical reasons, the Appropriate Legal Notices must display
 * the words "Powered by xbib".
 */
package org.xbib.io.codec.file;

import org.xbib.io.codec.Connection;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * File connection
 */
public class FileConnection extends URLConnection implements Connection<FileSession> {

    private FileSession session;

    private Path path;

    private OpenOption option;

    /**
     * Constructs a URL connection to the specified URL. A connection to
     * the object referenced by the URL is not created.
     *
     * @param url the specified URL.
     */
    public FileConnection(URL url) throws URISyntaxException {
        super(url);
        this.path = Paths.get(url.toURI().getSchemeSpecificPart());
        this.option = StandardOpenOption.READ;
    }

    @Override
    public void connect() throws IOException {
        this.session = createSession();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return Files.newInputStream(path, StandardOpenOption.READ);
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return Files.newOutputStream(path, StandardOpenOption.CREATE);
    }

    public void setPath(Path path, OpenOption option) {
        this.path = path;
        this.option = option;
    }

    public Path getPath() {
        return path;
    }

    @Override
    public FileSession createSession() throws IOException {
        FileSession session = new FileSession();
        session.setPath(path, option);
        return session;
    }

    @Override
    public void close() throws IOException {
        session.close();
    }
}

