package org.xbib.io.compress.lzf;

/**
 * Simple helper class used for loading {@link ChunkDecoder} implementations,
 * based on criteria such as "fastest available". <p> Yes, it looks butt-ugly,
 * but does the job. Nonetheless, if anyone has lipstick for this pig, let me
 * know.
 */
public class ChunkDecoderFactory {

    private static final ChunkDecoderFactory INSTANCE;

    static {
        INSTANCE = new ChunkDecoderFactory( VanillaChunkDecoder.class);
    }

    private final Class<? extends ChunkDecoder> implClass;

    @SuppressWarnings("unchecked")
    private ChunkDecoderFactory(Class<?> imp) {
        implClass = (Class<? extends ChunkDecoder>) imp;
    }

    /**
     * Method to use for getting decompressor instance that uses the most
     * optimal available methods for underlying data access. It should be safe
     * to call this method as implementations are dynamically loaded; however,
     * on some non-standard platforms it may be necessary to either directly
     * load instances, or use {@link #safeInstance()}.
     */
    public static ChunkDecoder optimalInstance() {
        try {
            return INSTANCE.implClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load a ChunkDecoder instance (" + e.getClass().getName() + "): "
                    + e.getMessage(), e);
        }
    }

    /**
     * Method that can be used to ensure that a "safe" decompressor instance is
     * loaded. Safe here means that it should work on any and all Java
     * platforms.
     */
    public static ChunkDecoder safeInstance() {
        // this will always succeed loading; no need to use dynamic class loading or instantiation
        return new VanillaChunkDecoder();
    }
}
