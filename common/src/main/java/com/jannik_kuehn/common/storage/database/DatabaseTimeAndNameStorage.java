package com.jannik_kuehn.common.storage.database;

import com.jannik_kuehn.common.api.storage.NameStorage;
import com.jannik_kuehn.common.api.storage.ReasonAwareTimeStorage;
import com.jannik_kuehn.common.api.storage.TimeEntryReason;
import com.jannik_kuehn.common.api.storage.TimeStorage;
import com.jannik_kuehn.common.exception.StorageException;
import com.jannik_kuehn.common.storage.database.provider.LoriTimeConnectionProvider;
import com.jannik_kuehn.common.storage.database.table.PlayerTable;
import com.jannik_kuehn.common.storage.database.table.ServerTable;
import com.jannik_kuehn.common.storage.database.table.TimeTable;
import com.jannik_kuehn.common.storage.database.table.WorldTable;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@SuppressWarnings({"PMD.CommentRequired", "PMD.UnusedPrivateField", "PMD.TooManyMethods"})
public class DatabaseTimeAndNameStorage implements NameStorage, TimeStorage, ReasonAwareTimeStorage {

    private static final String DEFAULT_SERVER_NAME = "default";

    private static final String DEFAULT_WORLD_NAME = "global";

    private final LoriTimeConnectionProvider provider;

    private final PlayerTable playerTable;

    private final ServerTable serverTable;

    private final WorldTable worldTable;

    private final TimeTable timeTable;

    private final ReadWriteLock poolLock;

    public DatabaseTimeAndNameStorage(final LoriTimeConnectionProvider provider,
                                      final PlayerTable playerTable, final ServerTable serverTable,
                                      final WorldTable worldTable, final TimeTable timeTable) {
        this.provider = provider;
        this.playerTable = playerTable;
        this.serverTable = serverTable;
        this.worldTable = worldTable;
        this.timeTable = timeTable;
        this.poolLock = new ReentrantReadWriteLock();
    }

    @Override
    public Optional<UUID> getUuid(final String name) throws StorageException {
        Objects.requireNonNull(name);
        poolLock.readLock().lock();
        try {

            checkClosed();
            try (Connection connection = provider.getConnection()) {
                return playerTable.findUuidByName(connection, name);
            }
        } catch (final SQLException ex) {
            throw new StorageException(ex);
        } finally {
            poolLock.readLock().unlock();
        }
    }

    @Override
    public Optional<String> getName(final UUID uniqueId) throws StorageException {
        Objects.requireNonNull(uniqueId);
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = provider.getConnection()) {
                return playerTable.findNameByUuid(connection, uniqueId);
            }
        } catch (final SQLException ex) {
            throw new StorageException(ex);
        } finally {
            poolLock.readLock().unlock();
        }
    }

    @Override
    public OptionalLong getTime(final UUID uniqueId) throws StorageException {
        Objects.requireNonNull(uniqueId);
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = provider.getConnection()) {
                return timeTable.sumForPlayer(connection, uniqueId);
            }
        } catch (final SQLException ex) {
            throw new StorageException(ex);
        } finally {
            poolLock.readLock().unlock();
        }
    }

    @Override
    public void addTime(final UUID uuid, final long additionalTime) throws StorageException {
        addTime(uuid, additionalTime, TimeEntryReason.MANUAL_ADJUSTMENT);
    }

    @Override
    public void addTime(final UUID uuid, final long additionalTime, final TimeEntryReason reason) throws StorageException {
        Objects.requireNonNull(uuid);
        Objects.requireNonNull(reason);
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = provider.getConnection()) {
                final long worldId = worldTable.ensureWorld(connection, DEFAULT_SERVER_NAME, DEFAULT_WORLD_NAME);
                final long playerId = playerTable.ensurePlayer(connection, uuid, Optional.empty());
                timeTable.insertDuration(connection, playerId, worldId, additionalTime, reason);
            }
        } catch (final SQLException ex) {
            throw new StorageException(ex);
        } finally {
            poolLock.readLock().unlock();
        }
    }

    @Override
    public void addTimes(final Map<UUID, Long> additionalTimes) throws StorageException {
        addTimes(additionalTimes, TimeEntryReason.MANUAL_ADJUSTMENT);
    }

    @Override
    public void addTimes(final Map<UUID, Long> additionalTimes, final TimeEntryReason reason) throws StorageException {
        if (additionalTimes == null || additionalTimes.isEmpty()) {
            return;
        }
        Objects.requireNonNull(reason);
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = provider.getConnection()) {
                final long worldId = worldTable.ensureWorld(connection, DEFAULT_SERVER_NAME, DEFAULT_WORLD_NAME);
                for (final Map.Entry<UUID, Long> entry : additionalTimes.entrySet()) {
                    final UUID uuid = entry.getKey();
                    final long playerId = playerTable.ensurePlayer(connection, uuid, Optional.empty());
                    timeTable.insertDuration(connection, playerId, worldId, entry.getValue(), reason);
                }
            }
        } catch (final SQLException ex) {
            throw new StorageException(ex);
        } finally {
            poolLock.readLock().unlock();
        }
    }

    @Override
    public void setEntry(final UUID uuid, final String name) throws StorageException {
        Objects.requireNonNull(uuid);
        Objects.requireNonNull(name);
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = provider.getConnection()) {
                playerTable.ensurePlayer(connection, uuid, Optional.of(name));
            }
        } catch (final SQLException ex) {
            throw new StorageException(ex);
        } finally {
            poolLock.readLock().unlock();
        }
    }

    @Override
    public void setEntry(final UUID uniqueId, final String name, final boolean override) throws StorageException {
        setEntry(uniqueId, name);
    }

    @Override
    public void setEntries(final Map<UUID, String> entries) throws StorageException {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = provider.getConnection()) {
                for (final Map.Entry<UUID, String> entry : entries.entrySet()) {
                    playerTable.ensurePlayer(connection, entry.getKey(), Optional.ofNullable(entry.getValue()));
                }
            }
        } catch (final SQLException ex) {
            throw new StorageException(ex);
        } finally {
            poolLock.readLock().unlock();
        }
    }

    @Override
    public Set<String> getNameEntries() throws StorageException {
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = provider.getConnection()) {
                return playerTable.getAllNames(connection);
            }
        } catch (final SQLException ex) {
            throw new StorageException(ex);
        } finally {
            poolLock.readLock().unlock();
        }
    }

    @Override
    public Map<String, ?> getAllTimeEntries() throws StorageException {
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = provider.getConnection()) {
                return timeTable.getAllTotals(connection);
            }
        } catch (final SQLException ex) {
            throw new StorageException(ex);
        } finally {
            poolLock.readLock().unlock();
        }
    }

    @Override
    public void removeUser(final UUID uniqueId) throws StorageException, SQLException {
        deleteUser(uniqueId);
    }

    @Override
    public void removeTimeHolder(final UUID uniqueId) throws StorageException, SQLException {
        deleteUser(uniqueId);
    }

    /**
     * Deletes a player entry by UUID.
     *
     * @param uuid the player UUID
     * @throws StorageException if the deletion fails
     */
    private void deleteUser(final UUID uuid) throws StorageException {
        if (uuid == null) {
            return;
        }
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = provider.getConnection()) {
                playerTable.deleteByUuid(connection, uuid);
            }
        } catch (final SQLException ex) {
            throw new StorageException(ex);
        } finally {
            poolLock.readLock().unlock();
        }
    }

    @Override
    public void close() throws StorageException {
        if (!provider.isClosed()) {
            try {
                provider.close();
            } catch (final IOException e) {
                throw new StorageException("The database could not be closed properly.", e);
            }
        }
    }

    /**
     * Throws an exception if the storage is already closed.
     *
     * @throws StorageException if the storage is closed
     */
    private void checkClosed() throws StorageException {
        if (provider.isClosed()) {
            throw new StorageException("closed");
        }
    }
}
