package org.xbib.io.archive.zip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import java.io.InputStream;

public class ZipTest {

    @Test
    public void testZip() throws Exception {
        InputStream in = getClass().getResourceAsStream("test.zip");
        ZipArchiveInputStream z = new ZipArchiveInputStream(in);
        byte[] buffer = new byte[1024];
        long total = 0L;
        while ((z.getNextEntry()) != null) {
            int len = 0;
            while ((len = z.read(buffer)) > 0) {
                total += len;
            }
        }
        assertEquals(1813L, total);
        z.close();
    }
}
