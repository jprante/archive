package org.xbib.io.codec;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.WeakHashMap;

/**
 *
 */
public class StreamCodecService {

    private final static Map<String, StreamCodec<InputStream, OutputStream>> codecs = new WeakHashMap<>();

    private final static StreamCodecService instance = new StreamCodecService();

    @SuppressWarnings({"rawtypes","unchecked"})
    private StreamCodecService() {
        ServiceLoader<StreamCodec> loader = ServiceLoader.load(StreamCodec.class);
        for (StreamCodec<InputStream, OutputStream> codec : loader) {
            if (!codecs.containsKey(codec.getName())) {
                codecs.put(codec.getName(), codec);
            }
        }
    }

    public static StreamCodecService getInstance() {
        return instance;
    }

    public StreamCodec<InputStream, OutputStream> getCodec(String suffix) {
        if (codecs.containsKey(suffix)) {
            return codecs.get(suffix);
        }
        throw new IllegalArgumentException("Stream codec for " + suffix + " not found in " + codecs);
    }

    public static Set<String> getCodecs() {
        return codecs.keySet();
    }
}
