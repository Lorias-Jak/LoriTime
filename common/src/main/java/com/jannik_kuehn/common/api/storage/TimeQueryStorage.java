package com.jannik_kuehn.common.api.storage;

import com.jannik_kuehn.common.exception.StorageException;

import java.util.Map;
import java.util.OptionalLong;
import java.util.UUID;

/**
 * Read contract for player time totals.
 */
public interface TimeQueryStorage {

    /**
     * Returns the total stored time for a player in seconds.
     *
     * @param uniqueId the player UUID.
     * @return the total time, if the player has stored time.
     * @throws StorageException if the lookup fails.
     */
    OptionalLong getTime(UUID uniqueId) throws StorageException;

    /**
     * Returns all player time totals keyed by UUID string.
     *
     * @return all stored time totals.
     * @throws StorageException if the lookup fails.
     */
    Map<String, ?> getAllTimeEntries() throws StorageException;
}
