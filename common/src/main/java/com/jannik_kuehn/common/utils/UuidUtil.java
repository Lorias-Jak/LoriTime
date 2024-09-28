package com.jannik_kuehn.common.utils;

import java.util.UUID;

/**
 * Utility class for converting UUIDs to byte arrays and vice versa.
 */
public final class UuidUtil {

    // Privater Konstruktor, um die Instanziierung zu verhindern
    private UuidUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Converts a UUID to a byte array.
     *
     * @param uuid the UUID to convert
     * @return the byte array
     */
    public static byte[] toBytes(final UUID uuid) {
        final byte[] result = new byte[16];
        long lsb = uuid.getLeastSignificantBits();
        for (int i = 15; i >= 8; i--) {
            result[i] = (byte) (lsb & 0xffL);
            lsb >>= 8;
        }
        long msb = uuid.getMostSignificantBits();
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte) (msb & 0xffL);
            msb >>= 8;
        }
        return result;
    }

    /**
     * Converts a byte array to a UUID.
     *
     * @param bytes the byte array to convert
     * @return the UUID
     */
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
