package org.xbib.io.archive.tar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

public class TarTest {

    @Test
    public void testReadTar() throws IOException {
        InputStream in = getClass().getResourceAsStream("test.tar");
        TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(in);
        byte[] buffer = new byte[1024];
        long total = 0L;
        while (tarArchiveInputStream.getNextEntry() != null) {
            int len = 0;
            while ((len = tarArchiveInputStream.read(buffer)) > 0) {
                total += len;
            }
        }
        assertEquals(1889L, total);
        tarArchiveInputStream.close();
    }
}
