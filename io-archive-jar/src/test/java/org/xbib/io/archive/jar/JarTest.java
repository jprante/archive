package org.xbib.io.archive.jar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import java.io.InputStream;

public class JarTest {

    @Test
    public void testJar() throws Exception {
        InputStream in = getClass().getResourceAsStream("test.jar");
        JarArchiveInputStream jarArchiveInputStream = new JarArchiveInputStream(in);
        byte[] buffer = new byte[1024];
        long total = 0L;
        while ((jarArchiveInputStream.getNextEntry()) != null) {
            int len = 0;
            while ((len = jarArchiveInputStream.read(buffer)) > 0) {
                total += len;
            }
        }
        assertEquals(1813L, total);
        jarArchiveInputStream.close();
    }
}
