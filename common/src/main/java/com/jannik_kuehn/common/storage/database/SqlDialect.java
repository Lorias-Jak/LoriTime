package com.jannik_kuehn.common.storage.database;

/**
 * Provides SQL fragments for a specific database dialect.
 */
interface SqlDialect {

    /**
     * Returns the CREATE TABLE DDL for the player table.
     *
     * @param tableName the table name
     * @return the DDL statement
     */
    String createPlayerTable(String tableName);

    /**
     * Returns the CREATE TABLE DDL for the server table.
     *
     * @param tableName the table name
     * @return the DDL statement
     */
    String createServerTable(String tableName);

    /**
     * Returns the CREATE TABLE DDL for the world table.
     *
     * @param tableName       the table name
     * @param serverTableName the server table name
     * @return the DDL statement
     */
    String createWorldTable(String tableName, String serverTableName);

    /**
     * Returns the CREATE TABLE DDL for the timetable.
     *
     * @param tableName       the table name
     * @param playerTableName the player table name
     * @param worldTableName  the world table name
     * @return the DDL statement
     */
    String createTimeTable(String tableName, String playerTableName, String worldTableName);

    /**
     * Returns the CREATE TABLE DDL for the statistic table.
     *
     * @param tableName the table name
     * @return the DDL statement
     */
    String createStatisticTable(String tableName);

    /**
     * Returns the SQL expression to compute duration in seconds.
     *
     * @param joinColumn  the join timestamp column
     * @param leaveColumn the leave timestamp column
     * @return the SQL expression
     */
    String durationSecondsExpression(String joinColumn, String leaveColumn);
}
