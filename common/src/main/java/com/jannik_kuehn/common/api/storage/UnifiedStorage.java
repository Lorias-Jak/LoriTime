package com.jannik_kuehn.common.api.storage;

import com.jannik_kuehn.common.exception.StorageException;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;

/**
 * Unified runtime storage contract for player identity and time persistence.
 */
@SuppressWarnings("PMD.TooManyMethods")
public interface UnifiedStorage extends AutoCloseable {

    /**
     * Looks up a stored UUID by player name.
     *
     * @param playerName the player name.
     * @return the UUID, if known.
     * @throws StorageException if the lookup fails.
     */
    Optional<UUID> getUuid(String playerName) throws StorageException;

    /**
     * Looks up the latest stored player name for a UUID.
     *
     * @param uniqueId the player UUID.
     * @return the player name, if known.
     * @throws StorageException if the lookup fails.
     */
    Optional<String> getName(UUID uniqueId) throws StorageException;

    /**
     * Stores or updates a player name.
     *
     * @param uniqueId the player UUID.
     * @param name the player name.
     * @throws StorageException if the write fails.
     */
    void setPlayerName(UUID uniqueId, String name) throws StorageException;

    /**
     * Stores or updates multiple player names.
     *
     * @param entries UUID to name entries.
     * @throws StorageException if the write fails.
     */
    void setPlayerNames(Map<UUID, String> entries) throws StorageException;

    /**
     * Returns all stored player names.
     *
     * @return the known player names.
     * @throws StorageException if the lookup fails.
     */
    Set<String> getNameEntries() throws StorageException;

    /**
     * Returns the total stored time for a player in seconds.
     *
     * @param uniqueId the player UUID.
     * @return the total time, if the player has stored time.
     * @throws StorageException if the lookup fails.
     */
    OptionalLong getTime(UUID uniqueId) throws StorageException;

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

    /**
     * Creates a persisted active session row.
     *
     * @param context the session context.
     * @param reason the creation reason.
     * @return the persisted session id.
     * @throws StorageException if the write fails.
     */
    long startSession(PlayerSessionContext context, TimeEntryReason reason) throws StorageException;

    /**
     * Updates a persisted session row with a stop timestamp and reason.
     *
     * @param sessionId the persisted session id.
     * @param stoppedAtMs the stop timestamp in epoch milliseconds.
     * @param reason the update reason.
     * @throws StorageException if the write fails.
     */
    void updateSession(long sessionId, long stoppedAtMs, TimeEntryReason reason) throws StorageException;

    /**
     * Updates the world context for an existing session row without changing its time range.
     *
     * @param sessionId the persisted session id.
     * @param server the canonical server context.
     * @param world the current world context.
     * @throws StorageException if the update fails.
     */
    void updateSessionWorld(long sessionId, String server, String world) throws StorageException;

    /**
     * Persists a completed session chunk.
     *
     * @param session the session chunk.
     * @throws StorageException if the write fails.
     */
    void persistSession(PlayerSessionChunk session) throws StorageException;

    /**
     * Returns all player time totals keyed by UUID string.
     *
     * @return all stored time totals.
     * @throws StorageException if the lookup fails.
     */
    Map<String, ?> getAllTimeEntries() throws StorageException;

    /**
     * Deletes a player identity and associated history.
     *
     * @param uniqueId the player UUID.
     * @throws StorageException if the delete fails.
     * @throws SQLException if the storage backend reports a SQL failure.
     */
    void deletePlayer(UUID uniqueId) throws StorageException, SQLException;

    /**
     * Deletes only history rows for inactive players.
     *
     * @param inactiveDays the inactivity threshold in days.
     * @return the number of deleted history rows.
     * @throws StorageException if cleanup fails.
     */
    int deleteInactiveHistory(long inactiveDays) throws StorageException;

    /**
     * Closes the storage.
     *
     * @throws StorageException if closing fails.
     */
    @Override
    void close() throws StorageException;
}
