package com.jannik_kuehn.common.api.storage;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistable player session chunk.
 *
 * @param uuid      player UUID
 * @param name      latest known player name
 * @param server    server context
 * @param world     world context
 * @param startedAt session start timestamp in milliseconds
 * @param stoppedAt session stop timestamp in milliseconds
 * @param reason    persistence reason
 */
public record PlayerSessionChunk(UUID uuid, Optional<String> name, String server, String world,
                                 long startedAt, long stoppedAt, TimeEntryReason reason) {

    /**
     * Creates a session chunk from an active context.
     *
     * @param context   active session context
     * @param stoppedAt stop timestamp in milliseconds
     * @param reason    persistence reason
     * @return session chunk
     */
    public static PlayerSessionChunk from(final PlayerSessionContext context, final long stoppedAt, final TimeEntryReason reason) {
        return new PlayerSessionChunk(context.uuid(), context.name(), context.server(), context.world(),
                context.startedAtMs(), stoppedAt, reason);
    }

    /**
     * Returns the chunk duration in seconds.
     *
     * @return duration in seconds
     */
    public long durationSeconds() {
        return Math.max(0, (stoppedAt - startedAt) / 1000L);
    }
}
