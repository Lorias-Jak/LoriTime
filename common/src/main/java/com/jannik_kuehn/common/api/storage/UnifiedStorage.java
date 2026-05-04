package com.jannik_kuehn.common.api.storage;

import com.jannik_kuehn.common.exception.StorageException;

import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;

/**
 * Unified runtime storage contract for player identity and time persistence.
 */
@SuppressWarnings("PMD.CommentRequired")
public interface UnifiedStorage extends AutoCloseable {

    Optional<UUID> getUuid(String playerName) throws StorageException;

    Optional<String> getName(UUID uniqueId) throws StorageException;

    void setPlayerName(UUID uniqueId, String name) throws StorageException;

    void setPlayerNames(Map<UUID, String> entries) throws StorageException;

    Set<String> getNameEntries() throws StorageException;

    OptionalLong getTime(UUID uniqueId) throws StorageException;

    void addTime(UUID uuid, long additionalTime, TimeEntryReason reason) throws StorageException;

    default void addTime(final UUID uuid, final long additionalTime) throws StorageException {
        addTime(uuid, additionalTime, TimeEntryReason.MANUAL_ADJUSTMENT);
    }

    void addTimes(Map<UUID, Long> additionalTimes, TimeEntryReason reason) throws StorageException;

    default void addTimes(final Map<UUID, Long> additionalTimes) throws StorageException {
        addTimes(additionalTimes, TimeEntryReason.MANUAL_ADJUSTMENT);
    }

    void persistSession(PlayerSessionChunk session) throws StorageException;

    Map<String, ?> getAllTimeEntries() throws StorageException;

    void removePlayer(UUID uniqueId) throws StorageException, SQLException;

    @Override
    void close() throws StorageException;
}
