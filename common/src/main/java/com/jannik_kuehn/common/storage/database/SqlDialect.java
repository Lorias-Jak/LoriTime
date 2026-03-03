package com.jannik_kuehn.common.storage.database;

/**
 * Provides SQL fragments for a specific database dialect.
 */
@FunctionalInterface
public interface SqlDialect {

    /**
     * Returns the SQL expression to compute duration in seconds.
     *
     * @param joinColumn  the join timestamp column
     * @param leaveColumn the leave timestamp column
     * @return the SQL expression
     */
    String durationSecondsExpression(String joinColumn, String leaveColumn);
}
