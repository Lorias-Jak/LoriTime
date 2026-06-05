package com.jannik_kuehn.common.api.storage;

import com.jannik_kuehn.common.storage.model.PlayerSessionContext;

import java.util.Objects;

/**
 * Scope for player time queries and signed adjustments.
 *
 * @param type   the scope type
 * @param server the server name for server and world scopes
 * @param world  the world name for world scopes
 */
public record TimeScope(Type type, String server, String world) {

    /**
     * Global player time scope.
     */
    public static final TimeScope GLOBAL = new TimeScope(Type.GLOBAL, null, null);

    /**
     * Creates a time scope.
     *
     * @param type   the scope type
     * @param server the server name
     * @param world  the world name
     */
    public TimeScope {
        Objects.requireNonNull(type, "type");
        switch (type) {
            case GLOBAL -> {
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
     * Creates a server time scope.
     *
     * @param server the server name
     * @return server scope
     */
    public static TimeScope server(final String server) {
        return new TimeScope(Type.SERVER, server, null);
    }

    /**
     * Creates a world time scope.
     *
     * @param server the server name
     * @param world  the world name
     * @return world scope
     */
    public static TimeScope world(final String server, final String world) {
        return new TimeScope(Type.WORLD, server, world);
    }

    private static String requireNonBlank(final String value, final String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    /**
     * Returns whether a session context contributes to this scope.
     *
     * @param context the session context
     * @return true when the context matches
     */
    public boolean matches(final PlayerSessionContext context) {
        Objects.requireNonNull(context, "context");
        return switch (type) {
            case GLOBAL -> true;
            case SERVER -> server.equals(context.server());
            case WORLD -> server.equals(context.server()) && world.equals(context.world());
        };
    }

    /**
     * Scope discriminator.
     */
    public enum Type {
        GLOBAL,
        SERVER,
        WORLD
    }
}
