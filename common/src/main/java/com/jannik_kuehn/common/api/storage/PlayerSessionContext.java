package com.jannik_kuehn.common.api.storage;

import java.util.Optional;
import java.util.UUID;

/**
 * Context for an active player session.
 *
 * @param uuid        player UUID
 * @param name        latest known player name
 * @param server      server context
 * @param world       world context
 * @param startedAtMs session start timestamp in milliseconds
 */
public record PlayerSessionContext(UUID uuid, Optional<String> name, String server, String world, long startedAtMs) {

    /**
     * Creates a session context.
     *
     * @param uuid        player UUID
     * @param name        latest known player name
     * @param server      server context
     * @param world       world context
     * @param startedAtMs session start timestamp in milliseconds
     */
    public PlayerSessionContext(final UUID uuid, final String name, final String server, final String world, final long startedAtMs) {
        this(uuid, Optional.ofNullable(name), server, world, startedAtMs);
    }
}
