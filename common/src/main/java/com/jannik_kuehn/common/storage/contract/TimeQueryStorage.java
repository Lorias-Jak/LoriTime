package com.jannik_kuehn.common.storage.contract;

import com.jannik_kuehn.common.api.storage.TimeRange;
import com.jannik_kuehn.common.api.storage.TimeScope;
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
     * Returns the stored time for a player in seconds for a specific scope.
     *
     * @param uniqueId the player UUID.
     * @param scope    the time scope.
     * @return the scoped time, if the player has stored time in that scope.
     * @throws StorageException if the lookup fails.
     */
    OptionalLong getTime(UUID uniqueId, TimeScope scope) throws StorageException;

    /**
     * Returns the stored time for a player in seconds for a specific scope and time range.
     *
     * @param uniqueId the player UUID.
     * @param scope    the time scope.
     * @param range    the time range.
     * @return the scoped time, if the player has stored time in that scope and range.
     * @throws StorageException if the lookup fails.
     */
    OptionalLong getTime(UUID uniqueId, TimeScope scope, TimeRange range) throws StorageException;

    /**
     * Returns all player time totals keyed by UUID string.
     *
     * @return all stored time totals.
     * @throws StorageException if the lookup fails.
     */
    Map<String, ?> getAllTimeEntries() throws StorageException;
}
