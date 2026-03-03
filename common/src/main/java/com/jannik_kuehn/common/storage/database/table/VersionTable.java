package com.jannik_kuehn.common.storage.database.table;

import com.jannik_kuehn.common.storage.database.SqlDialect;

/**
 * Table helper for database versioning
 */
public class VersionTable {
    /**
     * The table name.
     */
    private final String tableName;

    /**
     * The {@link SqlDialect} instance.
     */
    private final SqlDialect dialect;

    /**
     * Constructor
     *
     * @param tableName the table name
     * @param dialect   the {@link SqlDialect}
     */
    /* default */
    public VersionTable(final String tableName, final SqlDialect dialect) {
        this.tableName = tableName;
        this.dialect = dialect;
    }

    /**
     * Creation string of the sql table for the chosen dialect
     *
     * @return the DDL statement
     */
    /* default */
    public String createTableSql() {
        return dialect.createVersionTable(tableName);
    }

    /**
     * Retrieves the name of the database table associated with this instance.
     *
     * @return the name of the table as a {@link String}
     */
    /* default */
    public String getTableName() {
        return tableName;
    }
}
