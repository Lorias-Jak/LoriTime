package com.jannik_kuehn.common.storage.database;

final class SqliteDialect implements SqlDialect {

    @Override
    public String createPlayerTable(final String tableName) {
        return "CREATE TABLE IF NOT EXISTS `" + tableName + "` ("
                + "`id` INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "`uuid` BLOB NOT NULL UNIQUE,"
                + "`name` TEXT UNIQUE,"
                + "`last_seen` DATETIME NULL"
                + ")";
    }

    @Override
    public String createServerTable(final String tableName) {
        return "CREATE TABLE IF NOT EXISTS `" + tableName + "` ("
                + "`id` INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "`server` TEXT NOT NULL UNIQUE"
                + ")";
    }

    @Override
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
    public String durationSecondsExpression(final String joinColumn, final String leaveColumn) {
        return "strftime('%s', " + leaveColumn + ") - strftime('%s', " + joinColumn + ")";
    }
}
