package org.xbib.io.compress.bzip2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class BZip2Test {

    @Test
    public void testBZip2HelloWorld() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Bzip2OutputStream zOut = new Bzip2OutputStream(out);
        ObjectOutputStream objOut = new ObjectOutputStream(zOut);
        String helloWorld = "Hello World!";
        objOut.writeObject(helloWorld);
        zOut.close();
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        Bzip2InputStream zIn = new Bzip2InputStream(in);
        ObjectInputStream objIn = new ObjectInputStream(zIn);
        assertEquals("Hello World!", objIn.readObject());
    }

    @Test
    public void readBZip2File() throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("test.tar.bz2");
        Bzip2InputStream bzip2InputStream = new Bzip2InputStream(inputStream);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int i;
        byte[] data = new byte[1024];
        while ((i = bzip2InputStream.read(data, 0, data.length)) != -1) {
            outputStream.write(data, 0, i);
        }
        assertEquals(10240, outputStream.toByteArray().length);
    }
}
