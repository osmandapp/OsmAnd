package com.wdtinc.mapbox_vector_tile.encoding;

/**
 * See: <a href="https://developers.google.com/protocol-buffers/docs/encoding#types">Google Protocol Buffers Docs</a>
 */
public final class ZigZag {

    /**
     * See: <a href="https://developers.google.com/protocol-buffers/docs/encoding#types">Google Protocol Buffers Docs</a>
     *
     * @param n integer to encode
     * @return zig-zag encoded integer
     */
    public static int encode(int n) {
        return (n << 1) ^ (n >> 31);
    }

    /**
     * See: <a href="https://developers.google.com/protocol-buffers/docs/encoding#types">Google Protocol Buffers Docs</a>
     *
     * @param n zig-zag encoded integer to decode
     * @return decoded integer
     */
    public static int decode(int n) {
        return (n >> 1) ^ (-(n & 1));
    }
}
