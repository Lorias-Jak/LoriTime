package com.jannik_kuehn.common.storage.database;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.SQLException;

interface SqlConnectionProvider extends Closeable {

    void open();

    Connection getConnection() throws SQLException;

    boolean isClosed();

    String getTablePrefix();

    SqlDialect getDialect();
}
