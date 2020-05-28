module org.xbib.io.codec {
    exports org.xbib.io.codec;
    exports org.xbib.io.codec.ar;
    exports org.xbib.io.codec.cpio;
    exports org.xbib.io.codec.file;
    exports org.xbib.io.codec.jar;
    exports org.xbib.io.codec.tar;
    exports org.xbib.io.codec.zip;
    requires org.xbib.io.compress.bzip;
    requires org.xbib.io.compress.lzf;
    requires org.xbib.io.compress.xz;
    requires org.xbib.io.compress.zlib;
    requires org.xbib.io.archive;
    requires org.xbib.io.archive.ar;
    requires org.xbib.io.archive.cpio;
    requires org.xbib.io.archive.dump;
    requires org.xbib.io.archive.jar;
    requires org.xbib.io.archive.tar;
    requires org.xbib.io.archive.zip;
}
