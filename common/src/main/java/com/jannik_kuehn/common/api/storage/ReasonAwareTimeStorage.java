package com.jannik_kuehn.common.api.storage;

import com.jannik_kuehn.common.exception.StorageException;

import java.util.Map;
import java.util.UUID;

/**
 * Extended time storage contract that supports writing reasons per persisted entry.
 */
public interface ReasonAwareTimeStorage extends TimeStorage {

    /**
     * Adds the given time with a specific persistence reason.
     *
     * @param uuid player uuid
     * @param additionalTime additional time in seconds
     * @param reason persistence reason
     * @throws StorageException when persisting fails
     */
    void addTime(UUID uuid, long additionalTime, TimeEntryReason reason) throws StorageException;

    /**
     * Adds the given times with a specific persistence reason.
     *
     * @param additionalTimes additional times in seconds per player
     * @param reason persistence reason
     * @throws StorageException when persisting fails
     */
    void addTimes(Map<UUID, Long> additionalTimes, TimeEntryReason reason) throws StorageException;

    @Override
    default void addTime(final UUID uuid, final long additionalTime) throws StorageException {
        addTime(uuid, additionalTime, TimeEntryReason.UNSPECIFIED);
    }

    @Override
    default void addTimes(final Map<UUID, Long> additionalTimes) throws StorageException {
        addTimes(additionalTimes, TimeEntryReason.UNSPECIFIED);
    }
}

