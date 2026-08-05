package com.jannik_kuehn.common.storage.database.migration;

import com.github.roleplaycauldron.spellbook.database.updater.DatabaseVersion;
import com.github.roleplaycauldron.spellbook.database.updater.builder.VersionListBuilder;

import java.util.List;

/**
 * This class provides a utility for generating MySQL database schema migrations
 * for a graph-based data model. It constructs and returns a list of database
 * version migration scripts tailored to the specified table prefix.
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class MySQLMigration {

    private MySQLMigration() {
    }

    /**
     * Builds a list of database versions with associated schema definitions and migration scripts
     * based on the provided table prefix. The generated queries include the creation of tables,
     * indexes, and foreign key constraints for graph, node, edge, and version data storage.
     *
     * @param tablePrefix a prefix to be used for the table names in the generated SQL queries,
     *                    allowing for data namespace isolation.
     * @return a list of {@code DatabaseVersion} objects that represent the database schemas
     * and migration scripts to be applied.
     */
    public static List<DatabaseVersion> build(final String tablePrefix) {
        final VersionListBuilder builder = new VersionListBuilder();
        addMigrationOne(builder, tablePrefix);
        addMigrationTwo(builder, tablePrefix);
        addMigrationThree(builder, tablePrefix);

        return builder.finish();
    }

    private static void addMigrationThree(final VersionListBuilder builder, final String tablePrefix) {
        builder.version(3)
                .addUnconditionalQuery("CREATE TABLE IF NOT EXISTS `" + tablePrefix + "_afk_period` ("
                        + "`id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                        + "`player_id` BIGINT NOT NULL,"
                        + "`world_id` BIGINT NOT NULL,"
                        + "`started_at` DATETIME(3) NOT NULL,"
                        + "`ended_at` DATETIME(3) NULL,"
                        + "`end_reason` VARCHAR(32) NULL,"
                        + "INDEX `idx_afk_player_open` (`player_id`, `ended_at`),"
                        + "INDEX `idx_afk_range` (`started_at`, `ended_at`),"
                        + "INDEX `idx_afk_world` (`world_id`),"
                        + "CONSTRAINT `fk_afk_player` FOREIGN KEY (`player_id`) REFERENCES `"
                        + tablePrefix + "_player`(`id`) ON DELETE CASCADE,"
                        + "CONSTRAINT `fk_afk_world` FOREIGN KEY (`world_id`) REFERENCES `"
                        + tablePrefix + "_world`(`id`) ON DELETE CASCADE) ENGINE InnoDB")
                .addUnconditionalQuery("CREATE INDEX `idx_time_range` ON `" + tablePrefix
                        + "_time` (`join_time`, `leave_time`)")
                .finishVersion();
    }

    private static void addMigrationOne(final VersionListBuilder builder, final String tablePrefix) {
        builder.version(1)
                .addUnconditionalQuery(
                        "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "` ("
                                + "`id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                                + "`uuid` BINARY(16) NOT NULL UNIQUE,"
                                + "`name` CHAR(16) CHARACTER SET ascii UNIQUE,"
                                + "`time` BIGINT UNSIGNED NOT NULL DEFAULT 0"
                                + ") ENGINE InnoDB"
                )
                .finishVersion();
    }

    private static void addMigrationTwo(final VersionListBuilder builder, final String tablePrefix) {
        builder.version(2)
                .addFirstStartupQuery(
                        "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "_player` ("
                                + "`id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                                + "`uuid` BINARY(16) NOT NULL UNIQUE,"
                                + "`name` VARCHAR(16) CHARACTER SET ascii UNIQUE,"
                                + "`last_seen` TIMESTAMP NULL"
                                + ") ENGINE InnoDB"
                )
                .addFirstStartupQuery(
                        "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "_server` ("
                                + "`id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                                + "`server` VARCHAR(64) NOT NULL UNIQUE"
                                + ") ENGINE InnoDB"
                )
                .addFirstStartupQuery(
                        "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "_world` ("
                                + "`id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                                + "`server_id` BIGINT NOT NULL,"
                                + "`world` VARCHAR(64) NOT NULL,"
                                + "UNIQUE KEY `uk_world` (`server_id`, `world`),"
                                + "CONSTRAINT `fk_world_server` FOREIGN KEY (`server_id`) "
                                + "REFERENCES `" + tablePrefix + "_server`(`id`) ON DELETE CASCADE"
                                + ") ENGINE InnoDB"
                )
                .addFirstStartupQuery(
                        "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "_time` ("
                                + "`id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                                + "`player_id` BIGINT NOT NULL,"
                                + "`world_id` BIGINT NOT NULL,"
                                + "`join_time` DATETIME(3) NOT NULL,"
                                + "`leave_time` DATETIME(3) NOT NULL,"
                                + "`reason` VARCHAR(32) NOT NULL DEFAULT 'UNSPECIFIED',"
                                + "INDEX `idx_time_player` (`player_id`),"
                                + "INDEX `idx_time_world` (`world_id`),"
                                + "CONSTRAINT `fk_time_player` FOREIGN KEY (`player_id`) "
                                + "REFERENCES `" + tablePrefix + "_player`(`id`) ON DELETE CASCADE,"
                                + "CONSTRAINT `fk_time_world` FOREIGN KEY (`world_id`) "
                                + "REFERENCES `" + tablePrefix + "_world`(`id`) ON DELETE CASCADE"
                                + ") ENGINE InnoDB"
                )
                .addFirstStartupQuery(
                        "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "_time_adjustment` ("
                                + "`id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                                + "`player_id` BIGINT NOT NULL,"
                                + "`scope_type` VARCHAR(16) NOT NULL DEFAULT 'GLOBAL',"
                                + "`server_id` BIGINT NULL,"
                                + "`world_id` BIGINT NULL,"
                                + "`amount_seconds` BIGINT NOT NULL,"
                                + "`reason` VARCHAR(32) NOT NULL DEFAULT 'MANUAL_ADJUSTMENT',"
                                + "`actor_uuid` BINARY(16) NULL,"
                                + "`actor_name` VARCHAR(64) CHARACTER SET utf8mb4 NOT NULL,"
                                + "`created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),"
                                + "INDEX `idx_adjustment_player` (`player_id`),"
                                + "INDEX `idx_adjustment_scope` (`scope_type`, `server_id`, `world_id`),"
                                + "INDEX `idx_adjustment_created` (`created_at`),"
                                + "CONSTRAINT `fk_adjustment_player` FOREIGN KEY (`player_id`) "
                                + "REFERENCES `" + tablePrefix + "_player`(`id`) ON DELETE CASCADE,"
                                + "CONSTRAINT `fk_adjustment_server` FOREIGN KEY (`server_id`) "
                                + "REFERENCES `" + tablePrefix + "_server`(`id`) ON DELETE CASCADE,"
                                + "CONSTRAINT `fk_adjustment_world` FOREIGN KEY (`world_id`) "
                                + "REFERENCES `" + tablePrefix + "_world`(`id`) ON DELETE CASCADE"
                                + ") ENGINE InnoDB"
                )
                .addFirstStartupQuery(
                        "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "_version` ("
                                + "`version_no` INT NOT NULL,"
                                + "`applied_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                                + "INDEX `idx_version_no` (`version_no`)"
                                + ") ENGINE=InnoDB"
                )
                .addFirstStartupQuery(
                        "INSERT IGNORE INTO `" + tablePrefix + "_server` (`server`) "
                                + "VALUES ('default')"
                )
                .addFirstStartupQuery(
                        "INSERT IGNORE INTO `" + tablePrefix + "_world` (`server_id`, `world`) "
                                + "SELECT s.`id`, 'global' "
                                + "FROM `" + tablePrefix + "_server` s "
                                + "WHERE s.`server` = 'default'"
                )
                .addUnconditionalQuery(
                        "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "_version` ("
                                + "`version_no` INT NOT NULL,"
                                + "`applied_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                                + "INDEX `idx_version_no` (`version_no`)"
                                + ") ENGINE=InnoDB"
                )
                .addUnconditionalQuery(
                        "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "_player` ("
                                + "`id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                                + "`uuid` BINARY(16) NOT NULL UNIQUE,"
                                + "`name` VARCHAR(16) CHARACTER SET ascii UNIQUE,"
                                + "`last_seen` TIMESTAMP NULL"
                                + ") ENGINE InnoDB"
                )
                .addUnconditionalQuery(
                        "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "_server` ("
                                + "`id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                                + "`server` VARCHAR(64) NOT NULL UNIQUE"
                                + ") ENGINE InnoDB"
                )
                .addUnconditionalQuery(
                        "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "_world` ("
                                + "`id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                                + "`server_id` BIGINT NOT NULL,"
                                + "`world` VARCHAR(64) NOT NULL,"
                                + "UNIQUE KEY `uk_world` (`server_id`, `world`),"
                                + "CONSTRAINT `fk_world_server` FOREIGN KEY (`server_id`) "
                                + "REFERENCES `" + tablePrefix + "_server`(`id`) ON DELETE CASCADE"
                                + ") ENGINE InnoDB"
                )
                .addUnconditionalQuery(
                        "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "_time` ("
                                + "`id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                                + "`player_id` BIGINT NOT NULL,"
                                + "`world_id` BIGINT NOT NULL,"
                                + "`join_time` DATETIME(3) NOT NULL,"
                                + "`leave_time` DATETIME(3) NOT NULL,"
                                + "`reason` VARCHAR(32) NOT NULL DEFAULT 'UNSPECIFIED',"
                                + "INDEX `idx_time_player` (`player_id`),"
                                + "INDEX `idx_time_world` (`world_id`),"
                                + "CONSTRAINT `fk_time_player` FOREIGN KEY (`player_id`) "
                                + "REFERENCES `" + tablePrefix + "_player`(`id`) ON DELETE CASCADE,"
                                + "CONSTRAINT `fk_time_world` FOREIGN KEY (`world_id`) "
                                + "REFERENCES `" + tablePrefix + "_world`(`id`) ON DELETE CASCADE"
                                + ") ENGINE InnoDB"
                )
                .addUnconditionalQuery(
                        "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "_time_adjustment` ("
                                + "`id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                                + "`player_id` BIGINT NOT NULL,"
                                + "`scope_type` VARCHAR(16) NOT NULL DEFAULT 'GLOBAL',"
                                + "`server_id` BIGINT NULL,"
                                + "`world_id` BIGINT NULL,"
                                + "`amount_seconds` BIGINT NOT NULL,"
                                + "`reason` VARCHAR(32) NOT NULL DEFAULT 'MANUAL_ADJUSTMENT',"
                                + "`actor_uuid` BINARY(16) NULL,"
                                + "`actor_name` VARCHAR(64) CHARACTER SET utf8mb4 NOT NULL,"
                                + "`created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),"
                                + "INDEX `idx_adjustment_player` (`player_id`),"
                                + "INDEX `idx_adjustment_scope` (`scope_type`, `server_id`, `world_id`),"
                                + "INDEX `idx_adjustment_created` (`created_at`),"
                                + "CONSTRAINT `fk_adjustment_player` FOREIGN KEY (`player_id`) "
                                + "REFERENCES `" + tablePrefix + "_player`(`id`) ON DELETE CASCADE,"
                                + "CONSTRAINT `fk_adjustment_server` FOREIGN KEY (`server_id`) "
                                + "REFERENCES `" + tablePrefix + "_server`(`id`) ON DELETE CASCADE,"
                                + "CONSTRAINT `fk_adjustment_world` FOREIGN KEY (`world_id`) "
                                + "REFERENCES `" + tablePrefix + "_world`(`id`) ON DELETE CASCADE"
                                + ") ENGINE InnoDB"
                )
                .addUnconditionalQuery(
                        "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "_version` ("
                                + "`version_no` INT NOT NULL,"
                                + "`applied_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                                + "INDEX `idx_version_no` (`version_no`)"
                                + ") ENGINE=InnoDB"
                )
                .addUnconditionalQuery(
                        "INSERT IGNORE INTO `" + tablePrefix + "_server` (`server`) "
                                + "VALUES ('default')"
                )
                .addUnconditionalQuery(
                        "INSERT IGNORE INTO `" + tablePrefix + "_world` (`server_id`, `world`) "
                                + "SELECT s.`id`, 'global' "
                                + "FROM `" + tablePrefix + "_server` s "
                                + "WHERE s.`server` = 'default'"
                )
                .addUnconditionalQuery(
                        "INSERT INTO `" + tablePrefix + "_player` (`uuid`, `name`, `last_seen`) "
                                + "SELECT old.`uuid`, old.`name`, NOW(3) "
                                + "FROM `" + tablePrefix + "` old "
                                + "ON DUPLICATE KEY UPDATE "
                                + "`name` = VALUES(`name`), "
                                + "`last_seen` = VALUES(`last_seen`)"
                )
                .addUnconditionalQuery(
                        "INSERT INTO `" + tablePrefix + "_time_adjustment` "
                                + "(`player_id`, `scope_type`, `server_id`, `world_id`, "
                                + "`amount_seconds`, `reason`, `actor_uuid`, `actor_name`) "
                                + "SELECT "
                                + "p.`id`, "
                                + "'WORLD', "
                                + "NULL, "
                                + "w.`id`, "
                                + "old.`time`, "
                                + "'LEGACY_IMPORT', "
                                + "NULL, "
                                + "'MIGRATION' "
                                + "FROM `" + tablePrefix + "` old "
                                + "JOIN `" + tablePrefix + "_player` p ON p.`uuid` = old.`uuid` "
                                + "JOIN `" + tablePrefix + "_server` s ON s.`server` = 'default' "
                                + "JOIN `" + tablePrefix + "_world` w "
                                + "ON w.`server_id` = s.`id` AND w.`world` = 'global' "
                                + "WHERE old.`time` > 0 "
                                + "AND NOT EXISTS ("
                                + "SELECT 1 "
                                + "FROM `" + tablePrefix + "_time_adjustment` a "
                                + "WHERE a.`player_id` = p.`id` "
                                + "AND a.`scope_type` = 'WORLD' "
                                + "AND a.`world_id` = w.`id` "
                                + "AND a.`reason` = 'LEGACY_IMPORT'"
                                + ")"
                )
                .addUnconditionalQuery(
                        "DROP TABLE `" + tablePrefix + "`"
                )
                .finishVersion();
    }
}
