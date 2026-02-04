package com.jannik_kuehn.common.storage.database;

/**
 * Table helper for statistic entries.
 */
final class StatisticTable {

    private final String tableName;
    private final SqlDialect dialect;

    StatisticTable(final String tableName, final SqlDialect dialect) {
        this.tableName = tableName;
        this.dialect = dialect;
    }

    String createTableSql() {
        return dialect.createStatisticTable(tableName);
    }
}
