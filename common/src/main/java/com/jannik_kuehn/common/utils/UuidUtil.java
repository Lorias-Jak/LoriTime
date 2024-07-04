package com.jannik_kuehn.common.utils;

import java.util.UUID;

public final class UuidUtil {

    public static byte[] toBytes(final UUID id) {
        final byte[] result = new byte[16];
        long lsb = id.getLeastSignificantBits();
        for (int i = 15; i >= 8; i--) {
            result[i] = (byte) (lsb & 0xffL);
            lsb >>= 8;
        }
        long msb = id.getMostSignificantBits();
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte) (msb & 0xffL);
            msb >>= 8;
        }
        return result;
    }

    public static UUID fromBytes(final byte[] bytes) {
        long msb = 0;
        for (int i = 0; i < 8; i++) {
            msb <<= 8L;
            msb |= Byte.toUnsignedLong(bytes[i]);
        }
        long lsb = 0;
        for (int i = 8; i < 16; i++) {
            lsb <<= 8L;
            lsb |= Byte.toUnsignedLong(bytes[i]);
        }
        return new UUID(msb, lsb);
    }
}
