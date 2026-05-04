package com.jannik_kuehn.common.api.storage;

import com.jannik_kuehn.common.exception.StorageException;

import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("PMD.CommentRequired")
public interface TimeAccumulator extends AutoCloseable {

    void startAccumulating(UUID uuid, String name, String server, String world, long when) throws StorageException;

    void stopAccumulatingAndSaveOnlineTime(UUID uuid, String server, String world, long when, TimeEntryReason reason) throws StorageException;

    void switchContext(UUID uuid, String name, String server, String world, long when) throws StorageException;

    void flushOnlineTimeCache() throws StorageException;

    default void startAccumulating(final UUID uuid, final long when) throws StorageException {
        startAccumulating(uuid, null, "default", "global", when);
    }

    default void stopAccumulatingAndSaveOnlineTime(final UUID uuid, final long when) throws StorageException {
        stopAccumulatingAndSaveOnlineTime(uuid, "default", "global", when, TimeEntryReason.PLAYER_LEAVE);
    }

    default void stopAccumulatingAndSaveOnlineTime(final UUID uuid, final String server, final String world, final long when)
            throws StorageException {
        stopAccumulatingAndSaveOnlineTime(uuid, server, world, when, TimeEntryReason.PLAYER_LEAVE);
    }

    default PlayerSessionContext context(final UUID uuid, final String name, final String server, final String world, final long when) {
        return new PlayerSessionContext(uuid, Optional.ofNullable(name), server, world, when);
    }

    @Override
    void close() throws StorageException;
}
