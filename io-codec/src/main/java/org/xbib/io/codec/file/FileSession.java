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

import org.xbib.io.codec.Session;
import org.xbib.io.codec.StreamCodecService;
import org.xbib.io.codec.StringPacket;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.OpenOption;
import java.nio.file.Path;

/**
 * A File session
 */
public class FileSession implements Session<StringPacket> {

    private final static StreamCodecService factory = StreamCodecService.getInstance();

    private final static String encoding = System.getProperty("file.encoding");

    private Path path;

    private OpenOption option;

    private boolean isOpen;

    private Writer writer;

    private Reader reader;

    public FileSession() {
    }

    public void setPath(Path path, OpenOption option) {
        this.path = path;
        this.option = option;
    }

    @Override
    public void open(Mode mode) throws IOException {
        if (isOpen()) {
            return;
        }
        this.isOpen = false;
        String filename = path.toString();
        File file = new File(filename);
        switch (mode) {
            case READ: {
                if (!file.exists()) {
                    throw new IOException("file does not exist: " + path);
                }
                if (filename.endsWith(".gz")) {
                    FileInputStream in = new FileInputStream(file);
                    this.reader = new InputStreamReader(factory.getCodec("gz").decode(in), encoding);
                    this.isOpen = true;
                } else if (filename.endsWith(".bz2")) {
                    FileInputStream in = new FileInputStream(file);
                    this.reader = new InputStreamReader(factory.getCodec("bz2").decode(in), encoding);
                    this.isOpen = true;
                } else if (filename.endsWith(".xz")) {
                    FileInputStream in = new FileInputStream(file);
                    this.reader = new InputStreamReader(factory.getCodec("xz").decode(in), encoding);
                    this.isOpen = true;
                } else {
                    FileInputStream in = new FileInputStream(file);
                    this.reader = new InputStreamReader(in, encoding);
                    this.isOpen = true;
                }
                break;
            }
            case WRITE: {
                if (file.exists()) {
                    throw new IOException("cowardly not overwriting file: " + file.getAbsolutePath());
                } else {
                    // create directories if required
                    file.getParentFile().mkdirs();
                }
                if (filename.endsWith(".gz")) {
                    FileOutputStream out = new FileOutputStream(file);
                    this.writer = new OutputStreamWriter(factory.getCodec("gz").encode(out), encoding);
                    this.isOpen = true;
                } else if (filename.endsWith(".bz2")) {
                    FileOutputStream out = new FileOutputStream(file);
                    this.writer = new OutputStreamWriter(factory.getCodec("bz2").encode(out), encoding);
                    this.isOpen = true;
                } else if (filename.endsWith(".xz")) {
                    FileInputStream in = new FileInputStream(file);
                    this.reader = new InputStreamReader(factory.getCodec("xz").decode(in), encoding);
                    this.isOpen = true;
                } else {
                    FileOutputStream out = new FileOutputStream(file);
                    this.writer = new OutputStreamWriter(out, encoding);
                    this.isOpen = true;
                }
                break;
            }
            case APPEND: {
                if (!file.exists()) {
                    file.getParentFile().mkdirs();
                }
                if (filename.endsWith(".gz")) {
                    FileOutputStream out = new FileOutputStream(file, true);
                    this.writer = new OutputStreamWriter(factory.getCodec("gz").encode(out), encoding);
                    this.isOpen = true;
                } else if (filename.endsWith(".bz2")) {
                    FileOutputStream out = new FileOutputStream(file, true);
                    this.writer = new OutputStreamWriter(factory.getCodec("bz2").encode(out), encoding);
                    this.isOpen = true;
                } else if (filename.endsWith(".xz")) {
                    FileOutputStream out = new FileOutputStream(file, true);
                    this.writer = new OutputStreamWriter(factory.getCodec("xz").encode(out), encoding);
                    this.isOpen = true;
                } else {
                    FileOutputStream out = new FileOutputStream(file, true);
                    this.writer = new OutputStreamWriter(out, encoding);
                    this.isOpen = true;
                }
                break;
            }
        }
    }

    @Override
    public boolean isOpen() {
        return isOpen;
    }

    @Override
    public void close() throws IOException {
        if (reader != null) {
            reader.close();
            this.isOpen = false;
        }
        if (writer != null) {
            writer.close();
            this.isOpen = false;
        }
    }

    public StringPacket newPacket() {
        return new StringPacket();
    }

    public StringPacket read() throws IOException {
        char[] ch = new char[1024];
        reader.read(ch);
        return new StringPacket().packet(new String(ch));
    }

    public void write(StringPacket packet) throws IOException {
        writer.write(packet.packet());
    }

}
