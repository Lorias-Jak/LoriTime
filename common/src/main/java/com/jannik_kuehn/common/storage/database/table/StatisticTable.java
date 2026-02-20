package com.jannik_kuehn.common.storage.database.table;

import com.jannik_kuehn.common.storage.database.SqlDialect;

/**
 * Table helper for statistic entries.
 */
public final class StatisticTable {

    /**
     * The table name.
     */
    private final String tableName;

    /**
     * The {@link SqlDialect} instance.
     */
    private final SqlDialect dialect;

    /**
     * Default constructor.
     *
     * @param tableName the table name
     * @param dialect   the {@link SqlDialect} instance
     */
    /* default */
    public StatisticTable(final String tableName, final SqlDialect dialect) {
        this.tableName = tableName;
        this.dialect = dialect;
    }

    /**
     * Generates the SQL DDL statement for creating the statistic table.
     *
     * @return the SQL CREATE TABLE statement for the statistic table
     */
    /* default */
    public String createTableSql() {
        return dialect.createStatisticTable(tableName);
    }
}
