package com.jannik_kuehn.common.storage.model;

/**
 * Shared fallback session context values.
 */
public final class SessionContextDefaults {

    /**
     * Fallback server context for paths without a canonical server.
     */
    public static final String SERVER = "default";

    /**
     * Fallback world context for paths without a current world.
     */
    public static final String WORLD = "global";

    private SessionContextDefaults() {
        throw new UnsupportedOperationException("Utility class");
    }
}
