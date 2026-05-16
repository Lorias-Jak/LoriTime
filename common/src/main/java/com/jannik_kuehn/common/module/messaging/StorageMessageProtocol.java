package com.jannik_kuehn.common.module.messaging;

import java.util.Arrays;
import java.util.Optional;

/**
 * Versioned storage plugin message protocol.
 */
public final class StorageMessageProtocol {

    /**
     * Current storage plugin message protocol version.
     */
    public static final int VERSION = 2;

    private StorageMessageProtocol() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Parses a storage operation payload value.
     *
     * @param value raw operation value
     * @return operation type when valid
     */
    public static Optional<StorageMessageType> parseType(final String value) {
        return Arrays.stream(StorageMessageType.values())
                .filter(type -> type.wireValue().equals(value))
                .findFirst();
    }
}
