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
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
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
    private final String playerTable;
    private final String serverTable;
    private final String worldTable;
    private final String timeTable;
    private final String statisticTable;

    public DatabaseStorage(final Configuration config, final LoriTimePlugin loriTimePlugin) {
        this.log = loriTimePlugin.getLoggerFactory().create(DatabaseStorage.class);
        this.mySQL = new MySQL(config, loriTimePlugin);
        this.poolLock = new ReentrantReadWriteLock();

        this.legacyTable = mySQL.getTablePrefix();
        this.playerTable = mySQL.getTablePrefix() + "_player";
        this.serverTable = mySQL.getTablePrefix() + "_server";
        this.worldTable = mySQL.getTablePrefix() + "_world";
        this.timeTable = mySQL.getTablePrefix() + "_time";
        this.statisticTable = mySQL.getTablePrefix() + "_statistic";

        mySQL.open();

        try (Connection connection = mySQL.getConnection()) {
            createSchema(connection);
            migrateLegacyData(connection);
        } catch (final SQLException ex) {
            log.error("Error creating table", ex);
        }
    }

    private void createSchema(final Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(createPlayerTable());
            statement.execute(createServerTable());
            statement.execute(createWorldTable());
            statement.execute(createTimeTable());
            statement.execute(createStatisticTable());
        }
    }

    private String createPlayerTable() {
        return "CREATE TABLE IF NOT EXISTS `" + playerTable + "` ("
                + "`id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                + "`uuid` BINARY(16) NOT NULL UNIQUE,"
                + "`name` VARCHAR(16) CHARACTER SET ascii UNIQUE,"
                + "`last_seen` TIMESTAMP NULL"
                + ") ENGINE InnoDB";
    }

    private String createServerTable() {
        return "CREATE TABLE IF NOT EXISTS `" + serverTable + "` ("
                + "`id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                + "`server` VARCHAR(64) NOT NULL UNIQUE"
                + ") ENGINE InnoDB";
    }

    private String createWorldTable() {
        return "CREATE TABLE IF NOT EXISTS `" + worldTable + "` ("
                + "`id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                + "`server_id` BIGINT NOT NULL,"
                + "`world` VARCHAR(64) NOT NULL,"
                + "UNIQUE KEY `uk_world` (`server_id`, `world`),"
                + "CONSTRAINT `fk_world_server` FOREIGN KEY (`server_id`) REFERENCES `" + serverTable + "`(`id`) ON DELETE CASCADE"
                + ") ENGINE InnoDB";
    }

    private String createTimeTable() {
        return "CREATE TABLE IF NOT EXISTS `" + timeTable + "` ("
                + "`id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                + "`player_id` BIGINT NOT NULL,"
                + "`world_id` BIGINT NOT NULL,"
                + "`join_time` TIMESTAMP NOT NULL,"
                + "`leave_time` TIMESTAMP NOT NULL,"
                + "INDEX `idx_time_player` (`player_id`),"
                + "INDEX `idx_time_world` (`world_id`),"
                + "CONSTRAINT `fk_time_player` FOREIGN KEY (`player_id`) REFERENCES `" + playerTable + "`(`id`) ON DELETE CASCADE,"
                + "CONSTRAINT `fk_time_world` FOREIGN KEY (`world_id`) REFERENCES `" + worldTable + "`(`id`) ON DELETE CASCADE"
                + ") ENGINE InnoDB";
    }

    private String createStatisticTable() {
        return "CREATE TABLE IF NOT EXISTS `" + statisticTable + "` ("
                + "`id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                + "`statistic_name` VARCHAR(64) NOT NULL,"
                + "`calculation_time` TIMESTAMP NOT NULL,"
                + "`result` BIGINT NOT NULL,"
                + "UNIQUE KEY `uk_statistic` (`statistic_name`, `calculation_time`)",
                + "INDEX `idx_statistic_name` (`statistic_name`)",
                + "INDEX `idx_statistic_time` (`calculation_time`)"
                + ") ENGINE InnoDB";
    }

    private void migrateLegacyData(final Connection connection) throws SQLException {
        if (!tableExists(connection, legacyTable)) {
            return;
        }
        if (tableExists(connection, playerTable) && hasAnyPlayerData(connection)) {
            return;
        }
        log.info("Migrating legacy LoriTime table to the new schema ...");
        final long worldId = ensureWorld(connection, DEFAULT_SERVER_NAME, DEFAULT_WORLD_NAME);

        try (PreparedStatement selectLegacy = connection.prepareStatement(
                "SELECT `uuid`, `name`, `time` FROM `" + legacyTable + "`")) {
            try (ResultSet result = selectLegacy.executeQuery()) {
                while (result.next()) {
                    final UUID uuid = UuidUtil.fromBytes(result.getBytes("uuid"));
                    final Optional<String> name = Optional.ofNullable(result.getString("name"));
                    final long timeSeconds = result.getLong("time");
                    final long playerId = ensurePlayer(connection, uuid, name);
                    insertTimeEntry(connection, playerId, worldId, timeSeconds);
                }
            }
        }

        try (Statement dropLegacy = connection.createStatement()) {
            dropLegacy.execute("DROP TABLE IF EXISTS `" + legacyTable + "`");
        }
        log.info("Legacy migration completed.");
    }

    private boolean hasAnyPlayerData(final Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM `" + playerTable + "` LIMIT 1")) {
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
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

    private long ensureServer(final Connection connection, final String server) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT `id` FROM `" + serverTable + "` WHERE `server` = ?")) {
            select.setString(1, server);
            try (ResultSet result = select.executeQuery()) {
                if (result.next()) {
                    return result.getLong("id");
                }
            }
        }
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO `" + serverTable + "` (`server`) VALUES (?)",
                Statement.RETURN_GENERATED_KEYS)) {
            insert.setString(1, server);
            insert.executeUpdate();
            try (ResultSet keys = insert.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("Unable to create server entry");
    }

    private long ensureWorld(final Connection connection, final String server, final String world) throws SQLException {
        final long serverId = ensureServer(connection, server);
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT `id` FROM `" + worldTable + "` WHERE `server_id` = ? AND `world` = ?")) {
            select.setLong(1, serverId);
            select.setString(2, world);
            try (ResultSet result = select.executeQuery()) {
                if (result.next()) {
                    return result.getLong("id");
                }
            }
        }
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO `" + worldTable + "` (`server_id`, `world`) VALUES (?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            insert.setLong(1, serverId);
            insert.setString(2, world);
            insert.executeUpdate();
            try (ResultSet keys = insert.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("Unable to create world entry");
    }

    private Optional<Long> findPlayerId(final Connection connection, final UUID uuid) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT `id` FROM `" + playerTable + "` WHERE `uuid` = ?")) {
            select.setBytes(1, UuidUtil.toBytes(uuid));
            try (ResultSet result = select.executeQuery()) {
                if (result.next()) {
                    return Optional.of(result.getLong("id"));
                }
            }
        }
        return Optional.empty();
    }

    private Optional<Long> findPlayerIdByName(final Connection connection, final String name) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT `id` FROM `" + playerTable + "` WHERE `name` = ?")) {
            select.setString(1, name);
            try (ResultSet result = select.executeQuery()) {
                if (result.next()) {
                    return Optional.of(result.getLong("id"));
                }
            }
        }
        return Optional.empty();
    }

    private void releaseTakenName(final Connection connection, final String name, final UUID owner) throws SQLException {
        final Optional<Long> oldHolder = findPlayerIdByName(connection, name);
        if (oldHolder.isPresent()) {
            final Optional<Long> ownerId = findPlayerId(connection, owner);
            if (ownerId.isPresent() && ownerId.get().equals(oldHolder.get())) {
                return;
            }
            try (PreparedStatement unsetName = connection.prepareStatement(
                    "UPDATE `" + playerTable + "` SET `name` = NULL WHERE `id` = ?")) {
                unsetName.setLong(1, oldHolder.get());
                unsetName.executeUpdate();
            }
        }
    }

    private long ensurePlayer(final Connection connection, final UUID uuid, final Optional<String> name) throws SQLException {
        final Optional<Long> existingId = findPlayerId(connection, uuid);
        if (existingId.isPresent()) {
            if (name.isPresent()) {
                releaseTakenName(connection, name.get(), uuid);
                updatePlayerName(connection, existingId.get(), name.get());
            }
            updateLastSeen(connection, existingId.get());
            return existingId.get();
        }

        if (name.isPresent()) {
            releaseTakenName(connection, name.get(), uuid);
        }

        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO `" + playerTable + "` (`uuid`, `name`, `last_seen`) VALUES (?, ?, CURRENT_TIMESTAMP)",
                Statement.RETURN_GENERATED_KEYS)) {
            insert.setBytes(1, UuidUtil.toBytes(uuid));
            if (name.isPresent()) {
                insert.setString(2, name.get());
            } else {
                insert.setNull(2, Types.VARCHAR);
            }
            insert.executeUpdate();
            try (ResultSet keys = insert.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("Unable to insert player");
    }

    private void updatePlayerName(final Connection connection, final long playerId, final String name) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE `" + playerTable + "` SET `name` = ?, `last_seen` = CURRENT_TIMESTAMP WHERE `id` = ?")) {
            update.setString(1, name);
            update.setLong(2, playerId);
            update.executeUpdate();
        }
    }

    private void updateLastSeen(final Connection connection, final long playerId) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE `" + playerTable + "` SET `last_seen` = CURRENT_TIMESTAMP WHERE `id` = ?")) {
            update.setLong(1, playerId);
            update.executeUpdate();
        }
    }

    private void insertTimeEntry(final Connection connection, final long playerId, final long worldId,
                                 final long durationSeconds) throws SQLException {
        final Instant leave = Instant.now();
        final Instant join = leave.minusSeconds(durationSeconds);
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO `" + timeTable + "` (`player_id`, `world_id`, `join_time`, `leave_time`) VALUES (?, ?, ?, ?)")) {
            insert.setLong(1, playerId);
            insert.setLong(2, worldId);
            insert.setTimestamp(3, Timestamp.from(join));
            insert.setTimestamp(4, Timestamp.from(leave));
            insert.executeUpdate();
        }
    }

    @Override
    public Optional<UUID> getUuid(final String name) throws StorageException {
        Objects.requireNonNull(name);
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = mySQL.getConnection();
                 PreparedStatement select = connection.prepareStatement(
                         "SELECT `uuid` FROM `" + playerTable + "` WHERE `name` = ?")) {
                select.setString(1, name);
                try (ResultSet result = select.executeQuery()) {
                    if (result.next()) {
                        return Optional.of(UuidUtil.fromBytes(result.getBytes("uuid")));
                    }
                    return Optional.empty();
                }
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
            try (Connection connection = mySQL.getConnection();
                 PreparedStatement select = connection.prepareStatement(
                         "SELECT `name` FROM `" + playerTable + "` WHERE `uuid` = ?")) {
                select.setBytes(1, UuidUtil.toBytes(uniqueId));
                try (ResultSet result = select.executeQuery()) {
                    if (result.next()) {
                        return Optional.ofNullable(result.getString("name"));
                    }
                    return Optional.empty();
                }
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
            try (Connection connection = mySQL.getConnection();
                 PreparedStatement select = connection.prepareStatement(
                         "SELECT SUM(TIMESTAMPDIFF(SECOND, t.join_time, t.leave_time)) AS total "
                                 + "FROM `" + timeTable + "` t "
                                 + "JOIN `" + playerTable + "` p ON p.id = t.player_id "
                                 + "WHERE p.uuid = ?")) {
                select.setBytes(1, UuidUtil.toBytes(uniqueId));
                try (ResultSet result = select.executeQuery()) {
                    if (result.next()) {
                        final long value = result.getLong("total");
                        if (!result.wasNull()) {
                            return OptionalLong.of(value);
                        }
                    }
                    return OptionalLong.empty();
                }
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
                final long worldId = ensureWorld(connection, DEFAULT_SERVER_NAME, DEFAULT_WORLD_NAME);
                final long playerId = ensurePlayer(connection, uuid, Optional.empty());
                insertTimeEntry(connection, playerId, worldId, additionalTime);
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
                final long worldId = ensureWorld(connection, DEFAULT_SERVER_NAME, DEFAULT_WORLD_NAME);
                for (final Map.Entry<UUID, Long> entry : additionalTimes.entrySet()) {
                    final UUID uuid = entry.getKey();
                    final long playerId = ensurePlayer(connection, uuid, Optional.empty());
                    insertTimeEntry(connection, playerId, worldId, entry.getValue());
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
                ensurePlayer(connection, uuid, Optional.of(name));
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
                    ensurePlayer(connection, entry.getKey(), Optional.ofNullable(entry.getValue()));
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
            try (Connection connection = mySQL.getConnection();
                 PreparedStatement select = connection.prepareStatement(
                         "SELECT `name` FROM `" + playerTable + "` WHERE `name` IS NOT NULL")) {
                final Set<String> names = new HashSet<>();
                try (ResultSet result = select.executeQuery()) {
                    while (result.next()) {
                        names.add(result.getString("name"));
                    }
                }
                return names;
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
            try (Connection connection = mySQL.getConnection();
                 PreparedStatement select = connection.prepareStatement(
                         "SELECT p.uuid AS uuid, SUM(TIMESTAMPDIFF(SECOND, t.join_time, t.leave_time)) AS total "
                                 + "FROM `" + timeTable + "` t "
                                 + "JOIN `" + playerTable + "` p ON p.id = t.player_id "
                                 + "GROUP BY p.uuid")) {
                final Map<String, Long> totals = new HashMap<>();
                try (ResultSet result = select.executeQuery()) {
                    while (result.next()) {
                        totals.put(UuidUtil.fromBytes(result.getBytes("uuid")).toString(), result.getLong("total"));
                    }
                }
                return totals;
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
            try (Connection connection = mySQL.getConnection();
                 PreparedStatement delete = connection.prepareStatement(
                         "DELETE FROM `" + playerTable + "` WHERE `uuid` = ?")) {
                delete.setBytes(1, UuidUtil.toBytes(uuid));
                delete.executeUpdate();
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
