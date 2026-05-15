package com.jannik_kuehn.common.api.storage;

import com.jannik_kuehn.common.exception.StorageException;

import java.util.Optional;
import java.util.UUID;

/**
 * Tracks active player sessions and persists their elapsed online time.
 */
public interface TimeAccumulator extends AutoCloseable {

    /**
     * Starts tracking an active player session.
     *
     * @param uuid the player UUID.
     * @param name the player name, or null when unknown.
     * @param server the logical server context.
     * @param world the world context.
     * @param when the start timestamp in epoch milliseconds.
     * @throws StorageException if the session cannot be created.
     */
    void startAccumulating(UUID uuid, String name, String server, String world, long when) throws StorageException;

    /**
     * Starts tracking an active player session with default context.
     *
     * @param uuid the player UUID.
     * @param when the start timestamp in epoch milliseconds.
     * @throws StorageException if the session cannot be created.
     */
    default void startAccumulating(final UUID uuid, final long when) throws StorageException {
        startAccumulating(uuid, null, "default", "global", when);
    }

    /**
     * Stops tracking a player and updates the active session row.
     *
     * @param uuid the player UUID.
     * @param when the stop timestamp in epoch milliseconds.
     * @param reason the stop reason.
     * @throws StorageException if the session cannot be updated.
     */
    void stopAccumulatingAndSaveOnlineTime(UUID uuid, long when, TimeEntryReason reason) throws StorageException;

    /**
     * Stops tracking a player with the default leave reason.
     *
     * @param uuid the player UUID.
     * @param when the stop timestamp in epoch milliseconds.
     * @throws StorageException if the session cannot be updated.
     */
    default void stopAccumulatingAndSaveOnlineTime(final UUID uuid, final long when) throws StorageException {
        stopAccumulatingAndSaveOnlineTime(uuid, when, TimeEntryReason.PLAYER_LEAVE);
    }

    /**
     * Switches the active session context for a player.
     *
     * @param uuid the player UUID.
     * @param name the player name, or null when unknown.
     * @param server the next logical server context.
     * @param world the next world context.
     * @param when the switch timestamp in epoch milliseconds.
     * @throws StorageException if the old or new session cannot be persisted.
     */
    void switchContext(UUID uuid, String name, String server, String world, long when) throws StorageException;

    /**
     * Updates the current world context for a master-owned active session.
     *
     * @param uuid the player UUID.
     * @param world the current world context.
     * @param observedAtMs the observation timestamp in epoch milliseconds.
     * @throws StorageException if the active session cannot be updated.
     */
    void updateWorldContext(UUID uuid, String world, long observedAtMs) throws StorageException;

    /**
     * Flushes active session progress to storage.
     *
     * @throws StorageException if any session cannot be updated.
     */
    void flushOnlineTimeCache() throws StorageException;

    /**
     * Creates a session context from nullable player name input.
     *
     * @param uuid the player UUID.
     * @param name the player name, or null when unknown.
     * @param server the logical server context.
     * @param world the world context.
     * @param when the start timestamp in epoch milliseconds.
     * @return the session context.
     */
    default PlayerSessionContext context(final UUID uuid, final String name, final String server, final String world, final long when) {
        return new PlayerSessionContext(uuid, Optional.ofNullable(name), server, world, when);
    }

    /**
     * Closes the accumulator and persists remaining active sessions.
     *
     * @throws StorageException if closing fails.
     */
    @Override
    void close() throws StorageException;
}
