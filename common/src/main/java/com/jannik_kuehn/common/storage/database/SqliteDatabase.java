package com.jannik_kuehn.common.storage.database;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.logger.LoriTimeLogger;
import com.jannik_kuehn.common.config.Configuration;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Locale;

/**
 * SQLite connection provider backed by direct JDBC connections.
 */
final class SqliteDatabase implements SqlConnectionProvider {

    /**
     * Default database filename.
     */
    private static final String DEFAULT_FILENAME = "loritime.db";

    /**
     * The {@link SqlDialect} instance.
     */
    private final SqlDialect dialect;

    /**
     * The table prefix
     */
    private final String tablePrefix;

    /**
     * The {@link LoriTimeLogger} instance.
     */
    private final LoriTimeLogger log;

    /**
     * The SQLite database path.
     */
    private final String databasePath;

    /**
     * Tracks whether the provider is currently open.
     */
    private boolean open;

    /**
     * Creates an SQLite provider from configuration.
     *
     * @param config         the configuration to read connection settings from
     * @param loriTimePlugin the plugin instance for logging
     * @param dataFolder     the plugin data folder for the default DB path
     */
    /* default */
    SqliteDatabase(final Configuration config, final LoriTimePlugin loriTimePlugin, final File dataFolder) {
        this.log = loriTimePlugin.getLoggerFactory().create(SqliteDatabase.class);
        this.dialect = DatabaseDialect.SQLITE;

        String uncheckedTablePrefix = config.getString("sqlite.tablePrefix", "lori_time");
        if (uncheckedTablePrefix.toLowerCase(Locale.ROOT).contains("select")
                || uncheckedTablePrefix.toLowerCase(Locale.ROOT).contains("insert")
                || uncheckedTablePrefix.toLowerCase(Locale.ROOT).contains("drop")
                || uncheckedTablePrefix.toLowerCase(Locale.ROOT).contains("create")) {
            log.error("Unsafe database table name detected! Going back to default.");
            uncheckedTablePrefix = "loritime";
        }
        this.tablePrefix = uncheckedTablePrefix;

        this.databasePath = config.getString("sqlite.file", new File(dataFolder, DEFAULT_FILENAME).getAbsolutePath());
    }

    @Override
    public void open() {
        if (open) {
            log.error("The SQLite connection is already open!");
            return;
        }
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (final ClassNotFoundException e) {
            log.error("SQLite JDBC Driver was not loaded!", e);
            return;
        }
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath)) {
            open = true;
            log.info("Connected to SQLite database at " + databasePath);
            return;
        } catch (final SQLException ex) {
            log.error("Could not connect to the SQLite database!", ex);
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (!open) {
            throw new SQLException("SQLite database provider is not initialized.");
        }
        return DriverManager.getConnection("jdbc:sqlite:" + databasePath);
    }

    @Override
    public boolean isClosed() {
        return !open;
    }

    @Override
    public void close() {
        if (open) {
            log.info("Closing SQLite connection ...");
            open = false;
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
