package com.jannik_kuehn.common.api.storage;

import java.util.Objects;

/**
 * Scope used by admin storage maintenance operations.
 *
 * @param type   scope type
 * @param server server name for server/world scopes
 * @param world  world name for world scopes
 */
public record StorageMaintenanceScope(Type type, String server, String world) {

    public StorageMaintenanceScope {
        Objects.requireNonNull(type, "type");
        switch (type) {
            case STORAGE -> {
                server = null;
                world = null;
            }
            case SERVER -> {
                server = requireNonBlank(server, "server");
                world = null;
            }
            case WORLD -> {
                server = requireNonBlank(server, "server");
                world = requireNonBlank(world, "world");
            }
        }
    }

    /**
     * Creates a storage-wide scope.
     *
     * @return storage scope
     */
    public static StorageMaintenanceScope storage() {
        return new StorageMaintenanceScope(Type.STORAGE, null, null);
    }

    /**
     * Creates a server scope.
     *
     * @param server server name
     * @return server scope
     */
    public static StorageMaintenanceScope server(final String server) {
        return new StorageMaintenanceScope(Type.SERVER, server, null);
    }

    /**
     * Creates a world scope.
     *
     * @param server server name
     * @param world  world name
     * @return world scope
     */
    public static StorageMaintenanceScope world(final String server, final String world) {
        return new StorageMaintenanceScope(Type.WORLD, server, world);
    }

    private static String requireNonBlank(final String value, final String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    /**
     * Maintenance scope type.
     */
    public enum Type {
        STORAGE,
        SERVER,
        WORLD
    }
}
