package com.jannik_kuehn.loritime.common.utils;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UuidUtil {

    public static final Pattern UUID_PATTERN = Pattern.compile("([0-9a-f]{8})-?([0-9a-f]{4})-?([0-9a-f]{4})-?([0-9a-f]{4})-?([0-9a-f]{12})", Pattern.CASE_INSENSITIVE);

    public static byte[] toBytes(UUID id) {
        byte[] result = new byte[16];
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

    public static UUID fromBytes(byte[] bytes) {
        long msb = 0;
        for (int i = 0; i < 8; i++) {
            msb <<= 8L;
            msb |= Byte.toUnsignedLong(bytes[i]);
        }
        long lsb = 0;
        for (int i = 8; i < 16; i++) {
            lsb <<= 8L;
            lsb |= Byte.toUnsignedLong(bytes[i]);;
        }
        return new UUID(msb, lsb);
    }

    public static Optional<UUID> fromString(String string) {
        Matcher uuidMatcher = UuidUtil.UUID_PATTERN.matcher(string);
        if (uuidMatcher.matches()) {
            return Optional.of(UUID.fromString(uuidMatcher.group(1) + "-" + uuidMatcher.group(2) + "-" + uuidMatcher.group(3) + "-" + uuidMatcher.group(4) + "-" + uuidMatcher.group(5)));
        } else {
            return Optional.empty();
        }
    }

    private UuidUtil() {}
}
