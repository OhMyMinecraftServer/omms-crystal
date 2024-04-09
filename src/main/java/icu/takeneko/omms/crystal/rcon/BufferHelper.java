package icu.takeneko.omms.crystal.rcon;

import java.nio.charset.StandardCharsets;

public class BufferHelper {
    public static String getString(byte[] buf, int i, int j) {
        int k = j - 1;
        int l;
        for(l = Math.min(i, k); 0 != buf[l] && l < k; ++l) {}
        return new String(buf, i, l - i, StandardCharsets.UTF_8);
    }

    public static int getIntLE(byte[] buf, int start) {
        return getIntLE(buf, start, buf.length);
    }

    public static int getIntLE(byte[] buf, int start, int limit) {
        return 0 > limit - start - 4 ? 0 : buf[start + 3] << 24 | (buf[start + 2] & 255) << 16 | (buf[start + 1] & 255) << 8 | buf[start] & 255;
    }
}
