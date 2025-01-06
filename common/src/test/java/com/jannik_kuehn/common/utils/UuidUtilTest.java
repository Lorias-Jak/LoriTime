package com.jannik_kuehn.common.utils;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UuidUtilTest {

    @Test
    void testToBytesAndFromBytes() {
        final UUID originalUuid = UUID.randomUUID();
        final byte[] bytes = UuidUtil.toBytes(originalUuid);
        final UUID convertedUuid = UuidUtil.fromBytes(bytes);
        assertNotNull(bytes, "Byte array should not be null");
        assertEquals(16, bytes.length, "Byte array should have a length of 16");
        assertEquals(originalUuid, convertedUuid, "Converted UUID should match the original UUID");
    }

    @Test
    void testFromBytesWithInvalidLength() {
        final byte[] invalidBytes = new byte[15];
        final Exception exception = assertThrows(IllegalArgumentException.class, () -> UuidUtil.fromBytes(invalidBytes));
        assertEquals("Invalid byte array length for UUID: 15", exception.getMessage());
    }

    @Test
    void testToBytesForDeterministicConversion() {
        final UUID uuid = new UUID(0x1234567890ABCDEFL, 0xFEDCBA0987654321L);
        final byte[] bytes = UuidUtil.toBytes(uuid);
        final UUID convertedUuid = UuidUtil.fromBytes(bytes);
        assertEquals(uuid, convertedUuid, "Conversion should be deterministic and preserve UUID value");
    }
}
