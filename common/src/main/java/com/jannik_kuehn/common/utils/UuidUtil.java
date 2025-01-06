package com.jannik_kuehn.common.utils;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Utility class for converting UUIDs to byte arrays and vice versa.
 */
public final class UuidUtil {
    /**
     * The length of a UUID in bytes.
     */
    private static final int UUID_BYTE_LENGTH = 16;

    private UuidUtil() {
    }

    /**
     * Converts a UUID to a byte array.
     *
     * @param uuid the UUID to convert
     * @return the byte array
     */
    public static byte[] toBytes(final UUID uuid) {
        final ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return buffer.array();
    }

    /**
     * Converts a byte array to a UUID.
     *
     * @param bytes the byte array to convert
     * @return the UUID
     * @throws IllegalArgumentException if the byte array is not 16 bytes long
     */
    public static UUID fromBytes(final byte[] bytes) {
        if (bytes.length != UUID_BYTE_LENGTH) {
            throw new IllegalArgumentException("Invalid byte array length for UUID: " + bytes.length);
        }
        final ByteBuffer buffer = ByteBuffer.wrap(bytes);
        final long msb = buffer.getLong();
        final long lsb = buffer.getLong();
        return new UUID(msb, lsb);
    }
}
