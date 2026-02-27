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
    private boolean isOpen;

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
        final String lower = uncheckedTablePrefix.toLowerCase(Locale.ROOT);
        if (lower.contains("select")
                || lower.contains("insert")
                || lower.contains("drop")
                || lower.contains("create")) {
            log.error("Unsafe database table name detected! Going back to default.");
            uncheckedTablePrefix = "loritime";
        }

        this.tablePrefix = uncheckedTablePrefix;
        this.databasePath = new File(dataFolder, DEFAULT_FILENAME).getAbsolutePath();
    }

    @Override
    public void open() {
        if (isOpen) {
            log.error("The SQLite connection is already open!");
            return;
        }

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (final ClassNotFoundException e) {
            log.error("SQLite JDBC Driver was not loaded!", e);
            return;
        }

        final File databaseFile = new File(databasePath);
        final File parent = databaseFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            log.error("Could not create SQLite database directory: " + parent.getAbsolutePath());
            return;
        }

        try (Connection ignored = DriverManager.getConnection("jdbc:sqlite:" + databasePath)) {
            this.isOpen = true;
            log.info("Connected to SQLite database at " + databasePath);
        } catch (final SQLException ex) {
            log.error("Could not connect to the SQLite database!", ex);
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (!isOpen) {
            throw new SQLException("SQLite database provider is not initialized.");
        }
        return DriverManager.getConnection("jdbc:sqlite:" + databasePath);
    }

    @Override
    public boolean isClosed() {
        return !isOpen;
    }

    @Override
    public void close() {
        if (!isOpen) {
            log.error("Could not disconnect from the SQLite database, as it already was closed.");
            return;
        }
        this.isOpen = false;
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
