package com.jannik_kuehn.common.storage.database;

import com.jannik_kuehn.common.api.storage.TimeRange;
import com.jannik_kuehn.common.api.storage.TimeScope;
import com.jannik_kuehn.common.exception.StorageException;
import com.jannik_kuehn.common.storage.contract.AdminStorageMaintenance;
import com.jannik_kuehn.common.storage.contract.StatisticsStorage;
import com.jannik_kuehn.common.storage.contract.UnifiedStorage;
import com.jannik_kuehn.common.storage.database.provider.LoriTimeConnectionProvider;
import com.jannik_kuehn.common.storage.database.table.ManualAdjustmentTable;
import com.jannik_kuehn.common.storage.database.table.PlayerTable;
import com.jannik_kuehn.common.storage.database.table.ServerTable;
import com.jannik_kuehn.common.storage.database.table.TimeTable;
import com.jannik_kuehn.common.storage.database.table.WorldTable;
import com.jannik_kuehn.common.storage.model.AfkPeriod;
import com.jannik_kuehn.common.storage.model.AfkPeriodEndReason;
import com.jannik_kuehn.common.storage.model.ManualTimeAdjustment;
import com.jannik_kuehn.common.storage.model.PlayerSessionChunk;
import com.jannik_kuehn.common.storage.model.PlayerSessionContext;
import com.jannik_kuehn.common.storage.model.PlayerStorageTransferRequest;
import com.jannik_kuehn.common.storage.model.RecentPlayerIdentity;
import com.jannik_kuehn.common.storage.model.SessionHistoryRow;
import com.jannik_kuehn.common.storage.model.StatisticsRequest;
import com.jannik_kuehn.common.storage.model.StatisticsSnapshot;
import com.jannik_kuehn.common.storage.model.StorageDeleteRequest;
import com.jannik_kuehn.common.storage.model.StorageMaintenanceConfirmation;
import com.jannik_kuehn.common.storage.model.StorageMaintenanceOperation;
import com.jannik_kuehn.common.storage.model.StorageMaintenancePreview;
import com.jannik_kuehn.common.storage.model.StorageMaintenanceResult;
import com.jannik_kuehn.common.storage.model.StorageMaintenanceScope;
import com.jannik_kuehn.common.storage.model.StorageTransferMapping;
import com.jannik_kuehn.common.storage.model.StorageTransferRequest;
import com.jannik_kuehn.common.storage.model.TimeEntryReason;
import com.jannik_kuehn.common.storage.statistics.StatisticsAggregator;
import com.jannik_kuehn.common.utils.UuidUtil;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
        "PMD.InefficientStringBuffering",
        "PMD.TooManyMethods"
})
public class UnifiedDatabaseStorage implements UnifiedStorage, AdminStorageMaintenance, StatisticsStorage {

    /**
     * Actor label used when a legacy method does not provide explicit actor metadata.
     */
    private static final String SYSTEM_ACTOR = "SYSTEM";

    /**
     * Rows copied per batch during full storage-type transfer.
     */
    private static final int STORAGE_TRANSFER_BATCH_SIZE = 1_000;

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

    /**
     * server table names used by maintenance SQL.
     */
    private final String serverTableName;

    /**
     * world table names used by maintenance SQL.
     */
    private final String worldTableName;

    /**
     * time table names used by maintenance SQL.
     */
    private final String timeTableName;

    /**
     * adjustment table names used by maintenance SQL.
     */
    private final String adjustmentTableName;

    /** AFK-period table derived from the normalized table prefix. */
    private final String afkPeriodTableName;

    /**
     * Creates a database-backed unified storage instance.
     *
     * @param provider        the connection provider.
     * @param tablePrefix     normalized database table prefix.
     * @param playerTable     the player table helper.
     * @param serverTable     the server table helper.
     * @param worldTable      the world table helper.
     * @param timeTable       the timetable helper.
     * @param adjustmentTable the manual adjustment table helper.
     * @param dialect         the database dialect.
     */
    public UnifiedDatabaseStorage(final LoriTimeConnectionProvider provider,
                                  final String tablePrefix,
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
        this.afkPeriodTableName = tablePrefix + "_afk_period";
    }

    @Override
    public void openAfkPeriod(final UUID playerId, final String playerName, final String server, final String world,
                              final Instant startedAt) throws StorageException {
        Objects.requireNonNull(playerId);
        Objects.requireNonNull(server);
        Objects.requireNonNull(world);
        Objects.requireNonNull(startedAt);
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = provider.getConnection()) {
                final long internalPlayerId = playerTable.ensurePlayer(connection, playerId,
                        Optional.ofNullable(playerName));
                final long worldId = worldTable.ensureWorld(connection, server, world);
                try (PreparedStatement select = connection.prepareStatement("SELECT 1 FROM `" + afkPeriodTableName
                        + "` WHERE `player_id` = ? AND `ended_at` IS NULL LIMIT 1")) {
                    select.setLong(1, internalPlayerId);
                    try (ResultSet result = select.executeQuery()) {
                        if (result.next()) {
                            return;
                        }
                    }
                }
                try (PreparedStatement insert = connection.prepareStatement("INSERT INTO `" + afkPeriodTableName
                        + "` (`player_id`, `world_id`, `started_at`) VALUES (?, ?, ?)")) {
                    insert.setLong(1, internalPlayerId);
                    insert.setLong(2, worldId);
                    setStoredInstant(insert, 3, startedAt);
                    insert.executeUpdate();
                }
            }
        } catch (final SQLException ex) {
            throw new StorageException(ex);
        } finally {
            poolLock.readLock().unlock();
        }
    }

    @Override
    public void closeAfkPeriod(final UUID playerId, final Instant endedAt, final AfkPeriodEndReason reason)
            throws StorageException {
        Objects.requireNonNull(playerId);
        Objects.requireNonNull(endedAt);
        Objects.requireNonNull(reason);
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = provider.getConnection();
                 PreparedStatement update = connection.prepareStatement("UPDATE `" + afkPeriodTableName
                         + "` SET `ended_at` = ?, `end_reason` = ? WHERE `player_id` = (SELECT `id` FROM `"
                         + playerTableName + "` WHERE `uuid` = ?) AND `ended_at` IS NULL")) {
                setStoredInstant(update, 1, endedAt);
                update.setString(2, reason.name());
                update.setBytes(3, UuidUtil.toBytes(playerId));
                update.executeUpdate();
            }
        } catch (final SQLException ex) {
            throw new StorageException(ex);
        } finally {
            poolLock.readLock().unlock();
        }
    }

    @Override
    public int recoverOpenAfkPeriods(final Instant endedAt) throws StorageException {
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = provider.getConnection();
                 PreparedStatement update = connection.prepareStatement("UPDATE `" + afkPeriodTableName
                         + "` SET `ended_at` = ?, `end_reason` = 'SHUTDOWN' WHERE `ended_at` IS NULL")) {
                setStoredInstant(update, 1, endedAt);
                return update.executeUpdate();
            }
        } catch (final SQLException ex) {
            throw new StorageException(ex);
        } finally {
            poolLock.readLock().unlock();
        }
    }

    @Override
    public List<AfkPeriod> getAfkPeriods(final TimeRange range, final TimeScope scope) throws StorageException {
        final List<AfkPeriod> periods = new ArrayList<>();
        final String sql = "SELECT a.`id`, p.`uuid`, s.`server`, w.`world`, a.`started_at`, a.`ended_at`, "
                + "a.`end_reason` FROM `" + afkPeriodTableName + "` a JOIN `" + playerTableName
                + "` p ON p.`id` = a.`player_id` JOIN `" + worldTableName + "` w ON w.`id` = a.`world_id` "
                + "JOIN `" + serverTableName + "` s ON s.`id` = w.`server_id` WHERE "
                + "a.`started_at` < ? AND (a.`ended_at` IS NULL OR a.`ended_at` > ?)"
                + scopeCondition(scope, "s", "w")
                + " ORDER BY a.`started_at`";
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = provider.getConnection(); PreparedStatement select = connection.prepareStatement(sql)) {
                int index = 1;
                setStoredInstant(select, index++, range.endExclusive());
                setStoredInstant(select, index++, range.startInclusive());
                bindScope(select, index, scope);
                try (ResultSet result = select.executeQuery()) {
                    while (result.next()) {
                        final Object ended = result.getObject("ended_at");
                        final String reason = result.getString("end_reason");
                        periods.add(new AfkPeriod(result.getLong("id"), UuidUtil.fromBytes(result.getBytes("uuid")),
                                result.getString("server"), result.getString("world"), readInstant(result, "started_at"),
                                ended == null ? Optional.empty() : Optional.of(readInstant(result, "ended_at")),
                                reason == null ? Optional.empty() : Optional.of(AfkPeriodEndReason.valueOf(reason))));
                    }
                }
            }
            return periods;
        } catch (final SQLException ex) {
            throw new StorageException(ex);
        } finally {
            poolLock.readLock().unlock();
        }
    }

    @Override
    public StatisticsSnapshot getStatistics(final StatisticsRequest request) throws StorageException {
        final List<SessionHistoryRow> rows = new ArrayList<>();
        final String sql = "SELECT p.`uuid`, p.`name`, s.`server`, w.`world`, t.`join_time`, t.`leave_time`, "
                + "t.`reason`, (SELECT " + minimumInstant("t0.`join_time`") + " FROM `" + timeTableName
                + "` t0 WHERE t0.`player_id` = t.`player_id`) AS `first_join` FROM `" + timeTableName
                + "` t JOIN `" + playerTableName + "` p ON p.`id` = t.`player_id` JOIN `" + worldTableName
                + "` w ON w.`id` = t.`world_id` JOIN `" + serverTableName
                + "` s ON s.`id` = w.`server_id` WHERE " + instantLess("t.`join_time`") + " AND "
                + instantGreaterOrEqual("t.`leave_time`") + scopeCondition(request.scope(), "s", "w")
                + " ORDER BY p.`uuid`, t.`join_time`";
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = provider.getConnection(); PreparedStatement select = connection.prepareStatement(sql)) {
                int index = 1;
                setRangeParam(select, index++, request.range().endExclusive());
                setRangeParam(select, index++, request.range().startInclusive().minus(StatisticsAggregator.CONTEXT_SWITCH_TOLERANCE));
                bindScope(select, index, request.scope());
                try (ResultSet result = select.executeQuery()) {
                    while (result.next()) {
                        rows.add(new SessionHistoryRow(UuidUtil.fromBytes(result.getBytes("uuid")), result.getString("name"),
                                result.getString("server"), result.getString("world"), readInstant(result, "join_time"),
                                readInstant(result, "leave_time"), parseReason(result.getString("reason")),
                                readInstant(result, "first_join")));
                    }
                }
            }
            return StatisticsAggregator.aggregate(request, rows, getAfkPeriods(request.range(), request.scope()));
        } catch (final SQLException ex) {
            throw new StorageException(ex);
        } finally {
            poolLock.readLock().unlock();
        }
    }

    private TimeEntryReason parseReason(final String reason) {
        try {
            return TimeEntryReason.valueOf(reason);
        } catch (final IllegalArgumentException ex) {
            return TimeEntryReason.UNSPECIFIED;
        }
    }

    private String scopeCondition(final TimeScope scope, final String serverAlias, final String worldAlias) {
        return switch (scope.type()) {
            case GLOBAL -> "";
            case SERVER -> " AND " + serverAlias + ".`server` = ?";
            case WORLD -> " AND " + serverAlias + ".`server` = ? AND " + worldAlias + ".`world` = ?";
        };
    }

    private void bindScope(final PreparedStatement statement, final int start, final TimeScope scope) throws SQLException {
        if (scope.type() != TimeScope.Type.GLOBAL) {
            statement.setString(start, scope.server());
        }
        if (scope.type() == TimeScope.Type.WORLD) {
            statement.setString(start + 1, scope.world());
        }
    }

    private String instantLess(final String column) {
        return dialect == DatabaseDialect.SQLITE ? sqliteEpochMillis(column) + " < ?" : column + " < ?";
    }

    private String instantGreaterOrEqual(final String column) {
        return dialect == DatabaseDialect.SQLITE ? sqliteEpochMillis(column) + " >= ?" : column + " >= ?";
    }

    private String minimumInstant(final String column) {
        return dialect == DatabaseDialect.SQLITE ? "MIN(" + sqliteEpochMillis(column) + ")" : "MIN(" + column + ")";
    }

    private void setStoredInstant(final PreparedStatement statement, final int index, final Instant instant)
            throws SQLException {
        if (dialect == DatabaseDialect.SQLITE) {
            statement.setString(index, Timestamp.from(instant).toString());
        } else {
            statement.setTimestamp(index, Timestamp.from(instant));
        }
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
    public Map<String, Set<String>> getKnownWorldNamesByServer() throws StorageException {
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = provider.getConnection()) {
                return worldTable.getAllWorldsByServer(connection, timeTableName);
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

    @Override
    public StorageMaintenancePreview previewStorageTransferTo(final AdminStorageMaintenance target)
            throws StorageException {
        if (!(target instanceof final UnifiedDatabaseStorage targetStorage)) {
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
        if (!(target instanceof final UnifiedDatabaseStorage targetStorage)) {
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
                    targetStorage.copySnapshotHistory(sourceConnection, this, targetConnection);
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
    public StorageMaintenancePreview previewPlayerTransfer(final PlayerStorageTransferRequest request)
            throws StorageException {
        Objects.requireNonNull(request, "request");
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = provider.getConnection()) {
                return buildPlayerTransferPreview(connection, request);
            }
        } catch (final SQLException ex) {
            throw new StorageException(ex);
        } finally {
            poolLock.readLock().unlock();
        }
    }

    @Override
    public StorageMaintenanceResult applyPlayerTransfer(final PlayerStorageTransferRequest request,
                                                        final StorageMaintenanceConfirmation confirmation)
            throws StorageException {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(confirmation, "confirmation");
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = provider.getConnection()) {
                final boolean autoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    final StorageMaintenancePreview preview = buildPlayerTransferPreview(connection, request);
                    validateConfirmation(preview, confirmation);
                    switch (request.operation()) {
                        case SERVER_TRANSFER -> applyPlayerServerTransfer(connection, request);
                        case WORLD_TRANSFER -> applyPlayerWorldTransfer(connection, request);
                        default -> throw new StorageException("Unsupported player transfer operation: " + request.operation());
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
                    applyDeleteRequest(connection, request);
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

    private StorageMaintenancePreview buildPlayerTransferPreview(final Connection connection,
                                                                 final PlayerStorageTransferRequest request)
            throws SQLException {
        final Optional<Long> playerId = playerTable.findIdByUuid(connection, request.playerUuid());
        final Counts counts;
        final boolean targetDataExists;
        final List<String> collisions = new ArrayList<>();
        if (playerId.isEmpty()) {
            counts = Counts.empty();
            targetDataExists = false;
        } else if (request.operation() == StorageMaintenanceOperation.SERVER_TRANSFER) {
            validateMappingType(request.mapping(), StorageMaintenanceScope.Type.SERVER);
            counts = countPlayerServer(connection, playerId.get(), request.mapping().source().server(), request.timeRange());
            targetDataExists = countPlayerServer(connection, playerId.get(), request.mapping().target().server(),
                    request.timeRange()).hasData();
            collisions.addAll(serverWorldCollisions(connection, request.mapping().source().server(),
                    request.mapping().target().server()));
        } else if (request.operation() == StorageMaintenanceOperation.WORLD_TRANSFER) {
            validateMappingType(request.mapping(), StorageMaintenanceScope.Type.WORLD);
            counts = countPlayerWorld(connection, playerId.get(), request.mapping().source().server(),
                    request.mapping().source().world(), request.timeRange());
            targetDataExists = countPlayerWorld(connection, playerId.get(), request.mapping().target().server(),
                    request.mapping().target().world(), request.timeRange()).hasData();
            if (targetWorldExists(connection, request.mapping().target().server(), request.mapping().target().world())) {
                collisions.add(request.mapping().target().server() + "/" + request.mapping().target().world());
            }
        } else {
            throw new SQLException("Unsupported player transfer operation: " + request.operation());
        }
        final boolean confirmationRequired = targetDataExists || !collisions.isEmpty() || counts.hasData();
        return new StorageMaintenancePreview(request.operation(), List.of(request.mapping()), null,
                counts.sessions(), counts.adjustments(), counts.players(), targetDataExists, collisions,
                confirmationRequired, Optional.of(request.playerUuid()), request.playerName(), request.timeRange(),
                request.timeRangeInput(), fingerprint(request.operation(), request.mapping().toString(),
                request.playerUuid() + ":" + request.timeRange().map(TimeRange::toString).orElse("all"),
                counts, targetDataExists, collisions));
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

        return new StorageSnapshot(players, servers, worlds);
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

    private void insertSnapshotAfkPeriods(final Connection connection, final List<AfkPeriodRow> periods)
            throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO `" + afkPeriodTableName + "` (`player_id`, `world_id`, `started_at`, `ended_at`, "
                        + "`end_reason`) VALUES ((SELECT `id` FROM `" + playerTableName + "` WHERE `uuid` = ?), "
                        + "(SELECT w.`id` FROM `" + worldTableName + "` w "
                        + "JOIN `" + serverTableName + "` s ON s.`id` = w.`server_id` "
                        + "WHERE s.`server` = ? AND w.`world` = ?), ?, ?, ?)")) {
            for (final AfkPeriodRow period : periods) {
                insert.setBytes(1, period.playerUuid());
                insert.setString(2, period.server());
                insert.setString(3, period.world());
                insert.setObject(4, period.startedAt());
                insert.setObject(5, period.endedAt());
                insert.setString(6, period.endReason());
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    private void copySnapshotHistory(final Connection sourceConnection,
                                     final UnifiedDatabaseStorage sourceStorage,
                                     final Connection targetConnection) throws SQLException {
        long offset = 0L;
        List<SessionRow> sessions;
        do {
            sessions = sourceStorage.readSnapshotSessions(sourceConnection, offset);
            insertSnapshotSessions(targetConnection, sessions);
            offset += sessions.size();
        } while (sessions.size() == STORAGE_TRANSFER_BATCH_SIZE);

        offset = 0L;
        List<AdjustmentRow> adjustments;
        do {
            adjustments = sourceStorage.readSnapshotAdjustments(sourceConnection, offset);
            insertSnapshotAdjustments(targetConnection, adjustments);
            offset += adjustments.size();
        } while (adjustments.size() == STORAGE_TRANSFER_BATCH_SIZE);

        offset = 0L;
        List<AfkPeriodRow> periods;
        do {
            periods = sourceStorage.readSnapshotAfkPeriods(sourceConnection, offset);
            insertSnapshotAfkPeriods(targetConnection, periods);
            offset += periods.size();
        } while (periods.size() == STORAGE_TRANSFER_BATCH_SIZE);
    }

    private List<SessionRow> readSnapshotSessions(final Connection connection, final long offset) throws SQLException {
        final List<SessionRow> sessions = new ArrayList<>();
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT p.`uuid`, s.`server`, w.`world`, t.`join_time`, t.`leave_time`, t.`reason` "
                        + "FROM `" + timeTableName + "` t "
                        + "JOIN `" + playerTableName + "` p ON p.`id` = t.`player_id` "
                        + "JOIN `" + worldTableName + "` w ON w.`id` = t.`world_id` "
                        + "JOIN `" + serverTableName + "` s ON s.`id` = w.`server_id` "
                        + "ORDER BY t.`id` LIMIT ? OFFSET ?")) {
            select.setInt(1, STORAGE_TRANSFER_BATCH_SIZE);
            select.setLong(2, offset);
            try (ResultSet result = select.executeQuery()) {
                while (result.next()) {
                    sessions.add(new SessionRow(result.getBytes("uuid"), result.getString("server"),
                            result.getString("world"), result.getObject("join_time"),
                            result.getObject("leave_time"), result.getString("reason")));
                }
            }
        }
        return sessions;
    }

    private List<AdjustmentRow> readSnapshotAdjustments(final Connection connection, final long offset) throws SQLException {
        final List<AdjustmentRow> adjustments = new ArrayList<>();
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT p.`uuid`, a.`scope_type`, ss.`server` AS scope_server, ws.`server` AS world_server, "
                        + "w.`world`, a.`amount_seconds`, a.`reason`, a.`actor_uuid`, a.`actor_name`, a.`created_at` "
                        + "FROM `" + adjustmentTableName + "` a "
                        + "JOIN `" + playerTableName + "` p ON p.`id` = a.`player_id` "
                        + "LEFT JOIN `" + serverTableName + "` ss ON ss.`id` = a.`server_id` "
                        + "LEFT JOIN `" + worldTableName + "` w ON w.`id` = a.`world_id` "
                        + "LEFT JOIN `" + serverTableName + "` ws ON ws.`id` = w.`server_id` "
                        + "ORDER BY a.`id` LIMIT ? OFFSET ?")) {
            select.setInt(1, STORAGE_TRANSFER_BATCH_SIZE);
            select.setLong(2, offset);
            try (ResultSet result = select.executeQuery()) {
                while (result.next()) {
                    adjustments.add(new AdjustmentRow(result.getBytes("uuid"), result.getString("scope_type"),
                            result.getString("scope_server"), result.getString("world_server"),
                            result.getString("world"), result.getLong("amount_seconds"),
                            result.getString("reason"), result.getBytes("actor_uuid"),
                            result.getString("actor_name"), result.getObject("created_at")));
                }
            }
        }
        return adjustments;
    }

    private List<AfkPeriodRow> readSnapshotAfkPeriods(final Connection connection, final long offset)
            throws SQLException {
        final List<AfkPeriodRow> periods = new ArrayList<>();
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT p.`uuid`, s.`server`, w.`world`, a.`started_at`, a.`ended_at`, a.`end_reason` "
                        + "FROM `" + afkPeriodTableName + "` a "
                        + "JOIN `" + playerTableName + "` p ON p.`id` = a.`player_id` "
                        + "JOIN `" + worldTableName + "` w ON w.`id` = a.`world_id` "
                        + "JOIN `" + serverTableName + "` s ON s.`id` = w.`server_id` "
                        + "ORDER BY a.`id` LIMIT ? OFFSET ?")) {
            select.setInt(1, STORAGE_TRANSFER_BATCH_SIZE);
            select.setLong(2, offset);
            try (ResultSet result = select.executeQuery()) {
                while (result.next()) {
                    periods.add(new AfkPeriodRow(result.getBytes("uuid"), result.getString("server"),
                            result.getString("world"), result.getObject("started_at"), result.getObject("ended_at"),
                            result.getString("end_reason")));
                }
            }
        }
        return periods;
    }

    private StorageMaintenancePreview buildDeletePreview(final Connection connection,
                                                         final StorageDeleteRequest request) throws SQLException {
        final StorageMaintenanceScope scope = request.scope();
        final Counts counts = countDeleteRequest(connection, request);
        final StorageMaintenanceOperation operation = scope.type() == StorageMaintenanceScope.Type.SERVER
                ? StorageMaintenanceOperation.SERVER_DELETE : StorageMaintenanceOperation.WORLD_DELETE;
        return new StorageMaintenancePreview(operation, List.of(), scope, counts.sessions(), counts.adjustments(),
                counts.players(), false, List.of(), counts.hasData(), request.playerUuid(), request.playerName(),
                request.timeRange(), request.timeRangeInput(),
                fingerprint(operation, List.of().toString(), deleteFingerprintScope(request), counts, false, List.of()));
    }

    private Counts countDeleteRequest(final Connection connection, final StorageDeleteRequest request)
            throws SQLException {
        final StorageMaintenanceScope scope = request.scope();
        if (request.playerUuid().isPresent()) {
            final Optional<Long> playerId = playerTable.findIdByUuid(connection, request.playerUuid().get());
            if (playerId.isEmpty()) {
                return Counts.empty();
            }
            return scope.type() == StorageMaintenanceScope.Type.SERVER
                    ? countPlayerServer(connection, playerId.get(), scope.server(), request.timeRange())
                    : countPlayerWorld(connection, playerId.get(), scope.server(), scope.world(), request.timeRange());
        }
        if (request.timeRange().isPresent()) {
            final DeleteSelection selection = selectDeleteRows(connection, request);
            return new Counts(selection.sessionIds().size(), selection.adjustmentIds().size(), selection.playerIds().size());
        }
        return switch (scope.type()) {
            case SERVER -> countServer(connection, scope.server());
            case WORLD -> countWorld(connection, scope.server(), scope.world());
            case STORAGE -> throw new SQLException("delete scope must be server or world");
        };
    }

    private String deleteFingerprintScope(final StorageDeleteRequest request) {
        return request.scope() + ":"
                + request.playerUuid().map(UUID::toString).orElse("all") + ":"
                + request.timeRange().map(TimeRange::toString).orElse("all");
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

    private Counts countPlayerServer(final Connection connection,
                                     final long playerId,
                                     final String server,
                                     final Optional<TimeRange> range) throws SQLException {
        if (range.isPresent()) {
            final long sessions = countPlayerServerSessionsInRange(connection, playerId, server, range.get());
            final long adjustments = countPlayerServerAdjustmentsInRange(connection, playerId, server, range.get());
            return new Counts(sessions, adjustments, sessions > 0 || adjustments > 0 ? 1L : 0L);
        }
        final long sessions = singleLong(connection,
                "SELECT COUNT(*) FROM `" + timeTableName + "` t "
                        + "JOIN `" + worldTableName + "` w ON w.`id` = t.`world_id` "
                        + "JOIN `" + serverTableName + "` s ON s.`id` = w.`server_id` "
                        + "WHERE t.`player_id` = ? AND s.`server` = ?", playerId, server);
        final long adjustments = singleLong(connection,
                "SELECT COUNT(*) FROM `" + adjustmentTableName + "` a "
                        + "LEFT JOIN `" + worldTableName + "` w ON w.`id` = a.`world_id` "
                        + "LEFT JOIN `" + serverTableName + "` ws ON ws.`id` = w.`server_id` "
                        + "LEFT JOIN `" + serverTableName + "` ss ON ss.`id` = a.`server_id` "
                        + "WHERE a.`player_id` = ? AND ((a.`scope_type` = 'SERVER' AND ss.`server` = ?) "
                        + "OR (a.`scope_type` = 'WORLD' AND ws.`server` = ?))", playerId, server, server);
        return new Counts(sessions, adjustments, sessions > 0 || adjustments > 0 ? 1L : 0L);
    }

    private Counts countPlayerWorld(final Connection connection,
                                    final long playerId,
                                    final String server,
                                    final String world,
                                    final Optional<TimeRange> range) throws SQLException {
        if (range.isPresent()) {
            final long sessions = countPlayerWorldSessionsInRange(connection, playerId, server, world, range.get());
            final long adjustments = countPlayerWorldAdjustmentsInRange(connection, playerId, server, world, range.get());
            return new Counts(sessions, adjustments, sessions > 0 || adjustments > 0 ? 1L : 0L);
        }
        final long sessions = singleLong(connection,
                "SELECT COUNT(*) FROM `" + timeTableName + "` t "
                        + "JOIN `" + worldTableName + "` w ON w.`id` = t.`world_id` "
                        + "JOIN `" + serverTableName + "` s ON s.`id` = w.`server_id` "
                        + "WHERE t.`player_id` = ? AND s.`server` = ? AND w.`world` = ?",
                playerId, server, world);
        final long adjustments = singleLong(connection,
                "SELECT COUNT(*) FROM `" + adjustmentTableName + "` a "
                        + "JOIN `" + worldTableName + "` w ON w.`id` = a.`world_id` "
                        + "JOIN `" + serverTableName + "` s ON s.`id` = w.`server_id` "
                        + "WHERE a.`player_id` = ? AND a.`scope_type` = 'WORLD' "
                        + "AND s.`server` = ? AND w.`world` = ?", playerId, server, world);
        return new Counts(sessions, adjustments, sessions > 0 || adjustments > 0 ? 1L : 0L);
    }

    private long countPlayerServerSessionsInRange(final Connection connection,
                                                  final long playerId,
                                                  final String server,
                                                  final TimeRange range) throws SQLException {
        return matchingSessionIds(connection, playerId,
                "JOIN `" + worldTableName + "` w ON w.`id` = t.`world_id` "
                        + "JOIN `" + serverTableName + "` s ON s.`id` = w.`server_id` "
                        + "WHERE t.`player_id` = ? AND s.`server` = ?",
                range, server).size();
    }

    private long countPlayerWorldSessionsInRange(final Connection connection,
                                                 final long playerId,
                                                 final String server,
                                                 final String world,
                                                 final TimeRange range) throws SQLException {
        return matchingSessionIds(connection, playerId,
                "JOIN `" + worldTableName + "` w ON w.`id` = t.`world_id` "
                        + "JOIN `" + serverTableName + "` s ON s.`id` = w.`server_id` "
                        + "WHERE t.`player_id` = ? AND s.`server` = ? AND w.`world` = ?",
                range, server, world).size();
    }

    private long countPlayerServerAdjustmentsInRange(final Connection connection,
                                                     final long playerId,
                                                     final String server,
                                                     final TimeRange range) throws SQLException {
        return matchingAdjustmentIds(connection, playerId,
                "LEFT JOIN `" + worldTableName + "` w ON w.`id` = a.`world_id` "
                        + "LEFT JOIN `" + serverTableName + "` ws ON ws.`id` = w.`server_id` "
                        + "LEFT JOIN `" + serverTableName + "` ss ON ss.`id` = a.`server_id` "
                        + "WHERE a.`player_id` = ? AND ((a.`scope_type` = 'SERVER' AND ss.`server` = ?) "
                        + "OR (a.`scope_type` = 'WORLD' AND ws.`server` = ?))",
                range, server, server).size();
    }

    private long countPlayerWorldAdjustmentsInRange(final Connection connection,
                                                    final long playerId,
                                                    final String server,
                                                    final String world,
                                                    final TimeRange range) throws SQLException {
        return matchingAdjustmentIds(connection, playerId,
                "JOIN `" + worldTableName + "` w ON w.`id` = a.`world_id` "
                        + "JOIN `" + serverTableName + "` s ON s.`id` = w.`server_id` "
                        + "WHERE a.`player_id` = ? AND a.`scope_type` = 'WORLD' "
                        + "AND s.`server` = ? AND w.`world` = ?",
                range, server, world).size();
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
                || countRows(connection, adjustmentTableName) > 0
                || countRows(connection, afkPeriodTableName) > 0;
    }

    private boolean storageHasTransferBlockingData(final Connection connection) throws SQLException {
        return playerTable.hasAnyData(connection)
                || countRows(connection, timeTableName) > 0
                || countRows(connection, adjustmentTableName) > 0
                || countRows(connection, afkPeriodTableName) > 0
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

    private void applyPlayerServerTransfer(final Connection connection, final PlayerStorageTransferRequest request)
            throws SQLException {
        validateMappingType(request.mapping(), StorageMaintenanceScope.Type.SERVER);
        final Optional<Long> playerId = playerTable.findIdByUuid(connection, request.playerUuid());
        if (playerId.isEmpty()) {
            return;
        }
        final String sourceServer = request.mapping().source().server();
        final String targetServer = request.mapping().target().server();
        if (sourceServer.equals(targetServer)) {
            return;
        }
        final Optional<Long> sourceServerId = serverTable.findId(connection, sourceServer);
        if (sourceServerId.isEmpty()) {
            return;
        }
        final long targetServerId = serverTable.ensureServer(connection, targetServer);
        for (final WorldRow sourceWorld : sourceWorlds(connection, sourceServerId.get())) {
            final long targetWorldId = worldTable.ensureWorld(connection, targetServer, sourceWorld.world());
            updatePlayerWorldReferences(connection, playerId.get(), sourceWorld.worldId(), targetWorldId,
                    request.timeRange());
            deleteWorldIfUnreferenced(connection, sourceWorld.worldId());
        }
        updatePlayerServerAdjustments(connection, playerId.get(), sourceServerId.get(), targetServerId,
                request.timeRange());
        deleteServerIfUnreferenced(connection, sourceServerId.get());
    }

    private void applyPlayerWorldTransfer(final Connection connection, final PlayerStorageTransferRequest request)
            throws SQLException {
        validateMappingType(request.mapping(), StorageMaintenanceScope.Type.WORLD);
        final Optional<Long> playerId = playerTable.findIdByUuid(connection, request.playerUuid());
        if (playerId.isEmpty()) {
            return;
        }
        final Optional<Long> sourceWorldId = worldTable.findId(connection, request.mapping().source().server(),
                request.mapping().source().world());
        if (sourceWorldId.isEmpty()) {
            return;
        }
        final long targetWorldId = worldTable.ensureWorld(connection, request.mapping().target().server(),
                request.mapping().target().world());
        if (sourceWorldId.get().equals(targetWorldId)) {
            return;
        }
        updatePlayerWorldReferences(connection, playerId.get(), sourceWorldId.get(), targetWorldId, request.timeRange());
        deleteWorldIfUnreferenced(connection, sourceWorldId.get());
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
                     "UPDATE `" + adjustmentTableName + "` SET `world_id` = ? WHERE `world_id` = ?");
             PreparedStatement updateAfkPeriods = connection.prepareStatement(
                     "UPDATE `" + afkPeriodTableName + "` SET `world_id` = ? WHERE `world_id` = ?")) {
            updateTime.setLong(1, targetWorldId);
            updateTime.setLong(2, sourceWorldId);
            updateTime.executeUpdate();
            updateAdjustments.setLong(1, targetWorldId);
            updateAdjustments.setLong(2, sourceWorldId);
            updateAdjustments.executeUpdate();
            updateAfkPeriods.setLong(1, targetWorldId);
            updateAfkPeriods.setLong(2, sourceWorldId);
            updateAfkPeriods.executeUpdate();
        }
    }

    private void updatePlayerWorldReferences(final Connection connection,
                                             final long playerId,
                                             final long sourceWorldId,
                                             final long targetWorldId,
                                             final Optional<TimeRange> range)
            throws SQLException {
        if (range.isPresent()) {
            for (final long sessionId : matchingSessionIds(connection, playerId,
                    "WHERE t.`player_id` = ? AND t.`world_id` = ?", range.get(), sourceWorldId)) {
                updateRowWorldReference(connection, timeTableName, sessionId, targetWorldId);
            }
            for (final long adjustmentId : matchingAdjustmentIds(connection, playerId,
                    "WHERE a.`player_id` = ? AND a.`scope_type` = 'WORLD' AND a.`world_id` = ?",
                    range.get(), sourceWorldId)) {
                updateRowWorldReference(connection, adjustmentTableName, adjustmentId, targetWorldId);
            }
            for (final long periodId : matchingAfkPeriodIds(connection, playerId, sourceWorldId, range.get())) {
                updateRowWorldReference(connection, afkPeriodTableName, periodId, targetWorldId);
            }
            return;
        }
        try (PreparedStatement updateTime = connection.prepareStatement(
                "UPDATE `" + timeTableName + "` SET `world_id` = ? "
                        + "WHERE `player_id` = ? AND `world_id` = ?" + sessionRangeCondition(range));
             PreparedStatement updateAdjustments = connection.prepareStatement(
                     "UPDATE `" + adjustmentTableName + "` SET `world_id` = ? "
                             + "WHERE `player_id` = ? AND `scope_type` = 'WORLD' AND `world_id` = ?"
                             + adjustmentRangeCondition(range));
             PreparedStatement updateAfkPeriods = connection.prepareStatement(
                     "UPDATE `" + afkPeriodTableName + "` SET `world_id` = ? "
                             + "WHERE `player_id` = ? AND `world_id` = ?")) {
            setUpdateWorldReferenceParams(updateTime, targetWorldId, playerId, sourceWorldId, range);
            updateTime.executeUpdate();
            setUpdateWorldReferenceParams(updateAdjustments, targetWorldId, playerId, sourceWorldId, range);
            updateAdjustments.executeUpdate();
            updateAfkPeriods.setLong(1, targetWorldId);
            updateAfkPeriods.setLong(2, playerId);
            updateAfkPeriods.setLong(3, sourceWorldId);
            updateAfkPeriods.executeUpdate();
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

    private void updatePlayerServerAdjustments(final Connection connection,
                                               final long playerId,
                                               final long sourceServerId,
                                               final long targetServerId,
                                               final Optional<TimeRange> range)
            throws SQLException {
        if (range.isPresent()) {
            for (final long adjustmentId : matchingAdjustmentIds(connection, playerId,
                    "WHERE a.`player_id` = ? AND a.`scope_type` = 'SERVER' AND a.`server_id` = ?",
                    range.get(), sourceServerId)) {
                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE `" + adjustmentTableName + "` SET `server_id` = ? WHERE `id` = ?")) {
                    update.setLong(1, targetServerId);
                    update.setLong(2, adjustmentId);
                    update.executeUpdate();
                }
            }
            return;
        }
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE `" + adjustmentTableName + "` SET `server_id` = ? "
                        + "WHERE `player_id` = ? AND `scope_type` = 'SERVER' AND `server_id` = ?"
                        + adjustmentRangeCondition(range))) {
            update.setLong(1, targetServerId);
            update.setLong(2, playerId);
            update.setLong(3, sourceServerId);
            setRangeParams(update, 4, range);
            update.executeUpdate();
        }
    }

    private void updateRowWorldReference(final Connection connection,
                                         final String tableName,
                                         final long rowId,
                                         final long targetWorldId)
            throws SQLException {
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE `" + tableName + "` SET `world_id` = ? WHERE `id` = ?")) {
            update.setLong(1, targetWorldId);
            update.setLong(2, rowId);
            update.executeUpdate();
        }
    }

    private void setUpdateWorldReferenceParams(final PreparedStatement update,
                                               final long targetWorldId,
                                               final long playerId,
                                               final long sourceWorldId,
                                               final Optional<TimeRange> range)
            throws SQLException {
        update.setLong(1, targetWorldId);
        update.setLong(2, playerId);
        update.setLong(3, sourceWorldId);
        setRangeParams(update, 4, range);
    }

    private void setRangeParams(final PreparedStatement statement,
                                final int startIndex,
                                final Optional<TimeRange> range)
            throws SQLException {
        if (range.isEmpty()) {
            return;
        }
        setRangeParam(statement, startIndex, range.get().startInclusive());
        setRangeParam(statement, startIndex + 1, range.get().endExclusive());
    }

    private void applyDeleteRequest(final Connection connection, final StorageDeleteRequest request) throws SQLException {
        if (request.playerUuid().isPresent() || request.timeRange().isPresent()) {
            deleteSelectedRows(connection, request);
            cleanupDeleteScope(connection, request.scope());
            return;
        }
        if (request.scope().type() == StorageMaintenanceScope.Type.SERVER) {
            deleteServerScope(connection, request.scope().server());
        } else {
            deleteWorldScope(connection, request.scope().server(), request.scope().world());
        }
        cleanupDeleteScope(connection, request.scope());
    }

    private void deleteSelectedRows(final Connection connection, final StorageDeleteRequest request) throws SQLException {
        final DeleteSelection selection = selectDeleteRows(connection, request);
        deleteRowsById(connection, timeTableName, selection.sessionIds());
        deleteRowsById(connection, adjustmentTableName, selection.adjustmentIds());
    }

    private DeleteSelection selectDeleteRows(final Connection connection, final StorageDeleteRequest request)
            throws SQLException {
        final Set<Long> playerIds = new HashSet<>();
        final List<Long> sessionIds = selectDeleteSessionRows(connection, request, playerIds);
        final List<Long> adjustmentIds = selectDeleteAdjustmentRows(connection, request, playerIds);
        return new DeleteSelection(sessionIds, adjustmentIds, playerIds);
    }

    private List<Long> selectDeleteSessionRows(final Connection connection,
                                               final StorageDeleteRequest request,
                                               final Set<Long> playerIds)
            throws SQLException {
        final StorageMaintenanceScope scope = request.scope();
        final boolean serverScope = scope.type() == StorageMaintenanceScope.Type.SERVER;
        final StringBuilder sql = new StringBuilder("SELECT t.`id`, t.`player_id`, t.`join_time`, t.`leave_time` "
                + "FROM `" + timeTableName + "` t "
                + "JOIN `" + worldTableName + "` w ON w.`id` = t.`world_id` "
                + "JOIN `" + serverTableName + "` s ON s.`id` = w.`server_id` "
                + "WHERE s.`server` = ?");
        if (!serverScope) {
            sql.append(" AND w.`world` = ?");
        }
        request.playerUuid().ifPresent(ignored -> sql.append(" AND t.`player_id` = ?"));
        try (PreparedStatement select = connection.prepareStatement(sql.toString())) {
            int index = 1;
            select.setString(index++, scope.server());
            if (!serverScope) {
                select.setString(index++, scope.world());
            }
            if (request.playerUuid().isPresent()) {
                final Optional<Long> playerId = playerTable.findIdByUuid(connection, request.playerUuid().get());
                if (playerId.isEmpty()) {
                    return List.of();
                }
                select.setLong(index, playerId.get());
            }
            return readDeleteSessionRows(select, request.timeRange(), playerIds);
        }
    }

    private List<Long> readDeleteSessionRows(final PreparedStatement select,
                                             final Optional<TimeRange> range,
                                             final Set<Long> playerIds)
            throws SQLException {
        final List<Long> ids = new ArrayList<>();
        try (ResultSet result = select.executeQuery()) {
            while (result.next()) {
                if (range.isEmpty() || isFullyInside(result, range.get())) {
                    ids.add(result.getLong("id"));
                    playerIds.add(result.getLong("player_id"));
                }
            }
        }
        return ids;
    }

    private List<Long> selectDeleteAdjustmentRows(final Connection connection,
                                                  final StorageDeleteRequest request,
                                                  final Set<Long> playerIds)
            throws SQLException {
        final StorageMaintenanceScope scope = request.scope();
        final boolean serverScope = scope.type() == StorageMaintenanceScope.Type.SERVER;
        final StringBuilder sql = new StringBuilder("SELECT a.`id`, a.`player_id`, a.`created_at` "
                + "FROM `" + adjustmentTableName + "` a "
                + "LEFT JOIN `" + worldTableName + "` w ON w.`id` = a.`world_id` "
                + "LEFT JOIN `" + serverTableName + "` ws ON ws.`id` = w.`server_id` "
                + "LEFT JOIN `" + serverTableName + "` ss ON ss.`id` = a.`server_id` ");
        if (serverScope) {
            sql.append("WHERE ((a.`scope_type` = 'SERVER' AND ss.`server` = ?) "
                    + "OR (a.`scope_type` = 'WORLD' AND ws.`server` = ?))");
        } else {
            sql.append("WHERE a.`scope_type` = 'WORLD' AND ws.`server` = ? AND w.`world` = ?");
        }
        request.playerUuid().ifPresent(ignored -> sql.append(" AND a.`player_id` = ?"));
        try (PreparedStatement select = connection.prepareStatement(sql.toString())) {
            int index = 1;
            select.setString(index++, scope.server());
            if (serverScope) {
                select.setString(index++, scope.server());
            } else {
                select.setString(index++, scope.world());
            }
            if (request.playerUuid().isPresent()) {
                final Optional<Long> playerId = playerTable.findIdByUuid(connection, request.playerUuid().get());
                if (playerId.isEmpty()) {
                    return List.of();
                }
                select.setLong(index, playerId.get());
            }
            return readDeleteAdjustmentRows(select, request.timeRange(), playerIds);
        }
    }

    private List<Long> readDeleteAdjustmentRows(final PreparedStatement select,
                                                final Optional<TimeRange> range,
                                                final Set<Long> playerIds)
            throws SQLException {
        final List<Long> ids = new ArrayList<>();
        try (ResultSet result = select.executeQuery()) {
            while (result.next()) {
                if (range.isEmpty() || range.get().contains(readInstant(result, "created_at"))) {
                    ids.add(result.getLong("id"));
                    playerIds.add(result.getLong("player_id"));
                }
            }
        }
        return ids;
    }

    private void deleteRowsById(final Connection connection, final String tableName, final List<Long> ids)
            throws SQLException {
        if (ids.isEmpty()) {
            return;
        }
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM `" + tableName + "` WHERE `id` = ?")) {
            for (final long id : ids) {
                delete.setLong(1, id);
                delete.addBatch();
            }
            delete.executeBatch();
        }
    }

    private void cleanupDeleteScope(final Connection connection, final StorageMaintenanceScope scope)
            throws SQLException {
        if (scope.type() == StorageMaintenanceScope.Type.SERVER) {
            final Optional<Long> serverId = serverTable.findId(connection, scope.server());
            if (serverId.isEmpty()) {
                return;
            }
            for (final long worldId : worldIdsForServer(connection, serverId.get())) {
                deleteWorldIfUnreferenced(connection, worldId);
            }
            deleteServerIfUnreferenced(connection, serverId.get());
            return;
        }
        final Optional<Long> worldId = worldTable.findId(connection, scope.server(), scope.world());
        final Optional<Long> serverId = serverTable.findId(connection, scope.server());
        if (worldId.isPresent()) {
            deleteWorldIfUnreferenced(connection, worldId.get());
        }
        if (serverId.isPresent()) {
            deleteServerIfUnreferenced(connection, serverId.get());
        }
    }

    private List<Long> worldIdsForServer(final Connection connection, final long serverId) throws SQLException {
        final List<Long> ids = new ArrayList<>();
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT `id` FROM `" + worldTableName + "` WHERE `server_id` = ?")) {
            select.setLong(1, serverId);
            try (ResultSet result = select.executeQuery()) {
                while (result.next()) {
                    ids.add(result.getLong("id"));
                }
            }
        }
        return ids;
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
                || singleLong(connection, "SELECT COUNT(*) FROM `" + adjustmentTableName + "` WHERE `world_id` = ?", worldId) > 0
                || singleLong(connection, "SELECT COUNT(*) FROM `" + afkPeriodTableName + "` WHERE `world_id` = ?", worldId) > 0) {
            return;
        }
        try (PreparedStatement delete = connection.prepareStatement("DELETE FROM `" + worldTableName + "` WHERE `id` = ?")) {
            delete.setLong(1, worldId);
            delete.executeUpdate();
        }
    }

    private void deleteServerIfUnreferenced(final Connection connection, final long serverId) throws SQLException {
        for (final long worldId : worldIdsForServer(connection, serverId)) {
            deleteWorldIfUnreferenced(connection, worldId);
        }
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
                if (params[i] instanceof final Long value) {
                    select.setLong(i + 1, value);
                } else if (params[i] instanceof final Timestamp value) {
                    select.setTimestamp(i + 1, value);
                } else {
                    select.setString(i + 1, params[i].toString());
                }
            }
            try (ResultSet result = select.executeQuery()) {
                return result.next() ? result.getLong(1) : 0L;
            }
        }
    }

    private List<Long> matchingSessionIds(final Connection connection,
                                          final long playerId,
                                          final String sqlTail,
                                          final TimeRange range,
                                          final Object... params)
            throws SQLException {
        final List<Long> ids = new ArrayList<>();
        final String sql = "SELECT t.`id`, t.`join_time`, t.`leave_time` FROM `" + timeTableName + "` t " + sqlTail;
        try (PreparedStatement select = connection.prepareStatement(sql)) {
            select.setLong(1, playerId);
            bindParams(select, 2, params);
            try (ResultSet result = select.executeQuery()) {
                while (result.next()) {
                    if (isFullyInside(result, range)) {
                        ids.add(result.getLong("id"));
                    }
                }
            }
        }
        return ids;
    }

    private List<Long> matchingAdjustmentIds(final Connection connection,
                                             final long playerId,
                                             final String sqlTail,
                                             final TimeRange range,
                                             final Object... params)
            throws SQLException {
        final List<Long> ids = new ArrayList<>();
        final String sql = "SELECT a.`id`, a.`created_at` FROM `" + adjustmentTableName + "` a " + sqlTail;
        try (PreparedStatement select = connection.prepareStatement(sql)) {
            select.setLong(1, playerId);
            bindParams(select, 2, params);
            try (ResultSet result = select.executeQuery()) {
                while (result.next()) {
                    if (range.contains(readInstant(result, "created_at"))) {
                        ids.add(result.getLong("id"));
                    }
                }
            }
        }
        return ids;
    }

    private List<Long> matchingAfkPeriodIds(final Connection connection,
                                            final long playerId,
                                            final long worldId,
                                            final TimeRange range) throws SQLException {
        final List<Long> ids = new ArrayList<>();
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT `id`, `started_at`, `ended_at` FROM `" + afkPeriodTableName
                        + "` WHERE `player_id` = ? AND `world_id` = ?")) {
            select.setLong(1, playerId);
            select.setLong(2, worldId);
            try (ResultSet result = select.executeQuery()) {
                while (result.next()) {
                    final Object endedAt = result.getObject("ended_at");
                    if (endedAt != null && !readInstant(result, "started_at").isBefore(range.startInclusive())
                            && readInstant(result, "ended_at").isBefore(range.endExclusive())) {
                        ids.add(result.getLong("id"));
                    }
                }
            }
        }
        return ids;
    }

    private void bindParams(final PreparedStatement statement, final int startIndex, final Object... params)
            throws SQLException {
        for (int index = 0; index < params.length; index++) {
            final Object param = params[index];
            if (param instanceof final Long value) {
                statement.setLong(startIndex + index, value);
            } else {
                statement.setString(startIndex + index, param.toString());
            }
        }
    }

    private boolean isFullyInside(final ResultSet result, final TimeRange range) throws SQLException {
        return !readInstant(result, "join_time").isBefore(range.startInclusive())
                && readInstant(result, "leave_time").isBefore(range.endExclusive());
    }

    private Instant readInstant(final ResultSet result, final String column) throws SQLException {
        final Object value = result.getObject(column);
        if (value instanceof final Timestamp timestamp) {
            return timestamp.toInstant();
        }
        if (value instanceof final Number number) {
            return Instant.ofEpochMilli(number.longValue());
        }
        if (value instanceof final LocalDateTime localDateTime) {
            return localDateTime.toInstant(ZoneOffset.UTC);
        }
        if (value instanceof final String text && !text.isBlank()) {
            try {
                return Instant.ofEpochMilli(Long.parseLong(text));
            } catch (final NumberFormatException ignored) {
                // Continue with timestamp formats below.
            }
            try {
                return Timestamp.valueOf(text).toInstant();
            } catch (final IllegalArgumentException ignored) {
                // Continue with ISO-8601 parsing below.
            }
            try {
                return Instant.parse(text);
            } catch (final DateTimeParseException exception) {
                throw new SQLException("Unsupported timestamp value: " + text, exception);
            }
        }
        throw new SQLException("Unsupported timestamp value for column " + column + ": " + value);
    }

    private String sessionRangeCondition(final Optional<TimeRange> range) {
        if (range.isEmpty()) {
            return "";
        }
        if (dialect == DatabaseDialect.SQLITE) {
            return " AND " + sqliteEpochMillis("t.`join_time`") + " >= ? AND "
                    + sqliteEpochMillis("t.`leave_time`") + " < ?";
        }
        return " AND t.`join_time` >= ? AND t.`leave_time` < ?";
    }

    private String adjustmentRangeCondition(final Optional<TimeRange> range) {
        if (range.isEmpty()) {
            return "";
        }
        if (dialect == DatabaseDialect.SQLITE) {
            return " AND " + sqliteEpochMillis("a.`created_at`") + " >= ? AND "
                    + sqliteEpochMillis("a.`created_at`") + " < ?";
        }
        return " AND a.`created_at` >= ? AND a.`created_at` < ?";
    }

    private String sqliteEpochMillis(final String column) {
        return "CASE WHEN typeof(" + column + ") IN ('integer', 'real') THEN CAST(" + column
                + " AS INTEGER) WHEN trim(" + column + ") <> '' AND trim(" + column
                + ") NOT GLOB '*[^0-9]*' THEN CAST(" + column
                + " AS INTEGER) ELSE CAST(ROUND((julianday(" + column
                + ", 'utc') - 2440587.5) * 86400000) AS INTEGER) END";
    }

    private void setRangeParam(final PreparedStatement statement, final int index, final Instant instant)
            throws SQLException {
        if (dialect == DatabaseDialect.SQLITE) {
            statement.setLong(index, instant.toEpochMilli());
            return;
        }
        statement.setTimestamp(index, Timestamp.from(instant));
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

    private record ScopeReferences(OptionalLong serverId, OptionalLong worldId) {
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

    private record DeleteSelection(List<Long> sessionIds, List<Long> adjustmentIds, Set<Long> playerIds) {
    }

    private record WorldRow(long worldId, String world) {
    }

    private record StorageSnapshot(List<PlayerRow> players,
                                   List<ServerRow> servers,
                                   List<WorldSnapshotRow> worlds) {
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

    private record AfkPeriodRow(byte[] playerUuid,
                                String server,
                                String world,
                                Object startedAt,
                                Object endedAt,
                                String endReason) {
    }
}
