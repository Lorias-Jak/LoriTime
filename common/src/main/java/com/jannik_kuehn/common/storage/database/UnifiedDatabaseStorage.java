package com.jannik_kuehn.common.storage.database;

import com.jannik_kuehn.common.api.storage.ManualTimeAdjustment;
import com.jannik_kuehn.common.api.storage.PlayerSessionChunk;
import com.jannik_kuehn.common.api.storage.PlayerSessionContext;
import com.jannik_kuehn.common.api.storage.TimeEntryReason;
import com.jannik_kuehn.common.api.storage.UnifiedStorage;
import com.jannik_kuehn.common.exception.StorageException;
import com.jannik_kuehn.common.storage.database.provider.LoriTimeConnectionProvider;
import com.jannik_kuehn.common.storage.database.table.ManualAdjustmentTable;
import com.jannik_kuehn.common.storage.database.table.PlayerTable;
import com.jannik_kuehn.common.storage.database.table.ServerTable;
import com.jannik_kuehn.common.storage.database.table.TimeTable;
import com.jannik_kuehn.common.storage.database.table.WorldTable;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Database-backed unified storage implementation for player identity, sessions, and adjustments.
 */
@SuppressWarnings({"PMD.UnusedPrivateField", "PMD.TooManyMethods", "PMD.CouplingBetweenObjects"})
public class UnifiedDatabaseStorage implements UnifiedStorage {

    /**
     * Actor label used when a legacy method does not provide explicit actor metadata.
     */
    private static final String SYSTEM_ACTOR = "SYSTEM";

    /**
     * Database connection provider.
     */
    private final LoriTimeConnectionProvider provider;

    /**
     * SQL dialect used by this storage instance.
     */
    private final DatabaseDialect dialect;

    /**
     * Player table helper.
     */
    private final PlayerTable playerTable;

    /**
     * Server table helper.
     */
    private final ServerTable serverTable;

    /**
     * World table helper.
     */
    private final WorldTable worldTable;

    /**
     * Session timetable helper.
     */
    private final TimeTable timeTable;

    /**
     * Manual adjustment table helper.
     */
    private final ManualAdjustmentTable adjustmentTable;

    /**
     * Lock protecting storage access while the provider is closing.
     */
    private final ReadWriteLock poolLock;

    /**
     * Creates a database-backed unified storage instance.
     *
     * @param provider        the connection provider.
     * @param playerTable     the player table helper.
     * @param serverTable     the server table helper.
     * @param worldTable      the world table helper.
     * @param timeTable       the timetable helper.
     * @param adjustmentTable the manual adjustment table helper.
     * @param dialect         the database dialect.
     */
    public UnifiedDatabaseStorage(final LoriTimeConnectionProvider provider,
                                  final PlayerTable playerTable,
                                  final ServerTable serverTable,
                                  final WorldTable worldTable,
                                  final TimeTable timeTable,
                                  final ManualAdjustmentTable adjustmentTable,
                                  final DatabaseDialect dialect) {
        this.provider = provider;
        this.dialect = dialect;
        this.playerTable = playerTable;
        this.serverTable = serverTable;
        this.worldTable = worldTable;
        this.timeTable = timeTable;
        this.adjustmentTable = adjustmentTable;
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
                final Optional<Long> playerId = playerTable.findIdByUuid(connection, uniqueId);
                if (playerId.isEmpty()) {
                    return OptionalLong.empty();
                }
                final OptionalLong sessionSum = timeTable.sumForPlayer(connection, uniqueId);
                final OptionalLong adjustmentSum = adjustmentTable.sumForPlayer(connection, playerId.get());
                if (sessionSum.isEmpty() && adjustmentSum.isEmpty()) {
                    return OptionalLong.empty();
                }
                final long sessions = sessionSum.orElse(0L);
                final long adjustments = adjustmentSum.orElse(0L);
                final long total = sessions + adjustments;
                return OptionalLong.of(total);
            }
        } catch (final SQLException ex) {
            throw new StorageException(ex);
        } finally {
            poolLock.readLock().unlock();
        }
    }

    @Override
    public void addTime(final UUID uuid, final long additionalTime, final TimeEntryReason reason) throws StorageException {
        addTime(new ManualTimeAdjustment(uuid, additionalTime, reason, SYSTEM_ACTOR));
    }

    @Override
    public void addTime(final ManualTimeAdjustment adjustment) throws StorageException {
        Objects.requireNonNull(adjustment);
        Objects.requireNonNull(adjustment.playerUuid());
        Objects.requireNonNull(adjustment.reason());
        Objects.requireNonNull(adjustment.actorName());
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = provider.getConnection()) {
                final long playerId = playerTable.ensurePlayer(connection, adjustment.playerUuid(), Optional.empty());
                adjustmentTable.insert(connection, playerId, adjustment);
            }
        } catch (final SQLException ex) {
            throw new StorageException(ex);
        } finally {
            poolLock.readLock().unlock();
        }
    }

    @Override
    public void addTimes(final Map<UUID, Long> additionalTimes, final TimeEntryReason reason) throws StorageException {
        if (additionalTimes == null || additionalTimes.isEmpty()) {
            return;
        }
        addAdjustments(additionalTimes.entrySet().stream()
                .map(entry -> new ManualTimeAdjustment(entry.getKey(), entry.getValue(), reason, SYSTEM_ACTOR))
                .toList());
    }

    @Override
    public void addAdjustments(final List<ManualTimeAdjustment> adjustments) throws StorageException {
        if (adjustments == null || adjustments.isEmpty()) {
            return;
        }
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = provider.getConnection()) {
                for (final ManualTimeAdjustment adjustment : adjustments) {
                    final long playerId = playerTable.ensurePlayer(connection, adjustment.playerUuid(), Optional.empty());
                    adjustmentTable.insert(connection, playerId, adjustment);
                }
            }
        } catch (final SQLException ex) {
            throw new StorageException(ex);
        } finally {
            poolLock.readLock().unlock();
        }
    }

    @Override
    public long startSession(final PlayerSessionContext context, final TimeEntryReason reason) throws StorageException {
        Objects.requireNonNull(context);
        Objects.requireNonNull(reason);
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = provider.getConnection()) {
                final long worldId = worldTable.ensureWorld(connection, context.server(), context.world());
                final long playerId = playerTable.ensurePlayer(connection, context.uuid(), context.name());
                return timeTable.insertSession(connection, playerId, worldId,
                        Instant.ofEpochMilli(context.startedAtMs()),
                        Instant.ofEpochMilli(context.startedAtMs()),
                        reason);
            }
        } catch (final SQLException ex) {
            throw new StorageException(ex);
        } finally {
            poolLock.readLock().unlock();
        }
    }

    @Override
    public void updateSession(final long sessionId, final long stoppedAtMs, final TimeEntryReason reason) throws StorageException {
        Objects.requireNonNull(reason);
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = provider.getConnection()) {
                timeTable.updateSession(connection, sessionId, Instant.ofEpochMilli(stoppedAtMs), reason);
            }
        } catch (final SQLException ex) {
            throw new StorageException(ex);
        } finally {
            poolLock.readLock().unlock();
        }
    }

    @Override
    public void setPlayerName(final UUID uuid, final String name) throws StorageException {
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
    public void setPlayerNames(final Map<UUID, String> entries) throws StorageException {
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
                final Map<String, Long> totals = new HashMap<>(timeTable.getAllTotals(connection));
                adjustmentTable.getAllTotals(connection).forEach((uuid, value) -> totals.merge(uuid, value, Long::sum));
                return totals;
            }
        } catch (final SQLException ex) {
            throw new StorageException(ex);
        } finally {
            poolLock.readLock().unlock();
        }
    }

    @Override
    public void deletePlayer(final UUID uniqueId) throws StorageException, SQLException {
        deleteUser(uniqueId);
    }

    @Override
    public int deleteInactiveHistory(final long inactiveDays) throws StorageException {
        if (inactiveDays < 0) {
            return 0;
        }
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = provider.getConnection()) {
                final String cutoffSql = switch (dialect) {
                    case MYSQL, MARIADB -> "DATE_SUB(NOW(3), INTERVAL " + inactiveDays + " DAY)";
                    case SQLITE -> "DATETIME('now', '-" + inactiveDays + " days')";
                };
                final int timeRows = timeTable.deleteInactiveHistory(connection, cutoffSql);
                final int adjustmentRows = adjustmentTable.deleteInactiveHistory(connection, cutoffSql);
                return timeRows + adjustmentRows;
            }
        } catch (final SQLException ex) {
            throw new StorageException(ex);
        } finally {
            poolLock.readLock().unlock();
        }
    }

    @Override
    public void persistSession(final PlayerSessionChunk session) throws StorageException {
        Objects.requireNonNull(session);
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = provider.getConnection()) {
                final long worldId = worldTable.ensureWorld(connection, session.server(), session.world());
                final long playerId = playerTable.ensurePlayer(connection, session.uuid(), session.name());
                timeTable.insertSession(connection, playerId, worldId,
                        Instant.ofEpochMilli(session.startedAtMs()),
                        Instant.ofEpochMilli(session.stoppedAtMs()),
                        session.reason());
            }
        } catch (final SQLException ex) {
            throw new StorageException(ex);
        } finally {
            poolLock.readLock().unlock();
        }
    }

    private void deleteUser(final UUID uuid) throws StorageException {
        if (uuid == null) {
            return;
        }
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = provider.getConnection()) {
                final Optional<Long> playerId = playerTable.findIdByUuid(connection, uuid);
                if (playerId.isPresent()) {
                    timeTable.deleteForPlayer(connection, playerId.get());
                    adjustmentTable.deleteForPlayer(connection, playerId.get());
                }
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

    private void checkClosed() throws StorageException {
        if (provider.isClosed()) {
            throw new StorageException("closed");
        }
    }
}
