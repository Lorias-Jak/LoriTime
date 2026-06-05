package com.jannik_kuehn.common.storage.database;

import com.jannik_kuehn.common.api.storage.AdminStorageMaintenance;
import com.jannik_kuehn.common.api.storage.ManualTimeAdjustment;
import com.jannik_kuehn.common.api.storage.PlayerSessionChunk;
import com.jannik_kuehn.common.api.storage.PlayerSessionContext;
import com.jannik_kuehn.common.api.storage.RecentPlayerIdentity;
import com.jannik_kuehn.common.api.storage.StorageDeleteRequest;
import com.jannik_kuehn.common.api.storage.StorageMaintenanceConfirmation;
import com.jannik_kuehn.common.api.storage.StorageMaintenanceOperation;
import com.jannik_kuehn.common.api.storage.StorageMaintenancePreview;
import com.jannik_kuehn.common.api.storage.StorageMaintenanceResult;
import com.jannik_kuehn.common.api.storage.StorageMaintenanceScope;
import com.jannik_kuehn.common.api.storage.StorageTransferMapping;
import com.jannik_kuehn.common.api.storage.StorageTransferRequest;
import com.jannik_kuehn.common.api.storage.TimeEntryReason;
import com.jannik_kuehn.common.api.storage.TimeRange;
import com.jannik_kuehn.common.api.storage.TimeScope;
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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
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
@SuppressWarnings({
        "PMD.AvoidCatchingGenericException",
        "PMD.AvoidDuplicateLiterals",
        "PMD.CouplingBetweenObjects",
        "PMD.CyclomaticComplexity",
        "PMD.ExceptionAsFlowControl",
        "PMD.GodClass",
        "PMD.TooManyMethods"
})
public class UnifiedDatabaseStorage implements UnifiedStorage, AdminStorageMaintenance {

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
     * Recent player identity reader.
     */
    private final RecentPlayerIdentityReader recentPlayerIdentityReader;

    /**
     * Lock protecting storage access while the provider is closing.
     */
    private final ReadWriteLock poolLock;

    /**
     * Backing table names used by maintenance SQL.
     */
    private final String playerTableName;

    private final String serverTableName;

    private final String worldTableName;

    private final String timeTableName;

    private final String adjustmentTableName;

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
        this.recentPlayerIdentityReader = new RecentPlayerIdentityReader(provider, playerTable, dialect);
        this.poolLock = new ReentrantReadWriteLock();
        this.playerTableName = playerTable.toString();
        this.serverTableName = serverTable.toString();
        this.worldTableName = worldTable.toString();
        this.timeTableName = timeTable.toString();
        this.adjustmentTableName = adjustmentTable.toString();
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
        return getTime(uniqueId, TimeScope.GLOBAL);
    }

    @Override
    public OptionalLong getTime(final UUID uniqueId, final TimeScope scope) throws StorageException {
        Objects.requireNonNull(uniqueId);
        Objects.requireNonNull(scope);
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = provider.getConnection()) {
                final Optional<Long> playerId = playerTable.findIdByUuid(connection, uniqueId);
                if (playerId.isEmpty()) {
                    return OptionalLong.empty();
                }
                final OptionalLong sessionSum = sumSessions(connection, uniqueId, scope);
                final OptionalLong adjustmentSum = sumAdjustments(connection, playerId.get(), scope);
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
                final ScopeReferences references = resolveScopeReferences(connection, adjustment.scope(), true);
                adjustmentTable.insert(connection, playerId, references.serverId(), references.worldId(), adjustment);
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
                    final ScopeReferences references = resolveScopeReferences(connection, adjustment.scope(), true);
                    adjustmentTable.insert(connection, playerId, references.serverId(), references.worldId(), adjustment);
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
    public void updateSessionWorld(final long sessionId, final String server, final String world) throws StorageException {
        Objects.requireNonNull(server);
        Objects.requireNonNull(world);
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = provider.getConnection()) {
                final long worldId = worldTable.ensureWorld(connection, server, world);
                timeTable.updateSessionWorld(connection, sessionId, worldId);
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
    public OptionalLong getTime(final UUID uniqueId, final TimeScope scope, final TimeRange range) throws StorageException {
        Objects.requireNonNull(uniqueId);
        Objects.requireNonNull(scope);
        Objects.requireNonNull(range);
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = provider.getConnection()) {
                final Optional<Long> playerId = playerTable.findIdByUuid(connection, uniqueId);
                if (playerId.isEmpty()) {
                    return OptionalLong.empty();
                }
                final OptionalLong sessionSum = sumSessions(connection, uniqueId, scope, range);
                final OptionalLong adjustmentSum = sumAdjustments(connection, playerId.get(), scope, range);
                if (sessionSum.isEmpty() && adjustmentSum.isEmpty()) {
                    return OptionalLong.empty();
                }
                final long sessions = sessionSum.orElse(0L);
                final long adjustments = adjustmentSum.orElse(0L);
                return OptionalLong.of(sessions + adjustments);
            }
        } catch (final SQLException ex) {
            throw new StorageException(ex);
        } finally {
            poolLock.readLock().unlock();
        }
    }

    @Override
    public List<RecentPlayerIdentity> getRecentPlayerIdentities(final long recentDays) throws StorageException {
        poolLock.readLock().lock();
        try {
            checkClosed();
            return recentPlayerIdentityReader.read(recentDays);
        } finally {
            poolLock.readLock().unlock();
        }
    }

    @Override
    public Set<String> getKnownServerNames() throws StorageException {
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = provider.getConnection()) {
                return serverTable.getAllServers(connection);
            }
        } catch (final SQLException ex) {
            throw new StorageException(ex);
        } finally {
            poolLock.readLock().unlock();
        }
    }

    @Override
    public Set<String> getKnownWorldNames() throws StorageException {
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = provider.getConnection()) {
                return worldTable.getAllWorlds(connection);
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

    private OptionalLong sumSessions(final Connection connection, final UUID uniqueId, final TimeScope scope)
            throws SQLException {
        return switch (scope.type()) {
            case GLOBAL -> timeTable.sumForPlayer(connection, uniqueId);
            case SERVER -> timeTable.sumForPlayerAndServer(connection, uniqueId, scope.server());
            case WORLD -> timeTable.sumForPlayerAndWorld(connection, uniqueId, scope.server(), scope.world());
        };
    }

    private OptionalLong sumSessions(final Connection connection, final UUID uniqueId,
                                     final TimeScope scope, final TimeRange range) throws SQLException {
        return switch (scope.type()) {
            case GLOBAL -> timeTable.sumForPlayer(connection, uniqueId, range);
            case SERVER -> timeTable.sumForPlayerAndServer(connection, uniqueId, scope.server(), range);
            case WORLD -> timeTable.sumForPlayerAndWorld(connection, uniqueId, scope.server(), scope.world(), range);
        };
    }

    private OptionalLong sumAdjustments(final Connection connection, final long playerId, final TimeScope scope)
            throws SQLException {
        return switch (scope.type()) {
            case GLOBAL -> adjustmentTable.sums().sumForPlayer(connection, playerId);
            case SERVER -> {
                final Optional<Long> serverId = serverTable.findId(connection, scope.server());
                yield serverId.isEmpty() ? OptionalLong.empty()
                        : adjustmentTable.sums().sumForPlayerAndServer(connection, playerId, serverId.get());
            }
            case WORLD -> {
                final Optional<Long> worldId = worldTable.findId(connection, scope.server(), scope.world());
                yield worldId.isEmpty() ? OptionalLong.empty()
                        : adjustmentTable.sums().sumForPlayerAndWorld(connection, playerId, worldId.get());
            }
        };
    }

    private OptionalLong sumAdjustments(final Connection connection, final long playerId,
                                        final TimeScope scope, final TimeRange range) throws SQLException {
        return switch (scope.type()) {
            case GLOBAL -> adjustmentTable.sums().sumForPlayer(connection, playerId, range);
            case SERVER -> {
                final Optional<Long> serverId = serverTable.findId(connection, scope.server());
                yield serverId.isEmpty() ? OptionalLong.empty()
                        : adjustmentTable.sums().sumForPlayerAndServer(connection, playerId, serverId.get(), range);
            }
            case WORLD -> {
                final Optional<Long> worldId = worldTable.findId(connection, scope.server(), scope.world());
                yield worldId.isEmpty() ? OptionalLong.empty()
                        : adjustmentTable.sums().sumForPlayerAndWorld(connection, playerId, worldId.get(), range);
            }
        };
    }

    private ScopeReferences resolveScopeReferences(final Connection connection, final TimeScope scope,
                                                   final boolean createMissing) throws SQLException {
        return switch (scope.type()) {
            case GLOBAL -> new ScopeReferences(OptionalLong.empty(), OptionalLong.empty());
            case SERVER -> new ScopeReferences(resolveServerId(connection, scope, createMissing), OptionalLong.empty());
            case WORLD -> new ScopeReferences(OptionalLong.empty(), resolveWorldId(connection, scope, createMissing));
        };
    }

    private OptionalLong resolveServerId(final Connection connection, final TimeScope scope, final boolean createMissing)
            throws SQLException {
        if (createMissing) {
            return OptionalLong.of(serverTable.ensureServer(connection, scope.server()));
        }
        return serverTable.findId(connection, scope.server()).map(OptionalLong::of).orElseGet(OptionalLong::empty);
    }

    private OptionalLong resolveWorldId(final Connection connection, final TimeScope scope, final boolean createMissing)
            throws SQLException {
        if (createMissing) {
            return OptionalLong.of(worldTable.ensureWorld(connection, scope.server(), scope.world()));
        }
        return worldTable.findId(connection, scope.server(), scope.world()).map(OptionalLong::of).orElseGet(OptionalLong::empty);
    }

    private record ScopeReferences(OptionalLong serverId, OptionalLong worldId) {
    }

    @Override
    public StorageMaintenancePreview previewStorageTransferTo(final AdminStorageMaintenance target)
            throws StorageException {
        if (!(target instanceof UnifiedDatabaseStorage targetStorage)) {
            throw new StorageException("Target storage does not support database storage-type transfer");
        }
        poolLock.readLock().lock();
        targetStorage.poolLock.readLock().lock();
        try {
            checkClosed();
            targetStorage.checkClosed();
            try (Connection sourceConnection = provider.getConnection();
                 Connection targetConnection = targetStorage.provider.getConnection()) {
                return buildStorageTypeTransferPreview(sourceConnection, targetStorage, targetConnection);
            }
        } catch (final SQLException ex) {
            throw new StorageException(ex);
        } finally {
            targetStorage.poolLock.readLock().unlock();
            poolLock.readLock().unlock();
        }
    }

    @Override
    public StorageMaintenanceResult applyStorageTransferTo(final AdminStorageMaintenance target,
                                                          final StorageMaintenanceConfirmation confirmation)
            throws StorageException {
        Objects.requireNonNull(confirmation, "confirmation");
        if (!(target instanceof UnifiedDatabaseStorage targetStorage)) {
            throw new StorageException("Target storage does not support database storage-type transfer");
        }
        poolLock.readLock().lock();
        targetStorage.poolLock.readLock().lock();
        try {
            checkClosed();
            targetStorage.checkClosed();
            try (Connection sourceConnection = provider.getConnection();
                 Connection targetConnection = targetStorage.provider.getConnection()) {
                final boolean autoCommit = targetConnection.getAutoCommit();
                targetConnection.setAutoCommit(false);
                try {
                    final StorageMaintenancePreview preview =
                            buildStorageTypeTransferPreview(sourceConnection, targetStorage, targetConnection);
                    validateConfirmation(preview, confirmation);
                    targetStorage.rejectNonEmptyStorageTypeTarget(targetConnection);
                    targetStorage.importStorageSnapshot(targetConnection, exportStorageSnapshot(sourceConnection));
                    targetConnection.commit();
                    return new StorageMaintenanceResult(preview.operation(), preview.affectedSessions(),
                            preview.affectedAdjustments(), preview.affectedPlayers());
                } catch (final SQLException | StorageException | RuntimeException ex) {
                    targetConnection.rollback();
                    throw ex;
                } finally {
                    targetConnection.setAutoCommit(autoCommit);
                }
            }
        } catch (final SQLException ex) {
            throw new StorageException(ex);
        } finally {
            targetStorage.poolLock.readLock().unlock();
            poolLock.readLock().unlock();
        }
    }

    @Override
    public StorageMaintenancePreview previewTransfer(final StorageTransferRequest request) throws StorageException {
        Objects.requireNonNull(request, "request");
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = provider.getConnection()) {
                return buildTransferPreview(connection, request);
            }
        } catch (final SQLException ex) {
            throw new StorageException(ex);
        } finally {
            poolLock.readLock().unlock();
        }
    }

    @Override
    public StorageMaintenanceResult applyTransfer(final StorageTransferRequest request,
                                                  final StorageMaintenanceConfirmation confirmation) throws StorageException {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(confirmation, "confirmation");
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = provider.getConnection()) {
                final boolean autoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    final StorageMaintenancePreview preview = buildTransferPreview(connection, request);
                    validateConfirmation(preview, confirmation);
                    switch (request.operation()) {
                        case STORAGE_TYPE_TRANSFER -> rejectNonEmptyStorageTypeTarget(connection);
                        case SERVER_TRANSFER -> applyServerTransfers(connection, request.mappings());
                        case WORLD_TRANSFER -> applyWorldTransfers(connection, request.mappings());
                        default -> throw new StorageException("Unsupported transfer operation: " + request.operation());
                    }
                    connection.commit();
                    return new StorageMaintenanceResult(preview.operation(), preview.affectedSessions(),
                            preview.affectedAdjustments(), preview.affectedPlayers());
                } catch (final SQLException | StorageException | RuntimeException ex) {
                    connection.rollback();
                    throw ex;
                } finally {
                    connection.setAutoCommit(autoCommit);
                }
            }
        } catch (final SQLException ex) {
            throw new StorageException(ex);
        } finally {
            poolLock.readLock().unlock();
        }
    }

    @Override
    public StorageMaintenancePreview previewDelete(final StorageDeleteRequest request) throws StorageException {
        Objects.requireNonNull(request, "request");
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = provider.getConnection()) {
                return buildDeletePreview(connection, request);
            }
        } catch (final SQLException ex) {
            throw new StorageException(ex);
        } finally {
            poolLock.readLock().unlock();
        }
    }

    @Override
    public StorageMaintenanceResult applyDelete(final StorageDeleteRequest request,
                                                final StorageMaintenanceConfirmation confirmation) throws StorageException {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(confirmation, "confirmation");
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = provider.getConnection()) {
                final boolean autoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    final StorageMaintenancePreview preview = buildDeletePreview(connection, request);
                    validateConfirmation(preview, confirmation);
                    if (request.scope().type() == StorageMaintenanceScope.Type.SERVER) {
                        deleteServerScope(connection, request.scope().server());
                    } else {
                        deleteWorldScope(connection, request.scope().server(), request.scope().world());
                    }
                    connection.commit();
                    return new StorageMaintenanceResult(preview.operation(), preview.affectedSessions(),
                            preview.affectedAdjustments(), preview.affectedPlayers());
                } catch (final SQLException | StorageException | RuntimeException ex) {
                    connection.rollback();
                    throw ex;
                } finally {
                    connection.setAutoCommit(autoCommit);
                }
            }
        } catch (final SQLException ex) {
            throw new StorageException(ex);
        } finally {
            poolLock.readLock().unlock();
        }
    }

    private StorageMaintenancePreview buildTransferPreview(final Connection connection,
                                                           final StorageTransferRequest request) throws SQLException {
        final Counts counts;
        final boolean targetDataExists;
        final List<String> collisions = new ArrayList<>();
        switch (request.operation()) {
            case STORAGE_TYPE_TRANSFER -> {
                counts = countStorage(connection);
                targetDataExists = storageHasAnyData(connection);
            }
            case SERVER_TRANSFER -> {
                Counts total = Counts.empty();
                boolean targetData = false;
                for (final StorageTransferMapping mapping : request.mappings()) {
                    validateMappingType(mapping, StorageMaintenanceScope.Type.SERVER);
                    total = total.plus(countServer(connection, mapping.source().server()));
                    targetData |= countServer(connection, mapping.target().server()).hasData();
                    collisions.addAll(serverWorldCollisions(connection, mapping.source().server(), mapping.target().server()));
                }
                counts = total;
                targetDataExists = targetData;
            }
            case WORLD_TRANSFER -> {
                Counts total = Counts.empty();
                boolean targetData = false;
                for (final StorageTransferMapping mapping : request.mappings()) {
                    validateMappingType(mapping, StorageMaintenanceScope.Type.WORLD);
                    total = total.plus(countWorld(connection, mapping.source().server(), mapping.source().world()));
                    targetData |= countWorld(connection, mapping.target().server(), mapping.target().world()).hasData();
                    if (targetWorldExists(connection, mapping.target().server(), mapping.target().world())) {
                        collisions.add(mapping.target().server() + "/" + mapping.target().world());
                    }
                }
                counts = total;
                targetDataExists = targetData;
            }
            default -> throw new SQLException("Unsupported transfer operation: " + request.operation());
        }
        final boolean confirmationRequired = targetDataExists || !collisions.isEmpty() || counts.hasData();
        return new StorageMaintenancePreview(request.operation(), request.mappings(), null, counts.sessions(),
                counts.adjustments(), counts.players(), targetDataExists, collisions, confirmationRequired,
                fingerprint(request.operation(), request.mappings().toString(), null, counts, targetDataExists, collisions));
    }

    private StorageMaintenancePreview buildStorageTypeTransferPreview(final Connection sourceConnection,
                                                                      final UnifiedDatabaseStorage targetStorage,
                                                                      final Connection targetConnection)
            throws SQLException {
        final Counts counts = countStorage(sourceConnection);
        final boolean targetDataExists = targetStorage.storageHasTransferBlockingData(targetConnection);
        final List<String> collisions = List.of();
        return new StorageMaintenancePreview(StorageMaintenanceOperation.STORAGE_TYPE_TRANSFER,
                StorageTransferRequest.storageTypeTransfer().mappings(), null, counts.sessions(),
                counts.adjustments(), counts.players(), targetDataExists, collisions,
                targetDataExists || counts.hasData(),
                fingerprint(StorageMaintenanceOperation.STORAGE_TYPE_TRANSFER, "storageTypeTransfer",
                        null, counts, targetDataExists, collisions));
    }

    private StorageSnapshot exportStorageSnapshot(final Connection connection) throws SQLException {
        final List<PlayerRow> players = new ArrayList<>();
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT `uuid`, `name`, `last_seen` FROM `" + playerTableName + "` ORDER BY `id`");
             ResultSet result = select.executeQuery()) {
            while (result.next()) {
                players.add(new PlayerRow(result.getBytes("uuid"), result.getString("name"),
                        result.getObject("last_seen")));
            }
        }

        final List<ServerRow> servers = new ArrayList<>();
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT `server` FROM `" + serverTableName + "` ORDER BY `id`");
             ResultSet result = select.executeQuery()) {
            while (result.next()) {
                servers.add(new ServerRow(result.getString("server")));
            }
        }

        final List<WorldSnapshotRow> worlds = new ArrayList<>();
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT s.`server`, w.`world` FROM `" + worldTableName + "` w "
                        + "JOIN `" + serverTableName + "` s ON s.`id` = w.`server_id` ORDER BY w.`id`");
             ResultSet result = select.executeQuery()) {
            while (result.next()) {
                worlds.add(new WorldSnapshotRow(result.getString("server"), result.getString("world")));
            }
        }

        final List<SessionRow> sessions = new ArrayList<>();
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT p.`uuid`, s.`server`, w.`world`, t.`join_time`, t.`leave_time`, t.`reason` "
                        + "FROM `" + timeTableName + "` t "
                        + "JOIN `" + playerTableName + "` p ON p.`id` = t.`player_id` "
                        + "JOIN `" + worldTableName + "` w ON w.`id` = t.`world_id` "
                        + "JOIN `" + serverTableName + "` s ON s.`id` = w.`server_id` ORDER BY t.`id`");
             ResultSet result = select.executeQuery()) {
            while (result.next()) {
                sessions.add(new SessionRow(result.getBytes("uuid"), result.getString("server"),
                        result.getString("world"), result.getObject("join_time"),
                        result.getObject("leave_time"), result.getString("reason")));
            }
        }

        final List<AdjustmentRow> adjustments = new ArrayList<>();
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT p.`uuid`, a.`scope_type`, ss.`server` AS scope_server, ws.`server` AS world_server, "
                        + "w.`world`, a.`amount_seconds`, a.`reason`, a.`actor_uuid`, a.`actor_name`, a.`created_at` "
                        + "FROM `" + adjustmentTableName + "` a "
                        + "JOIN `" + playerTableName + "` p ON p.`id` = a.`player_id` "
                        + "LEFT JOIN `" + serverTableName + "` ss ON ss.`id` = a.`server_id` "
                        + "LEFT JOIN `" + worldTableName + "` w ON w.`id` = a.`world_id` "
                        + "LEFT JOIN `" + serverTableName + "` ws ON ws.`id` = w.`server_id` ORDER BY a.`id`");
             ResultSet result = select.executeQuery()) {
            while (result.next()) {
                adjustments.add(new AdjustmentRow(result.getBytes("uuid"), result.getString("scope_type"),
                        result.getString("scope_server"), result.getString("world_server"),
                        result.getString("world"), result.getLong("amount_seconds"),
                        result.getString("reason"), result.getBytes("actor_uuid"),
                        result.getString("actor_name"), result.getObject("created_at")));
            }
        }
        return new StorageSnapshot(players, servers, worlds, sessions, adjustments);
    }

    private void importStorageSnapshot(final Connection connection, final StorageSnapshot snapshot)
            throws SQLException {
        insertSnapshotPlayers(connection, snapshot.players());
        for (final ServerRow server : snapshot.servers()) {
            serverTable.ensureServer(connection, server.server());
        }
        for (final WorldSnapshotRow world : snapshot.worlds()) {
            worldTable.ensureWorld(connection, world.server(), world.world());
        }
        insertSnapshotSessions(connection, snapshot.sessions());
        insertSnapshotAdjustments(connection, snapshot.adjustments());
    }

    private void insertSnapshotPlayers(final Connection connection, final List<PlayerRow> players) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO `" + playerTableName + "` (`uuid`, `name`, `last_seen`) VALUES (?, ?, ?)")) {
            for (final PlayerRow player : players) {
                insert.setBytes(1, player.uuid());
                insert.setString(2, player.name());
                insert.setObject(3, player.lastSeen());
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    private void insertSnapshotSessions(final Connection connection, final List<SessionRow> sessions)
            throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO `" + timeTableName + "` (`player_id`, `world_id`, `join_time`, `leave_time`, `reason`) "
                        + "VALUES ((SELECT `id` FROM `" + playerTableName + "` WHERE `uuid` = ?), "
                        + "(SELECT w.`id` FROM `" + worldTableName + "` w "
                        + "JOIN `" + serverTableName + "` s ON s.`id` = w.`server_id` "
                        + "WHERE s.`server` = ? AND w.`world` = ?), ?, ?, ?)")) {
            for (final SessionRow session : sessions) {
                insert.setBytes(1, session.playerUuid());
                insert.setString(2, session.server());
                insert.setString(3, session.world());
                insert.setObject(4, session.joinTime());
                insert.setObject(5, session.leaveTime());
                insert.setString(6, session.reason());
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    private void insertSnapshotAdjustments(final Connection connection, final List<AdjustmentRow> adjustments)
            throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO `" + adjustmentTableName + "` (`player_id`, `scope_type`, `server_id`, `world_id`, "
                        + "`amount_seconds`, `reason`, `actor_uuid`, `actor_name`, `created_at`) VALUES "
                        + "((SELECT `id` FROM `" + playerTableName + "` WHERE `uuid` = ?), ?, "
                        + "(SELECT `id` FROM `" + serverTableName + "` WHERE `server` = ?), "
                        + "(SELECT w.`id` FROM `" + worldTableName + "` w "
                        + "JOIN `" + serverTableName + "` s ON s.`id` = w.`server_id` "
                        + "WHERE s.`server` = ? AND w.`world` = ?), ?, ?, ?, ?, ?)")) {
            for (final AdjustmentRow adjustment : adjustments) {
                insert.setBytes(1, adjustment.playerUuid());
                insert.setString(2, adjustment.scopeType());
                insert.setString(3, adjustment.scopeServer());
                insert.setString(4, adjustment.worldServer());
                insert.setString(5, adjustment.world());
                insert.setLong(6, adjustment.amountSeconds());
                insert.setString(7, adjustment.reason());
                insert.setBytes(8, adjustment.actorUuid());
                insert.setString(9, adjustment.actorName());
                insert.setObject(10, adjustment.createdAt());
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    private StorageMaintenancePreview buildDeletePreview(final Connection connection,
                                                         final StorageDeleteRequest request) throws SQLException {
        final StorageMaintenanceScope scope = request.scope();
        final Counts counts = switch (scope.type()) {
            case SERVER -> countServer(connection, scope.server());
            case WORLD -> countWorld(connection, scope.server(), scope.world());
            case STORAGE -> throw new SQLException("delete scope must be server or world");
        };
        final StorageMaintenanceOperation operation = scope.type() == StorageMaintenanceScope.Type.SERVER
                ? StorageMaintenanceOperation.SERVER_DELETE : StorageMaintenanceOperation.WORLD_DELETE;
        return new StorageMaintenancePreview(operation, List.of(), scope, counts.sessions(), counts.adjustments(),
                counts.players(), false, List.of(), counts.hasData(),
                fingerprint(operation, List.of().toString(), scope.toString(), counts, false, List.of()));
    }

    private void validateConfirmation(final StorageMaintenancePreview preview,
                                      final StorageMaintenanceConfirmation confirmation) throws StorageException {
        if (!preview.fingerprint().equals(confirmation.fingerprint())) {
            throw new StorageException("Storage maintenance confirmation does not match the current preview");
        }
    }

    private String fingerprint(final StorageMaintenanceOperation operation,
                               final String mappings,
                               final String scope,
                               final Counts counts,
                               final boolean targetDataExists,
                               final List<String> collisions) {
        return Integer.toHexString(Objects.hash(operation, mappings, scope, counts.sessions(), counts.adjustments(),
                counts.players(), targetDataExists, collisions));
    }

    private void validateMappingType(final StorageTransferMapping mapping,
                                     final StorageMaintenanceScope.Type expectedType) throws SQLException {
        if (mapping.source().type() != expectedType || mapping.target().type() != expectedType) {
            throw new SQLException("Expected " + expectedType + " transfer mapping");
        }
    }

    private Counts countStorage(final Connection connection) throws SQLException {
        return new Counts(countRows(connection, timeTableName), countRows(connection, adjustmentTableName),
                countDistinctPlayers(connection, ""));
    }

    private Counts countServer(final Connection connection, final String server) throws SQLException {
        final long sessions = singleLong(connection,
                "SELECT COUNT(*) FROM `" + timeTableName + "` t "
                        + "JOIN `" + worldTableName + "` w ON w.`id` = t.`world_id` "
                        + "JOIN `" + serverTableName + "` s ON s.`id` = w.`server_id` "
                        + "WHERE s.`server` = ?", server);
        final long adjustments = singleLong(connection,
                "SELECT COUNT(*) FROM `" + adjustmentTableName + "` a "
                        + "LEFT JOIN `" + worldTableName + "` w ON w.`id` = a.`world_id` "
                        + "LEFT JOIN `" + serverTableName + "` ws ON ws.`id` = w.`server_id` "
                        + "LEFT JOIN `" + serverTableName + "` ss ON ss.`id` = a.`server_id` "
                        + "WHERE (a.`scope_type` = 'SERVER' AND ss.`server` = ?) "
                        + "OR (a.`scope_type` = 'WORLD' AND ws.`server` = ?)", server, server);
        final long players = countDistinctPlayers(connection,
                "WHERE `id` IN ("
                        + "SELECT t.`player_id` FROM `" + timeTableName + "` t "
                        + "JOIN `" + worldTableName + "` w ON w.`id` = t.`world_id` "
                        + "JOIN `" + serverTableName + "` s ON s.`id` = w.`server_id` WHERE s.`server` = ? "
                        + "UNION SELECT a.`player_id` FROM `" + adjustmentTableName + "` a "
                        + "LEFT JOIN `" + worldTableName + "` w ON w.`id` = a.`world_id` "
                        + "LEFT JOIN `" + serverTableName + "` ws ON ws.`id` = w.`server_id` "
                        + "LEFT JOIN `" + serverTableName + "` ss ON ss.`id` = a.`server_id` "
                        + "WHERE (a.`scope_type` = 'SERVER' AND ss.`server` = ?) "
                        + "OR (a.`scope_type` = 'WORLD' AND ws.`server` = ?))", server, server, server);
        return new Counts(sessions, adjustments, players);
    }

    private Counts countWorld(final Connection connection, final String server, final String world) throws SQLException {
        final long sessions = singleLong(connection,
                "SELECT COUNT(*) FROM `" + timeTableName + "` t "
                        + "JOIN `" + worldTableName + "` w ON w.`id` = t.`world_id` "
                        + "JOIN `" + serverTableName + "` s ON s.`id` = w.`server_id` "
                        + "WHERE s.`server` = ? AND w.`world` = ?", server, world);
        final long adjustments = singleLong(connection,
                "SELECT COUNT(*) FROM `" + adjustmentTableName + "` a "
                        + "JOIN `" + worldTableName + "` w ON w.`id` = a.`world_id` "
                        + "JOIN `" + serverTableName + "` s ON s.`id` = w.`server_id` "
                        + "WHERE a.`scope_type` = 'WORLD' AND s.`server` = ? AND w.`world` = ?", server, world);
        final long players = countDistinctPlayers(connection,
                "WHERE `id` IN ("
                        + "SELECT t.`player_id` FROM `" + timeTableName + "` t "
                        + "JOIN `" + worldTableName + "` w ON w.`id` = t.`world_id` "
                        + "JOIN `" + serverTableName + "` s ON s.`id` = w.`server_id` "
                        + "WHERE s.`server` = ? AND w.`world` = ? "
                        + "UNION SELECT a.`player_id` FROM `" + adjustmentTableName + "` a "
                        + "JOIN `" + worldTableName + "` w ON w.`id` = a.`world_id` "
                        + "JOIN `" + serverTableName + "` s ON s.`id` = w.`server_id` "
                        + "WHERE a.`scope_type` = 'WORLD' AND s.`server` = ? AND w.`world` = ?)",
                server, world, server, world);
        return new Counts(sessions, adjustments, players);
    }

    private long countRows(final Connection connection, final String tableName) throws SQLException {
        return singleLong(connection, "SELECT COUNT(*) FROM `" + tableName + "`");
    }

    private long countDistinctPlayers(final Connection connection, final String condition, final Object... params)
            throws SQLException {
        return singleLong(connection, "SELECT COUNT(DISTINCT `id`) FROM `" + playerTableName + "` " + condition, params);
    }

    private boolean storageHasAnyData(final Connection connection) throws SQLException {
        return playerTable.hasAnyData(connection)
                || countRows(connection, serverTableName) > 0
                || countRows(connection, worldTableName) > 0
                || countRows(connection, timeTableName) > 0
                || countRows(connection, adjustmentTableName) > 0;
    }

    private boolean storageHasTransferBlockingData(final Connection connection) throws SQLException {
        return playerTable.hasAnyData(connection)
                || countRows(connection, timeTableName) > 0
                || countRows(connection, adjustmentTableName) > 0
                || singleLong(connection,
                        "SELECT COUNT(*) FROM `" + serverTableName + "` WHERE `server` <> 'default'") > 0
                || singleLong(connection,
                        "SELECT COUNT(*) FROM `" + worldTableName + "` w "
                                + "JOIN `" + serverTableName + "` s ON s.`id` = w.`server_id` "
                                + "WHERE s.`server` <> 'default' OR w.`world` <> 'global'") > 0;
    }

    private boolean targetWorldExists(final Connection connection, final String server, final String world)
            throws SQLException {
        return worldTable.findId(connection, server, world).isPresent();
    }

    private List<String> serverWorldCollisions(final Connection connection, final String sourceServer,
                                               final String targetServer) throws SQLException {
        final List<String> collisions = new ArrayList<>();
        final String sql = "SELECT sw.`world` FROM `" + worldTableName + "` sw "
                + "JOIN `" + serverTableName + "` ss ON ss.`id` = sw.`server_id` "
                + "JOIN `" + serverTableName + "` ts ON ts.`server` = ? "
                + "JOIN `" + worldTableName + "` tw ON tw.`server_id` = ts.`id` AND tw.`world` = sw.`world` "
                + "WHERE ss.`server` = ?";
        try (PreparedStatement select = connection.prepareStatement(sql)) {
            select.setString(1, targetServer);
            select.setString(2, sourceServer);
            try (ResultSet result = select.executeQuery()) {
                while (result.next()) {
                    collisions.add(targetServer + "/" + result.getString("world"));
                }
            }
        }
        return collisions;
    }

    private void applyServerTransfers(final Connection connection, final List<StorageTransferMapping> mappings)
            throws SQLException {
        for (final StorageTransferMapping mapping : mappings) {
            validateMappingType(mapping, StorageMaintenanceScope.Type.SERVER);
            final String sourceServer = mapping.source().server();
            final String targetServer = mapping.target().server();
            if (sourceServer.equals(targetServer)) {
                continue;
            }
            final Optional<Long> sourceServerId = serverTable.findId(connection, sourceServer);
            if (sourceServerId.isEmpty()) {
                continue;
            }
            final long targetServerId = serverTable.ensureServer(connection, targetServer);
            for (final WorldRow sourceWorld : sourceWorlds(connection, sourceServerId.get())) {
                final long targetWorldId = worldTable.ensureWorld(connection, targetServer, sourceWorld.world());
                updateWorldReferences(connection, sourceWorld.worldId(), targetWorldId);
                deleteWorldIfUnreferenced(connection, sourceWorld.worldId());
            }
            updateServerAdjustments(connection, sourceServerId.get(), targetServerId);
            deleteServerIfUnreferenced(connection, sourceServerId.get());
        }
    }

    private void applyWorldTransfers(final Connection connection, final List<StorageTransferMapping> mappings)
            throws SQLException {
        for (final StorageTransferMapping mapping : mappings) {
            validateMappingType(mapping, StorageMaintenanceScope.Type.WORLD);
            final Optional<Long> sourceWorldId = worldTable.findId(connection, mapping.source().server(), mapping.source().world());
            if (sourceWorldId.isEmpty()) {
                continue;
            }
            final long targetWorldId = worldTable.ensureWorld(connection, mapping.target().server(), mapping.target().world());
            if (sourceWorldId.get().equals(targetWorldId)) {
                continue;
            }
            updateWorldReferences(connection, sourceWorldId.get(), targetWorldId);
            deleteWorldIfUnreferenced(connection, sourceWorldId.get());
        }
    }

    private void rejectNonEmptyStorageTypeTarget(final Connection connection) throws SQLException, StorageException {
        if (storageHasTransferBlockingData(connection)) {
            throw new StorageException("Target storage must be empty before storage-type transfer");
        }
    }

    private List<WorldRow> sourceWorlds(final Connection connection, final long serverId) throws SQLException {
        final List<WorldRow> worlds = new ArrayList<>();
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT `id`, `world` FROM `" + worldTableName + "` WHERE `server_id` = ?")) {
            select.setLong(1, serverId);
            try (ResultSet result = select.executeQuery()) {
                while (result.next()) {
                    worlds.add(new WorldRow(result.getLong("id"), result.getString("world")));
                }
            }
        }
        return worlds;
    }

    private void updateWorldReferences(final Connection connection, final long sourceWorldId, final long targetWorldId)
            throws SQLException {
        try (PreparedStatement updateTime = connection.prepareStatement(
                "UPDATE `" + timeTableName + "` SET `world_id` = ? WHERE `world_id` = ?");
             PreparedStatement updateAdjustments = connection.prepareStatement(
                     "UPDATE `" + adjustmentTableName + "` SET `world_id` = ? WHERE `world_id` = ?")) {
            updateTime.setLong(1, targetWorldId);
            updateTime.setLong(2, sourceWorldId);
            updateTime.executeUpdate();
            updateAdjustments.setLong(1, targetWorldId);
            updateAdjustments.setLong(2, sourceWorldId);
            updateAdjustments.executeUpdate();
        }
    }

    private void updateServerAdjustments(final Connection connection, final long sourceServerId, final long targetServerId)
            throws SQLException {
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE `" + adjustmentTableName + "` SET `server_id` = ? "
                        + "WHERE `scope_type` = 'SERVER' AND `server_id` = ?")) {
            update.setLong(1, targetServerId);
            update.setLong(2, sourceServerId);
            update.executeUpdate();
        }
    }

    private void deleteServerScope(final Connection connection, final String server) throws SQLException {
        final Optional<Long> serverId = serverTable.findId(connection, server);
        if (serverId.isEmpty()) {
            return;
        }
        try (PreparedStatement deleteTime = connection.prepareStatement(
                "DELETE FROM `" + timeTableName + "` WHERE `world_id` IN "
                        + "(SELECT `id` FROM `" + worldTableName + "` WHERE `server_id` = ?)");
             PreparedStatement deleteWorldAdjustments = connection.prepareStatement(
                     "DELETE FROM `" + adjustmentTableName + "` WHERE `world_id` IN "
                             + "(SELECT `id` FROM `" + worldTableName + "` WHERE `server_id` = ?)");
             PreparedStatement deleteServerAdjustments = connection.prepareStatement(
                     "DELETE FROM `" + adjustmentTableName + "` WHERE `scope_type` = 'SERVER' AND `server_id` = ?")) {
            deleteTime.setLong(1, serverId.get());
            deleteTime.executeUpdate();
            deleteWorldAdjustments.setLong(1, serverId.get());
            deleteWorldAdjustments.executeUpdate();
            deleteServerAdjustments.setLong(1, serverId.get());
            deleteServerAdjustments.executeUpdate();
        }
    }

    private void deleteWorldScope(final Connection connection, final String server, final String world) throws SQLException {
        final Optional<Long> worldId = worldTable.findId(connection, server, world);
        if (worldId.isEmpty()) {
            return;
        }
        try (PreparedStatement deleteTime = connection.prepareStatement(
                "DELETE FROM `" + timeTableName + "` WHERE `world_id` = ?");
             PreparedStatement deleteAdjustments = connection.prepareStatement(
                     "DELETE FROM `" + adjustmentTableName + "` WHERE `scope_type` = 'WORLD' AND `world_id` = ?")) {
            deleteTime.setLong(1, worldId.get());
            deleteTime.executeUpdate();
            deleteAdjustments.setLong(1, worldId.get());
            deleteAdjustments.executeUpdate();
        }
    }

    private void deleteWorldIfUnreferenced(final Connection connection, final long worldId) throws SQLException {
        if (singleLong(connection, "SELECT COUNT(*) FROM `" + timeTableName + "` WHERE `world_id` = ?", worldId) > 0
                || singleLong(connection, "SELECT COUNT(*) FROM `" + adjustmentTableName + "` WHERE `world_id` = ?", worldId) > 0) {
            return;
        }
        try (PreparedStatement delete = connection.prepareStatement("DELETE FROM `" + worldTableName + "` WHERE `id` = ?")) {
            delete.setLong(1, worldId);
            delete.executeUpdate();
        }
    }

    private void deleteServerIfUnreferenced(final Connection connection, final long serverId) throws SQLException {
        if (singleLong(connection, "SELECT COUNT(*) FROM `" + worldTableName + "` WHERE `server_id` = ?", serverId) > 0
                || singleLong(connection, "SELECT COUNT(*) FROM `" + adjustmentTableName + "` WHERE `server_id` = ?", serverId) > 0) {
            return;
        }
        try (PreparedStatement delete = connection.prepareStatement("DELETE FROM `" + serverTableName + "` WHERE `id` = ?")) {
            delete.setLong(1, serverId);
            delete.executeUpdate();
        }
    }

    private long singleLong(final Connection connection, final String sql, final Object... params) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                if (params[i] instanceof Long value) {
                    select.setLong(i + 1, value);
                } else {
                    select.setString(i + 1, params[i].toString());
                }
            }
            try (ResultSet result = select.executeQuery()) {
                return result.next() ? result.getLong(1) : 0L;
            }
        }
    }

    private record Counts(long sessions, long adjustments, long players) {

        private static Counts empty() {
            return new Counts(0L, 0L, 0L);
        }

        private Counts plus(final Counts other) {
            return new Counts(sessions + other.sessions, adjustments + other.adjustments, players + other.players);
        }

        private boolean hasData() {
            return sessions > 0 || adjustments > 0;
        }
    }

    private record WorldRow(long worldId, String world) {
    }

    private record StorageSnapshot(List<PlayerRow> players,
                                   List<ServerRow> servers,
                                   List<WorldSnapshotRow> worlds,
                                   List<SessionRow> sessions,
                                   List<AdjustmentRow> adjustments) {
    }

    private record PlayerRow(byte[] uuid, String name, Object lastSeen) {
    }

    private record ServerRow(String server) {
    }

    private record WorldSnapshotRow(String server, String world) {
    }

    private record SessionRow(byte[] playerUuid,
                              String server,
                              String world,
                              Object joinTime,
                              Object leaveTime,
                              String reason) {
    }

    private record AdjustmentRow(byte[] playerUuid,
                                 String scopeType,
                                 String scopeServer,
                                 String worldServer,
                                 String world,
                                 long amountSeconds,
                                 String reason,
                                 byte[] actorUuid,
                                 String actorName,
                                 Object createdAt) {
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
