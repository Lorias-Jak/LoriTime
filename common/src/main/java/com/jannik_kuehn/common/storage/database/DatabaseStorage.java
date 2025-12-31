package com.jannik_kuehn.common.storage.database;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.logger.LoriTimeLogger;
import com.jannik_kuehn.common.api.storage.NameStorage;
import com.jannik_kuehn.common.api.storage.TimeStorage;
import com.jannik_kuehn.common.config.Configuration;
import com.jannik_kuehn.common.exception.StorageException;
import com.jannik_kuehn.common.utils.UuidUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@SuppressWarnings({"PMD.CommentRequired", "PMD.TooManyMethods"})
public class DatabaseStorage implements NameStorage, TimeStorage {

    private static final String DEFAULT_SERVER_NAME = "default";
    private static final String DEFAULT_WORLD_NAME = "global";

    private final MySQL mySQL;
    private final LoriTimeLogger log;
    private final ReadWriteLock poolLock;

    private final String legacyTable;
    private final PlayerTable playerTable;
    private final ServerTable serverTable;
    private final WorldTable worldTable;
    private final TimeTable timeTable;
    private final StatisticTable statisticTable;

    public DatabaseStorage(final Configuration config, final LoriTimePlugin loriTimePlugin) {
        this.log = loriTimePlugin.getLoggerFactory().create(DatabaseStorage.class);
        this.mySQL = new MySQL(config, loriTimePlugin);
        this.poolLock = new ReentrantReadWriteLock();

        this.legacyTable = mySQL.getTablePrefix();
        final String playerTableName = mySQL.getTablePrefix() + "_player";
        final String serverTableName = mySQL.getTablePrefix() + "_server";
        final String worldTableName = mySQL.getTablePrefix() + "_world";
        final String timeTableName = mySQL.getTablePrefix() + "_time";
        final String statisticTableName = mySQL.getTablePrefix() + "_statistic";

        this.playerTable = new PlayerTable(playerTableName);
        this.serverTable = new ServerTable(serverTableName);
        this.worldTable = new WorldTable(worldTableName, serverTable);
        this.timeTable = new TimeTable(timeTableName, playerTableName, worldTableName);
        this.statisticTable = new StatisticTable(statisticTableName);

        mySQL.open();

        try (Connection connection = mySQL.getConnection()) {
            createSchema(connection);
            migrateLegacyData(connection);
        } catch (final SQLException ex) {
            log.error("Error creating table", ex);
        }
    }

    private void createSchema(final Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(playerTable.createTableSql())) {
            statement.execute();
        }
        try (PreparedStatement statement = connection.prepareStatement(serverTable.createTableSql())) {
            statement.execute();
        }
        try (PreparedStatement statement = connection.prepareStatement(worldTable.createTableSql())) {
            statement.execute();
        }
        try (PreparedStatement statement = connection.prepareStatement(timeTable.createTableSql())) {
            statement.execute();
        }
        try (PreparedStatement statement = connection.prepareStatement(statisticTable.createTableSql())) {
            statement.execute();
        }
    }

    private void migrateLegacyData(final Connection connection) throws SQLException {
        if (!tableExists(connection, legacyTable)) {
            return;
        }
        if (tableExists(connection, playerTable.getTableName()) && playerTable.hasAnyData(connection)) {
            return;
        }
        log.info("Migrating legacy LoriTime table to the new schema ...");
        final long worldId = worldTable.ensureWorld(connection, DEFAULT_SERVER_NAME, DEFAULT_WORLD_NAME);

        try (PreparedStatement selectLegacy = connection.prepareStatement(
                "SELECT `uuid`, `name`, `time` FROM `" + legacyTable + "`")) {
            try (ResultSet result = selectLegacy.executeQuery()) {
                while (result.next()) {
                    final UUID uuid = UuidUtil.fromBytes(result.getBytes("uuid"));
                    final Optional<String> name = Optional.ofNullable(result.getString("name"));
                    final long timeSeconds = result.getLong("time");
                    final long playerId = playerTable.ensurePlayer(connection, uuid, name);
                    timeTable.insertDuration(connection, playerId, worldId, timeSeconds);
                }
            }
        }

        try (Statement dropLegacy = connection.createStatement()) {
            dropLegacy.execute("DROP TABLE IF EXISTS `" + legacyTable + "`");
        }
        log.info("Legacy migration completed.");
    }

    private boolean tableExists(final Connection connection, final String tableName) throws SQLException {
        final String query = "SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, tableName);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    @Override
    public Optional<UUID> getUuid(final String name) throws StorageException {
        Objects.requireNonNull(name);
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = mySQL.getConnection()) {
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
            try (Connection connection = mySQL.getConnection()) {
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
            try (Connection connection = mySQL.getConnection()) {
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
        Objects.requireNonNull(uuid);
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = mySQL.getConnection()) {
                final long worldId = worldTable.ensureWorld(connection, DEFAULT_SERVER_NAME, DEFAULT_WORLD_NAME);
                final long playerId = playerTable.ensurePlayer(connection, uuid, Optional.empty());
                timeTable.insertDuration(connection, playerId, worldId, additionalTime);
            }
        } catch (final SQLException ex) {
            throw new StorageException(ex);
        } finally {
            poolLock.readLock().unlock();
        }
    }

    @Override
    public void addTimes(final Map<UUID, Long> additionalTimes) throws StorageException {
        if (additionalTimes == null || additionalTimes.isEmpty()) {
            return;
        }
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = mySQL.getConnection()) {
                final long worldId = worldTable.ensureWorld(connection, DEFAULT_SERVER_NAME, DEFAULT_WORLD_NAME);
                for (final Map.Entry<UUID, Long> entry : additionalTimes.entrySet()) {
                    final UUID uuid = entry.getKey();
                    final long playerId = playerTable.ensurePlayer(connection, uuid, Optional.empty());
                    timeTable.insertDuration(connection, playerId, worldId, entry.getValue());
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
            try (Connection connection = mySQL.getConnection()) {
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
            try (Connection connection = mySQL.getConnection()) {
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
            try (Connection connection = mySQL.getConnection()) {
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
            try (Connection connection = mySQL.getConnection()) {
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

    private void deleteUser(final UUID uuid) throws StorageException {
        if (uuid == null) {
            return;
        }
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = mySQL.getConnection()) {
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
        if (!mySQL.isClosed()) {
            mySQL.close();
        }
    }

    private void checkClosed() throws StorageException {
        if (mySQL.isClosed()) {
            throw new StorageException("closed");
        }
    }
}
