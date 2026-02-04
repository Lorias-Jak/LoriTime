package com.jannik_kuehn.common.storage.database;

/**
 * Built-in SQL dialects supported by the storage layer.
 */
enum DatabaseDialect implements SqlDialect {
    MYSQL {
        @Override
        /** {@inheritDoc} */
        public String createPlayerTable(final String tableName) {
            return "CREATE TABLE IF NOT EXISTS `" + tableName + "` ("
                    + "`id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                    + "`uuid` BINARY(16) NOT NULL UNIQUE,"
                    + "`name` VARCHAR(16) CHARACTER SET ascii UNIQUE,"
                    + "`last_seen` TIMESTAMP NULL"
                    + ") ENGINE InnoDB";
        }

        @Override
        /** {@inheritDoc} */
        public String createServerTable(final String tableName) {
            return "CREATE TABLE IF NOT EXISTS `" + tableName + "` ("
                    + "`id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                    + "`server` VARCHAR(64) NOT NULL UNIQUE"
                    + ") ENGINE InnoDB";
        }

        @Override
        /** {@inheritDoc} */
        public String createWorldTable(final String tableName, final String serverTableName) {
            return "CREATE TABLE IF NOT EXISTS `" + tableName + "` ("
                    + "`id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                    + "`server_id` BIGINT NOT NULL,"
                    + "`world` VARCHAR(64) NOT NULL,"
                    + "UNIQUE KEY `uk_world` (`server_id`, `world`),"
                    + "CONSTRAINT `fk_world_server` FOREIGN KEY (`server_id`) REFERENCES `" + serverTableName + "`(`id`) ON DELETE CASCADE"
                    + ") ENGINE InnoDB";
        }

        @Override
        /** {@inheritDoc} */
        public String createTimeTable(final String tableName, final String playerTableName, final String worldTableName) {
            return "CREATE TABLE IF NOT EXISTS `" + tableName + "` ("
                    + "`id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                    + "`player_id` BIGINT NOT NULL,"
                    + "`world_id` BIGINT NOT NULL,"
                    + "`join_time` TIMESTAMP NOT NULL,"
                    + "`leave_time` TIMESTAMP NOT NULL,"
                    + "INDEX `idx_time_player` (`player_id`),"
                    + "INDEX `idx_time_world` (`world_id`),"
                    + "CONSTRAINT `fk_time_player` FOREIGN KEY (`player_id`) REFERENCES `" + playerTableName + "`(`id`) ON DELETE CASCADE,"
                    + "CONSTRAINT `fk_time_world` FOREIGN KEY (`world_id`) REFERENCES `" + worldTableName + "`(`id`) ON DELETE CASCADE"
                    + ") ENGINE InnoDB";
        }

        @Override
        /** {@inheritDoc} */
        public String createStatisticTable(final String tableName) {
            return "CREATE TABLE IF NOT EXISTS `" + tableName + "` ("
                    + "`id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                    + "`statistic_name` VARCHAR(64) NOT NULL,"
                    + "`calculation_time` TIMESTAMP NOT NULL,"
                    + "`result` BIGINT NOT NULL,"
                    + "UNIQUE KEY `uk_statistic` (`statistic_name`, `calculation_time`)"
                    + ", INDEX `idx_statistic_name` (`statistic_name`),"
                    + " INDEX `idx_statistic_time` (`calculation_time`)"
                    + ") ENGINE InnoDB";
        }

        @Override
        /** {@inheritDoc} */
        public String durationSecondsExpression(final String joinColumn, final String leaveColumn) {
            return "TIMESTAMPDIFF(SECOND, " + joinColumn + ", " + leaveColumn + ")";
        }
    },
    SQLITE {
        @Override
        /** {@inheritDoc} */
        public String createPlayerTable(final String tableName) {
            return "CREATE TABLE IF NOT EXISTS `" + tableName + "` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "`uuid` BLOB NOT NULL UNIQUE,"
                    + "`name` TEXT UNIQUE,"
                    + "`last_seen` DATETIME NULL"
                    + ")";
        }

        @Override
        /** {@inheritDoc} */
        public String createServerTable(final String tableName) {
            return "CREATE TABLE IF NOT EXISTS `" + tableName + "` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "`server` TEXT NOT NULL UNIQUE"
                    + ")";
        }

        @Override
        /** {@inheritDoc} */
        public String createWorldTable(final String tableName, final String serverTableName) {
            return "CREATE TABLE IF NOT EXISTS `" + tableName + "` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "`server_id` INTEGER NOT NULL,"
                    + "`world` TEXT NOT NULL,"
                    + "UNIQUE (`server_id`, `world`),"
                    + "FOREIGN KEY (`server_id`) REFERENCES `" + serverTableName + "`(`id`) ON DELETE CASCADE"
                    + ")";
        }

        @Override
        /** {@inheritDoc} */
        public String createTimeTable(final String tableName, final String playerTableName, final String worldTableName) {
            return "CREATE TABLE IF NOT EXISTS `" + tableName + "` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "`player_id` INTEGER NOT NULL,"
                    + "`world_id` INTEGER NOT NULL,"
                    + "`join_time` DATETIME NOT NULL,"
                    + "`leave_time` DATETIME NOT NULL,"
                    + "FOREIGN KEY (`player_id`) REFERENCES `" + playerTableName + "`(`id`) ON DELETE CASCADE,"
                    + "FOREIGN KEY (`world_id`) REFERENCES `" + worldTableName + "`(`id`) ON DELETE CASCADE"
                    + ")";
        }

        @Override
        /** {@inheritDoc} */
        public String createStatisticTable(final String tableName) {
            return "CREATE TABLE IF NOT EXISTS `" + tableName + "` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "`statistic_name` TEXT NOT NULL,"
                    + "`calculation_time` DATETIME NOT NULL,"
                    + "`result` INTEGER NOT NULL,"
                    + "UNIQUE (`statistic_name`, `calculation_time`)"
                    + ")";
        }

        @Override
        /** {@inheritDoc} */
        public String durationSecondsExpression(final String joinColumn, final String leaveColumn) {
            return "strftime('%s', " + leaveColumn + ") - strftime('%s', " + joinColumn + ")";
        }
    };
}
