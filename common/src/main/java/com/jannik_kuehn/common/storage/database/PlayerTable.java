package com.jannik_kuehn.common.storage.database;

import com.jannik_kuehn.common.utils.UuidUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Table helper for player entries.
 */
final class PlayerTable {

    private final String tableName;
    private final SqlDialect dialect;

    PlayerTable(final String tableName, final SqlDialect dialect) {
        this.tableName = tableName;
        this.dialect = dialect;
    }

    String createTableSql() {
        return dialect.createPlayerTable(tableName);
    }

    String getTableName() {
        return tableName;
    }

    boolean hasAnyData(final Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM `" + tableName + "` LIMIT 1")) {
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    Optional<Long> findIdByUuid(final Connection connection, final UUID uuid) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT `id` FROM `" + tableName + "` WHERE `uuid` = ?")) {
            select.setBytes(1, UuidUtil.toBytes(uuid));
            try (ResultSet result = select.executeQuery()) {
                if (result.next()) {
                    return Optional.of(result.getLong("id"));
                }
            }
        }
        return Optional.empty();
    }

    Optional<Long> findIdByName(final Connection connection, final String name) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT `id` FROM `" + tableName + "` WHERE `name` = ?")) {
            select.setString(1, name);
            try (ResultSet result = select.executeQuery()) {
                if (result.next()) {
                    return Optional.of(result.getLong("id"));
                }
            }
        }
        return Optional.empty();
    }

    Optional<UUID> findUuidByName(final Connection connection, final String name) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT `uuid` FROM `" + tableName + "` WHERE `name` = ?")) {
            select.setString(1, name);
            try (ResultSet result = select.executeQuery()) {
                if (result.next()) {
                    return Optional.of(UuidUtil.fromBytes(result.getBytes("uuid")));
                }
            }
        }
        return Optional.empty();
    }

    Optional<String> findNameByUuid(final Connection connection, final UUID uuid) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT `name` FROM `" + tableName + "` WHERE `uuid` = ?")) {
            select.setBytes(1, UuidUtil.toBytes(uuid));
            try (ResultSet result = select.executeQuery()) {
                if (result.next()) {
                    return Optional.ofNullable(result.getString("name"));
                }
            }
        }
        return Optional.empty();
    }

    Set<String> getAllNames(final Connection connection) throws SQLException {
        final Set<String> names = new HashSet<>();
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT `name` FROM `" + tableName + "` WHERE `name` IS NOT NULL")) {
            try (ResultSet result = select.executeQuery()) {
                while (result.next()) {
                    names.add(result.getString("name"));
                }
            }
        }
        return names;
    }

    long ensurePlayer(final Connection connection, final UUID uuid, final Optional<String> name) throws SQLException {
        final Optional<Long> existingId = findIdByUuid(connection, uuid);
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
                "INSERT INTO `" + tableName + "` (`uuid`, `name`, `last_seen`) VALUES (?, ?, CURRENT_TIMESTAMP)",
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

    void deleteByUuid(final Connection connection, final UUID uuid) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM `" + tableName + "` WHERE `uuid` = ?")) {
            delete.setBytes(1, UuidUtil.toBytes(uuid));
            delete.executeUpdate();
        }
    }

    private void releaseTakenName(final Connection connection, final String name, final UUID owner) throws SQLException {
        final Optional<Long> oldHolder = findIdByName(connection, name);
        if (oldHolder.isEmpty()) {
            return;
        }

        final Optional<Long> ownerId = findIdByUuid(connection, owner);
        if (ownerId.isPresent() && ownerId.get().equals(oldHolder.get())) {
            return;
        }

        try (PreparedStatement unsetName = connection.prepareStatement(
                "UPDATE `" + tableName + "` SET `name` = NULL WHERE `id` = ?")) {
            unsetName.setLong(1, oldHolder.get());
            unsetName.executeUpdate();
        }
    }

    private void updatePlayerName(final Connection connection, final long playerId, final String name) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE `" + tableName + "` SET `name` = ?, `last_seen` = CURRENT_TIMESTAMP WHERE `id` = ?")) {
            update.setString(1, name);
            update.setLong(2, playerId);
            update.executeUpdate();
        }
    }

    private void updateLastSeen(final Connection connection, final long playerId) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE `" + tableName + "` SET `last_seen` = CURRENT_TIMESTAMP WHERE `id` = ?")) {
            update.setLong(1, playerId);
            update.executeUpdate();
        }
    }
}
