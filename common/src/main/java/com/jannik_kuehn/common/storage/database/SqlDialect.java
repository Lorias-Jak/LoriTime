package com.jannik_kuehn.common.storage.database;

interface SqlDialect {

    String createPlayerTable(String tableName);

    String createServerTable(String tableName);

    String createWorldTable(String tableName, String serverTableName);

    String createTimeTable(String tableName, String playerTableName, String worldTableName);

    String createStatisticTable(String tableName);

    String durationSecondsExpression(String joinColumn, String leaveColumn);
}
