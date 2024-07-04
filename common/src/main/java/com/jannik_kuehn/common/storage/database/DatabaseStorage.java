package com.jannik_kuehn.common.storage.database;

import com.jannik_kuehn.common.LoriTimePlugin;
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
import java.sql.Types;
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

public class DatabaseStorage implements NameStorage, TimeStorage {

    private final MySQL mySQL;

    private final ReadWriteLock poolLock;

    public DatabaseStorage(final Configuration config, final LoriTimePlugin plugin) {
        this.mySQL = new MySQL(config, plugin);
        mySQL.open();
        this.poolLock = new ReentrantReadWriteLock();

        try (
                Connection connection = mySQL.getConnection();
                Statement statement = connection.createStatement()
        ) {
            statement.execute(createTable());
        } catch (final SQLException ex) {
            plugin.getLogger().error("Error creating table", ex);
        }
    }

    private String createTable() {
        return "CREATE TABLE IF NOT EXISTS `" + mySQL.getTablePrefix() + "` ("
                + "`id`   INT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                + "`uuid` BINARY(16) NOT NULL UNIQUE,"
                + "`name` CHAR(16) CHARACTER SET ascii UNIQUE,"
                + "`time` BIGINT UNSIGNED NOT NULL DEFAULT 0"
                + ") ENGINE InnoDB";
    }

    private String getByUuid() {
        return "SELECT `name`, `time` FROM `" + mySQL.getTablePrefix() + "` WHERE `uuid` = ?";
    }

    private String getAllEntriesSet() {
        return "SELECT `uuid` AS uuid, `time` FROM `" + mySQL.getTablePrefix() + "`";
    }

    private String getAllNameEntries() {
        return "SELECT `name` FROM `" + mySQL.getTablePrefix() + "`";
    }

    private String getByName() {
        return "SELECT `uuid` AS uuid FROM `" + mySQL.getTablePrefix() + "` WHERE `name` = ?";
    }

    private String unsetTakenName() {
        return "UPDATE `" + mySQL.getTablePrefix() + "` SET name = NULL WHERE `uuid` = ?";
    }

    private String insertOrUpdateEntry() {
        return "INSERT INTO `" + mySQL.getTablePrefix() + "` (`uuid`, `name`, `time`) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE `name` = ?, `time` = `time` + ?";
    }

    @Override
    public Optional<UUID> getUuid(final String name) throws StorageException {
        Objects.requireNonNull(name);
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = mySQL.getConnection()) {
                return getUuid(connection, name);
            }
        } catch (final SQLException ex) {
            throw new StorageException(ex);
        } finally {
            poolLock.readLock().unlock();
        }
    }

    private Optional<UUID> getUuid(final Connection connection, final String name) throws SQLException {
        try (PreparedStatement getByNameStatement = connection.prepareStatement(getByName())) {
            getByNameStatement.setString(1, name);
            try (ResultSet result = getByNameStatement.executeQuery()) {
                if (result.next()) {
                    return Optional.of(UuidUtil.fromBytes(result.getBytes("uuid")));
                } else {
                    return Optional.empty();
                }
            }
        }
    }

    @Override
    public Optional<String> getName(final UUID uniqueId) throws StorageException {
        Objects.requireNonNull(uniqueId);
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = mySQL.getConnection()) {
                return getName(connection, uniqueId);
            }
        } catch (final SQLException ex) {
            throw new StorageException(ex);
        } finally {
            poolLock.readLock().unlock();
        }
    }

    private Optional<String> getName(final Connection connection, final UUID uuid) throws SQLException {
        try (PreparedStatement getByUuidStatement = connection.prepareStatement(getByUuid())) {
            getByUuidStatement.setBytes(1, UuidUtil.toBytes(uuid));
            try (ResultSet result = getByUuidStatement.executeQuery()) {
                if (result.next()) {
                    return Optional.of(result.getString("name"));
                } else {
                    return Optional.empty();
                }
            }
        }
    }

    @Override
    public OptionalLong getTime(final UUID uniqueId) throws StorageException {
        Objects.requireNonNull(uniqueId);
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = mySQL.getConnection()) {
                return getOnlineTime(connection, uniqueId);
            }
        } catch (final SQLException ex) {
            throw new StorageException(ex);
        } finally {
            poolLock.readLock().unlock();
        }
    }

    private OptionalLong getOnlineTime(final Connection connection, final UUID uuid) throws SQLException {
        try (PreparedStatement getByUuidStatement = connection.prepareStatement(getByUuid())) {
            getByUuidStatement.setBytes(1, UuidUtil.toBytes(uuid));
            try (ResultSet result = getByUuidStatement.executeQuery()) {
                if (result.next()) {
                    return OptionalLong.of(result.getLong("time"));
                } else {
                    return OptionalLong.empty();
                }
            }
        }
    }

    @Override
    public void addTime(final UUID uuid, final long additionalTime) throws StorageException {
        Objects.requireNonNull(uuid);
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = mySQL.getConnection()) {
                addOnlineTime(connection, uuid, additionalTime);
            }
        } catch (final SQLException ex) {
            throw new StorageException(ex);
        } finally {
            poolLock.readLock().unlock();
        }
    }

    private void addOnlineTime(final Connection connection, final UUID uuid, final long additionalOnlineTime) throws SQLException {
        try (PreparedStatement insertOrUpdateEntryStatement = connection.prepareStatement(insertOrUpdateEntry())) {
            final Optional<String> name = getName(connection, uuid);
            insertOrUpdateEntryParams(insertOrUpdateEntryStatement, uuid, name, additionalOnlineTime);
            insertOrUpdateEntryStatement.executeUpdate();
        }
    }

    @Override
    public void addTimes(final Map<UUID, Long> additionalTimes) throws StorageException {
        if (additionalTimes == null) {
            return;
        }
        poolLock.readLock().lock();
        try {
            try (Connection connection = mySQL.getConnection()) {
                addOnlineTimes(connection, additionalTimes);
            }
        } catch (final SQLException ex) {
            throw new StorageException(ex);
        } finally {
            poolLock.readLock().unlock();
        }
    }

    private void addOnlineTimes(final Connection connection, final Map<UUID, Long> additionalOnlineTimes) throws SQLException {
        try (PreparedStatement insertOrUpdateEntryStatement = connection.prepareStatement(insertOrUpdateEntry())) {
            for (final Map.Entry<UUID, Long> entry : additionalOnlineTimes.entrySet()) {
                final UUID uuid = entry.getKey();
                final Optional<String> name = getName(connection, uuid);
                final long additionalOnlineTime = entry.getValue();
                insertOrUpdateEntryParams(insertOrUpdateEntryStatement, uuid, name, additionalOnlineTime);
                insertOrUpdateEntryStatement.addBatch();
            }
            insertOrUpdateEntryStatement.executeBatch();
        }
    }

    private void insertOrUpdateEntryParams(final PreparedStatement insertOrUpdateEntryStatement, final UUID uuid, final Optional<String> name, final long additionalOnlineTime) throws SQLException {
        insertOrUpdateEntryStatement.setBytes(1, UuidUtil.toBytes(uuid));
        if (name.isPresent()) {
            insertOrUpdateEntryStatement.setString(2, name.get());
            insertOrUpdateEntryStatement.setString(4, name.get());
        } else {
            insertOrUpdateEntryStatement.setNull(2, Types.CHAR);
            insertOrUpdateEntryStatement.setNull(4, Types.CHAR);
        }
        insertOrUpdateEntryStatement.setLong(3, Math.max(0, additionalOnlineTime));
        insertOrUpdateEntryStatement.setLong(5, additionalOnlineTime);
    }

    @Override
    public void setEntry(final UUID uuid, final String name) throws StorageException {
        Objects.requireNonNull(uuid);
        Objects.requireNonNull(name);
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = mySQL.getConnection()) {
                setEntry(connection, uuid, name);
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

    private void setEntry(final Connection connection, final UUID uuid, final String name) throws SQLException {
        final Optional<UUID> oldNameHolder = getUuid(connection, name);
        if (oldNameHolder.filter(oldUuid -> !oldUuid.equals(uuid)).isPresent()) { // name not unique ? update on duplicate uuid
            try (PreparedStatement unsetTakenNameStatement = connection.prepareStatement(unsetTakenName())) {
                unsetTakenNameStatement.setBytes(1, UuidUtil.toBytes(oldNameHolder.get()));
                unsetTakenNameStatement.executeUpdate();
            }
        }
        try (PreparedStatement insertOrUpdateEntryStatement = connection.prepareStatement(insertOrUpdateEntry())) {
            insertOrUpdateEntryStatement.setBytes(1, UuidUtil.toBytes(uuid));
            insertOrUpdateEntryStatement.setString(2, name);
            insertOrUpdateEntryStatement.setString(4, name);
            insertOrUpdateEntryStatement.setLong(3, 0);
            insertOrUpdateEntryStatement.setLong(5, 0);
            insertOrUpdateEntryStatement.executeUpdate();
        }
    }

    @Override
    public void setEntries(final Map<UUID, String> entries) throws StorageException {
        if (entries == null) {
            return;
        }
        poolLock.readLock().lock();
        try {
            try (Connection connection = mySQL.getConnection()) {
                setEntries(connection, entries);
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
                return getNameEntries(connection);
            }
        } catch (final SQLException ex) {
            throw new StorageException(ex);
        } finally {
            poolLock.readLock().unlock();
        }
    }

    private Set<String> getNameEntries(final Connection connection) throws SQLException {
        try (PreparedStatement getByUuidStatement = connection.prepareStatement(getAllNameEntries())) {
            try (ResultSet result = getByUuidStatement.executeQuery()) {
                final Set<String> nameSet = new HashSet<>();
                while (result.next()) {
                    nameSet.add(result.getString("name"));
                }
                return nameSet;
            }
        }
    }

    @Override
    public Map<String, ?> getAllTimeEntries() throws StorageException {
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = mySQL.getConnection()) {
                return getAllTimeEntries(connection);
            }
        } catch (final SQLException ex) {
            throw new StorageException(ex);
        } finally {
            poolLock.readLock().unlock();
        }
    }

    private Map<String, ?> getAllTimeEntries(final Connection connection) throws SQLException {
        try (PreparedStatement getByUuidStatement = connection.prepareStatement(getAllEntriesSet())) {
            try (ResultSet result = getByUuidStatement.executeQuery()) {
                final Map<String, Long> test = new HashMap<>();
                while (result.next()) {
                    test.put(UuidUtil.fromBytes(result.getBytes("uuid")).toString(), result.getLong("time"));
                }
                return test;
            }
        }
    }

    @Override
    public void close() throws StorageException {
        if (!mySQL.isClosed()) {
            mySQL.close();
        }
    }

    private void setEntries(final Connection connection, final Map<UUID, String> entries) throws SQLException {
        try (PreparedStatement unsetTakenNameStatement = connection.prepareStatement(unsetTakenName());
             PreparedStatement insertOrUpdateEntryStatement = connection.prepareStatement(insertOrUpdateEntry())) {
            for (final Map.Entry<UUID, String> entry : entries.entrySet()) {
                final UUID uuid = entry.getKey();
                final String name = entry.getValue();
                final Optional<UUID> oldNameHolder = getUuid(connection, name);
                if (oldNameHolder.filter(oldUuid -> !oldUuid.equals(uuid)).isPresent()) { // name not unique ? update on duplicate uuid
                    unsetTakenNameStatement.setBytes(1, UuidUtil.toBytes(oldNameHolder.get()));
                    unsetTakenNameStatement.addBatch();
                }
                insertOrUpdateEntryStatement.setBytes(1, UuidUtil.toBytes(uuid));
                insertOrUpdateEntryStatement.setString(2, name);
                insertOrUpdateEntryStatement.setString(4, name);
                insertOrUpdateEntryStatement.setLong(3, 0);
                insertOrUpdateEntryStatement.setLong(5, 0);
                insertOrUpdateEntryStatement.addBatch();
            }
            unsetTakenNameStatement.executeBatch();
            insertOrUpdateEntryStatement.executeBatch();
        }
    }

    private void checkClosed() throws StorageException {
        if (mySQL.isClosed()) {
            throw new StorageException("closed");
        }
    }
}
