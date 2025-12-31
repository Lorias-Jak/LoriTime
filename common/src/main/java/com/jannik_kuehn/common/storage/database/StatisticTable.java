package com.jannik_kuehn.common.storage.database;

final class StatisticTable {

    private final String tableName;

    StatisticTable(final String tableName) {
        this.tableName = tableName;
    }

    String createTableSql() {
        return "CREATE TABLE IF NOT EXISTS `" + tableName + "` ("
                + "`id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                + "`statistic_name` VARCHAR(64) NOT NULL,"
                + "`calculation_time` TIMESTAMP NOT NULL,"
                + "`result` BIGINT NOT NULL,"
                + "UNIQUE KEY `uk_statistic` (`statistic_name`, `calculation_time`),"
                + "INDEX `idx_statistic_name` (`statistic_name`),"
                + "INDEX `idx_statistic_time` (`calculation_time`)"
                + ") ENGINE InnoDB";
    }
}
