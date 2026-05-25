package com.jannik_kuehn.common.api.storage;

import com.jannik_kuehn.common.exception.StorageException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Write contract for signed time adjustments.
 */
public interface TimeAdjustmentStorage {

    /**
     * Adds a signed time adjustment without explicit actor metadata.
     *
     * @param uuid the target player UUID.
     * @param additionalTime the signed amount in seconds.
     * @param reason the adjustment reason.
     * @throws StorageException if the write fails.
     */
    void addTime(UUID uuid, long additionalTime, TimeEntryReason reason) throws StorageException;

    /**
     * Adds a signed time adjustment without explicit actor metadata for a scope.
     *
     * @param uuid the target player UUID.
     * @param additionalTime the signed amount in seconds.
     * @param reason the adjustment reason.
     * @param scope the adjustment scope.
     * @throws StorageException if the write fails.
     */
    default void addTime(final UUID uuid, final long additionalTime, final TimeEntryReason reason,
                         final TimeScope scope) throws StorageException {
        addTime(new ManualTimeAdjustment(uuid, additionalTime, reason, "SYSTEM", scope));
    }

    /**
     * Adds a manual signed time adjustment without explicit actor metadata.
     *
     * @param uuid the target player UUID.
     * @param additionalTime the signed amount in seconds.
     * @throws StorageException if the write fails.
     */
    default void addTime(final UUID uuid, final long additionalTime) throws StorageException {
        addTime(uuid, additionalTime, TimeEntryReason.MANUAL_ADJUSTMENT);
    }

    /**
     * Persists an actor-aware signed time adjustment.
     *
     * @param adjustment the adjustment to persist.
     * @throws StorageException if the write fails.
     */
    void addTime(ManualTimeAdjustment adjustment) throws StorageException;

    /**
     * Persists an actor-aware signed time adjustment.
     *
     * @param uuid the target player UUID.
     * @param additionalTime the signed amount in seconds.
     * @param reason the adjustment reason.
     * @param actorUuid the actor UUID, or null for console/system actors.
     * @param actorName the actor display name.
     * @throws StorageException if the write fails.
     */
    default void addTime(final UUID uuid, final long additionalTime, final TimeEntryReason reason,
                         final UUID actorUuid, final String actorName) throws StorageException {
        addTime(new ManualTimeAdjustment(uuid, additionalTime, reason, actorUuid, actorName));
    }

    /**
     * Persists an actor-aware signed time adjustment for a scope.
     *
     * @param uuid the target player UUID.
     * @param additionalTime the signed amount in seconds.
     * @param reason the adjustment reason.
     * @param actorUuid the actor UUID, or null for console/system actors.
     * @param actorName the actor display name.
     * @param scope the adjustment scope.
     * @throws StorageException if the write fails.
     */
    default void addTime(final UUID uuid, final long additionalTime, final TimeEntryReason reason,
                         final UUID actorUuid, final String actorName, final TimeScope scope) throws StorageException {
        addTime(new ManualTimeAdjustment(uuid, additionalTime, reason, actorUuid, actorName, scope));
    }

    /**
     * Adds signed time adjustments for multiple players without explicit actor metadata.
     *
     * @param additionalTimes target UUID to signed seconds.
     * @param reason the adjustment reason.
     * @throws StorageException if the write fails.
     */
    void addTimes(Map<UUID, Long> additionalTimes, TimeEntryReason reason) throws StorageException;

    /**
     * Adds manual signed time adjustments for multiple players without explicit actor metadata.
     *
     * @param additionalTimes target UUID to signed seconds.
     * @throws StorageException if the write fails.
     */
    default void addTimes(final Map<UUID, Long> additionalTimes) throws StorageException {
        addTimes(additionalTimes, TimeEntryReason.MANUAL_ADJUSTMENT);
    }

    /**
     * Persists actor-aware signed time adjustments.
     *
     * @param adjustments the adjustments to persist.
     * @throws StorageException if the write fails.
     */
    void addAdjustments(List<ManualTimeAdjustment> adjustments) throws StorageException;
}
