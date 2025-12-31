package com.jannik_kuehn.common.storage.database;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.logger.LoriTimeLogger;
import com.jannik_kuehn.common.config.Configuration;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;

final class SqliteDatabase implements SqlConnectionProvider {

    private static final String DEFAULT_FILENAME = "loritime.db";

    private final SqlDialect dialect;
    private final String tablePrefix;
    private final LoriTimeLogger log;
    private final String databasePath;

    private HikariDataSource hikari;

    SqliteDatabase(final Configuration config, final LoriTimePlugin loriTimePlugin, final File dataFolder) {
        this.log = loriTimePlugin.getLoggerFactory().create(SqliteDatabase.class);
        this.dialect = new SqliteDialect();

        String uncheckedTablePrefix = config.getString("sqlite.tablePrefix", "lori_time");
        if (uncheckedTablePrefix.toLowerCase(Locale.ROOT).contains("select")
                || uncheckedTablePrefix.toLowerCase(Locale.ROOT).contains("insert")
                || uncheckedTablePrefix.toLowerCase(Locale.ROOT).contains("drop")
                || uncheckedTablePrefix.toLowerCase(Locale.ROOT).contains("create")) {
            log.error("Unsafe database table name detected! Going back to default.");
            uncheckedTablePrefix = "lori_time";
        }
        this.tablePrefix = uncheckedTablePrefix;

        final String configuredPath = config.getString("sqlite.file", new File(dataFolder, DEFAULT_FILENAME).getAbsolutePath());
        this.databasePath = configuredPath;
    }

    @Override
    public void open() {
        if (hikari != null && !hikari.isClosed()) {
            log.error("The SQLite connection is already open!");
            return;
        }
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (final ClassNotFoundException e) {
            log.error("SQLite JDBC Driver was not loaded!", e);
            return;
        }

        final HikariConfig databaseConfig = getHikariConfig();
        try {
            hikari = new HikariDataSource(databaseConfig);
        } catch (final Exception e) {
            log.error("Failed to open SQLite database", e);
            return;
        }

        if (!hikari.isClosed()) {
            log.info("Connected to SQLite database at " + databasePath);
            return;
        }
        log.error("Could not connect to the SQLite database!");
    }

    private HikariConfig getHikariConfig() {
        final HikariConfig databaseConfig = new HikariConfig();
        databaseConfig.setJdbcUrl("jdbc:sqlite:" + databasePath);
        databaseConfig.setPoolName("LoriTime-SqlitePool");
        databaseConfig.setConnectionTestQuery("SELECT 1");
        databaseConfig.setConnectionInitSql("PRAGMA foreign_keys=ON");

        final ThreadFactoryBuilder builder = new ThreadFactoryBuilder();
        builder.setNameFormat("HikariThread-%d");
        databaseConfig.setThreadFactory(builder.build());
        databaseConfig.setMaximumPoolSize(4);
        return databaseConfig;
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (hikari == null) {
            throw new SQLException("HikariDataSource is not initialized.");
        }
        return hikari.getConnection();
    }

    @Override
    public boolean isClosed() {
        return hikari == null || hikari.isClosed();
    }

    @Override
    public void close() {
        if (hikari != null) {
            log.info("Closing SQLite connection ...");
            hikari.close();
            hikari = null;
            log.info("Successfully closed the connection to SQLite!");
            return;
        }
        log.error("Could not disconnect from the SQLite database!");
    }

    @Override
    public String getTablePrefix() {
        return tablePrefix;
    }

    @Override
    public SqlDialect getDialect() {
        return dialect;
    }
}
