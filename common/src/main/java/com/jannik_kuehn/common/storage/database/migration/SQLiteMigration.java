package com.jannik_kuehn.common.storage.database.migration;

import com.github.roleplaycauldron.spellbook.database.updater.DatabaseVersion;
import com.github.roleplaycauldron.spellbook.database.updater.builder.VersionListBuilder;

import java.util.List;

/**
 * The SQLiteMigration class is responsible for generating a set of database migration
 * queries tailored for an SQLite database. It creates necessary tables, indexes, and
 * relationships required to manage a graph structure, including nodes, edges, and their
 * associations. The class utilizes a VersionListBuilder to define versioned queries that
 * are executed during the database initialization or upgrade process.
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class SQLiteMigration {

    private SQLiteMigration() {

    }

    /**
     * Constructs a list of {@link DatabaseVersion} objects by defining the database schema
     * for a SQLite-based migration system. The method creates necessary tables, indices,
     * and versioning information based on the provided table prefix.
     *
     * @param tablePrefix the prefix to be applied to the names of all database tables and indices.
     *                    This allows multiple sets of tables to coexist within the same database.
     * @return a list of {@link DatabaseVersion} objects representing the defined schema and
     * migration versions.
     */
    public static List<DatabaseVersion> build(final String tablePrefix) {
        final VersionListBuilder builder = new VersionListBuilder();
        addMigrationOne(builder, tablePrefix);
        addMigrationTwo(builder, tablePrefix);

        return builder.finish();
    }

    private static void addMigrationOne(final VersionListBuilder builder, final String tablePrefix) {
        builder.version(1)
                .addUnconditionalQuery(
                        "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "` ("
                                + "`id` INTEGER PRIMARY KEY AUTOINCREMENT,"
                                + "`uuid` BLOB NOT NULL UNIQUE,"
                                + "`name` TEXT UNIQUE,"
                                + "`time` INTEGER NOT NULL DEFAULT 0"
                                + ")"
                )
                .finishVersion();
    }

    private static void addMigrationTwo(final VersionListBuilder builder, final String tablePrefix) {
        builder.version(2)
                .addFirstStartupQuery(
                        "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "_player` ("
                                + "`id` INTEGER PRIMARY KEY AUTOINCREMENT,"
                                + "`uuid` BLOB NOT NULL UNIQUE,"
                                + "`name` TEXT UNIQUE,"
                                + "`last_seen` TEXT NULL"
                                + ")"
                )
                .addFirstStartupQuery(
                        "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "_server` ("
                                + "`id` INTEGER PRIMARY KEY AUTOINCREMENT,"
                                + "`server` TEXT NOT NULL UNIQUE"
                                + ")"
                )
                .addFirstStartupQuery(
                        "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "_world` ("
                                + "`id` INTEGER PRIMARY KEY AUTOINCREMENT,"
                                + "`server_id` INTEGER NOT NULL,"
                                + "`world` TEXT NOT NULL,"
                                + "UNIQUE(`server_id`, `world`),"
                                + "FOREIGN KEY (`server_id`) REFERENCES `" + tablePrefix + "_server`(`id`) ON DELETE CASCADE"
                                + ")"
                )
                .addFirstStartupQuery(
                        "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "_time` ("
                                + "`id` INTEGER PRIMARY KEY AUTOINCREMENT,"
                                + "`player_id` INTEGER NOT NULL,"
                                + "`world_id` INTEGER NOT NULL,"
                                + "`join_time` TEXT NOT NULL,"
                                + "`leave_time` TEXT NOT NULL,"
                                + "`reason` TEXT NOT NULL DEFAULT 'UNSPECIFIED',"
                                + "FOREIGN KEY (`player_id`) REFERENCES `" + tablePrefix + "_player`(`id`) ON DELETE CASCADE,"
                                + "FOREIGN KEY (`world_id`) REFERENCES `" + tablePrefix + "_world`(`id`) ON DELETE CASCADE"
                                + ")"
                )
                .addFirstStartupQuery(
                        "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "_version` ("
                                + "`version_no` INTEGER NOT NULL,"
                                + "`applied_at` TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP"
                                + ")"
                )
                .addFirstStartupQuery(
                        "CREATE INDEX IF NOT EXISTS `idx_" + tablePrefix + "_time_player` "
                                + "ON `" + tablePrefix + "_time` (`player_id`)"
                )
                .addFirstStartupQuery(
                        "CREATE INDEX IF NOT EXISTS `idx_" + tablePrefix + "_time_world` "
                                + "ON `" + tablePrefix + "_time` (`world_id`)"
                )
                .addFirstStartupQuery(
                        "CREATE INDEX IF NOT EXISTS `idx_" + tablePrefix + "_version_no` "
                                + "ON `" + tablePrefix + "_version` (`version_no`)"
                )
                .addFirstStartupQuery(
                        "INSERT OR IGNORE INTO `" + tablePrefix + "_server` (`server`) "
                                + "VALUES ('default')"
                )
                .addFirstStartupQuery(
                        "INSERT OR IGNORE INTO `" + tablePrefix + "_world` (`server_id`, `world`) "
                                + "SELECT s.`id`, 'global' "
                                + "FROM `" + tablePrefix + "_server` s "
                                + "WHERE s.`server` = 'default'"
                )

                .addUnconditionalQuery(
                        "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "_player` ("
                                + "`id` INTEGER PRIMARY KEY AUTOINCREMENT,"
                                + "`uuid` BLOB NOT NULL UNIQUE,"
                                + "`name` TEXT UNIQUE,"
                                + "`last_seen` TEXT NULL"
                                + ")"
                )
                .addUnconditionalQuery(
                        "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "_server` ("
                                + "`id` INTEGER PRIMARY KEY AUTOINCREMENT,"
                                + "`server` TEXT NOT NULL UNIQUE"
                                + ")"
                )
                .addUnconditionalQuery(
                        "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "_world` ("
                                + "`id` INTEGER PRIMARY KEY AUTOINCREMENT,"
                                + "`server_id` INTEGER NOT NULL,"
                                + "`world` TEXT NOT NULL,"
                                + "UNIQUE(`server_id`, `world`),"
                                + "FOREIGN KEY (`server_id`) REFERENCES `" + tablePrefix + "_server`(`id`) ON DELETE CASCADE"
                                + ")"
                )
                .addUnconditionalQuery(
                        "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "_time` ("
                                + "`id` INTEGER PRIMARY KEY AUTOINCREMENT,"
                                + "`player_id` INTEGER NOT NULL,"
                                + "`world_id` INTEGER NOT NULL,"
                                + "`join_time` TEXT NOT NULL,"
                                + "`leave_time` TEXT NOT NULL,"
                                + "`reason` TEXT NOT NULL DEFAULT 'UNSPECIFIED',"
                                + "FOREIGN KEY (`player_id`) REFERENCES `" + tablePrefix + "_player`(`id`) ON DELETE CASCADE,"
                                + "FOREIGN KEY (`world_id`) REFERENCES `" + tablePrefix + "_world`(`id`) ON DELETE CASCADE"
                                + ")"
                )
                .addUnconditionalQuery(
                        "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "_version` ("
                                + "`version_no` INTEGER NOT NULL,"
                                + "`applied_at` TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP"
                                + ")"
                )
                .addUnconditionalQuery(
                        "CREATE INDEX IF NOT EXISTS `idx_" + tablePrefix + "_time_player` "
                                + "ON `" + tablePrefix + "_time` (`player_id`)"
                )
                .addUnconditionalQuery(
                        "CREATE INDEX IF NOT EXISTS `idx_" + tablePrefix + "_time_world` "
                                + "ON `" + tablePrefix + "_time` (`world_id`)"
                )
                .addUnconditionalQuery(
                        "CREATE INDEX IF NOT EXISTS `idx_" + tablePrefix + "_version_no` "
                                + "ON `" + tablePrefix + "_version` (`version_no`)"
                )
                .addUnconditionalQuery(
                        "INSERT OR IGNORE INTO `" + tablePrefix + "_server` (`server`) "
                                + "VALUES ('default')"
                )
                .addUnconditionalQuery(
                        "INSERT OR IGNORE INTO `" + tablePrefix + "_world` (`server_id`, `world`) "
                                + "SELECT s.`id`, 'global' "
                                + "FROM `" + tablePrefix + "_server` s "
                                + "WHERE s.`server` = 'default'"
                )
                .addUnconditionalQuery(
                        "INSERT OR IGNORE INTO `" + tablePrefix + "_player` (`uuid`, `name`, `last_seen`) "
                                + "SELECT old.`uuid`, old.`name`, STRFTIME('%Y-%m-%d %H:%M:%f', 'now') "
                                + "FROM `" + tablePrefix + "` old"
                )
                .addUnconditionalQuery(
                        "INSERT INTO `" + tablePrefix + "_time` "
                                + "(`player_id`, `world_id`, `join_time`, `leave_time`, `reason`) "
                                + "SELECT "
                                + "p.`id`, "
                                + "w.`id`, "
                                + "STRFTIME('%Y-%m-%d %H:%M:%f', 'now', '-' || old.`time` || ' seconds'), "
                                + "STRFTIME('%Y-%m-%d %H:%M:%f', 'now'), "
                                + "'LEGACY_IMPORT' "
                                + "FROM `" + tablePrefix + "` old "
                                + "JOIN `" + tablePrefix + "_player` p ON p.`uuid` = old.`uuid` "
                                + "JOIN `" + tablePrefix + "_server` s ON s.`server` = 'default' "
                                + "JOIN `" + tablePrefix + "_world` w "
                                + "  ON w.`server_id` = s.`id` AND w.`world` = 'global' "
                                + "WHERE old.`time` > 0 "
                                + "AND NOT EXISTS ("
                                + "    SELECT 1 "
                                + "    FROM `" + tablePrefix + "_time` t "
                                + "    WHERE t.`player_id` = p.`id` "
                                + "      AND t.`world_id` = w.`id` "
                                + "      AND t.`reason` = 'LEGACY_IMPORT'"
                                + ")"
                )
                .addUnconditionalQuery(
                        "DROP TABLE `" + tablePrefix + "`"
                )
                .finishVersion();
    }
}
