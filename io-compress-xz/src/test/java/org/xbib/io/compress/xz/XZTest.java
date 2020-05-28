package org.xbib.io.compress.xz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class XZTest {

    @Test
    public void testHelloWorld() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XZOutputStream zOut = new XZOutputStream(out);
        ObjectOutputStream objOut = new ObjectOutputStream(zOut);
        String helloWorld = "Hello World!";
        objOut.writeObject(helloWorld);
        zOut.close();
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        XZInputStream zIn = new XZInputStream(in);
        ObjectInputStream objIn = new ObjectInputStream(zIn);
        assertEquals("Hello World!", objIn.readObject());
    }

    @Test
    public void readXZFile() throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("test.xz");
        XZInputStream xzInputStream = new XZInputStream(inputStream);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int i;
        byte[] data = new byte[1024];
        while ((i = xzInputStream.read(data, 0, data.length)) != -1) {
            outputStream.write(data, 0, i);
        }
        assertEquals("Hello world\n", new String(outputStream.toByteArray()));
    }
}
