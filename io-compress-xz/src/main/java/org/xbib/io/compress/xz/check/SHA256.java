package org.xbib.io.compress.xz.check;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SHA256 extends Check {

    private final MessageDigest sha256;

    public SHA256() throws NoSuchAlgorithmException {
        size = 32;
        name = "SHA-256";
        sha256 = MessageDigest.getInstance("SHA-256");
    }

    public void update(byte[] buf, int off, int len) {
        sha256.update(buf, off, len);
    }

    public byte[] finish() {
        byte[] buf = sha256.digest();
        sha256.reset();
        return buf;
    }
}
