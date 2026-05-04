package com.jannik_kuehn.common.api.storage;

import java.util.Locale;

/**
 * Storage responsibility mode for a LoriTime instance.
 */
public enum StorageMode {

    /**
     * Local canonical storage reader and writer.
     */
    STANDALONE,

    /**
     * Canonical storage authority for a multi-setup.
     */
    MASTER,

    /**
     * Remote client that reports writes to a master and keeps local read projections.
     */
    SLAVE;

    /**
     * Parses a configured storage mode.
     *
     * @param value configured value
     * @return parsed storage mode
     */
    public static StorageMode parse(final String value) {
        if (value == null || value.isBlank()) {
            return STANDALONE;
        }
        return StorageMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    /**
     * Returns the lower-case config value for this mode.
     *
     * @return lower-case config value
     */
    public String configValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
