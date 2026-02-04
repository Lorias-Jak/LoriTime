package com.jannik_kuehn.common.storage.database;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.logger.LoriTimeLogger;
import com.jannik_kuehn.common.config.Configuration;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;

/**
 * MySQL/MariaDB connection provider backed by HikariCP.
 */
@SuppressWarnings("PMD.CommentRequired")
public class MySQL implements SqlConnectionProvider {

    private final String mySqlHost;

    private final Integer mySqlPort;

    private final String mySqlDatabase;

    private final String mySqlUser;

    private final String mySqlPassword;

    private final String tablePrefix;

    private final SqlDialect dialect;

    private final String driverClassName;

    private final String jdbcScheme;

    private final LoriTimeLogger log;

    private HikariDataSource hikari;

    /**
     * Creates a MySQL/MariaDB provider from configuration.
     *
     * @param config          the configuration to read connection settings from
     * @param loriTimePlugin  the plugin instance for logging
     */
    public MySQL(final Configuration config, final LoriTimePlugin loriTimePlugin) {
        this.log = loriTimePlugin.getLoggerFactory().create(MySQL.class);
        this.dialect = DatabaseDialect.MYSQL;

        this.mySqlHost = config.getString("mysql.host", "localhost");
        this.mySqlPort = config.getInt("mysql.port", 3306);
        this.mySqlDatabase = config.getString("mysql.database");
        this.mySqlUser = config.getString("mysql.user");
        this.mySqlPassword = config.getString("mysql.password");
        String uncheckedTablePrefix = config.getString("mysql.tablePrefix", "lori_time");
        if (uncheckedTablePrefix.toLowerCase(Locale.ROOT).contains("select")
                || uncheckedTablePrefix.toLowerCase(Locale.ROOT).contains("insert")
                || uncheckedTablePrefix.toLowerCase(Locale.ROOT).contains("drop")
                || uncheckedTablePrefix.toLowerCase(Locale.ROOT).contains("create")) {
            log.error("Unsafe database table name detected! Going back to default.");
            uncheckedTablePrefix = "lori_time";
        }
        this.tablePrefix = uncheckedTablePrefix;

        final String configuredDialect = config.getString("mysql.dialect", "mariadb").toLowerCase(Locale.ROOT);
        final boolean useMariaDb = "mariadb".equals(configuredDialect);
        this.driverClassName = useMariaDb ? "org.mariadb.jdbc.Driver" : "com.mysql.cj.jdbc.Driver";
        this.jdbcScheme = useMariaDb ? "jdbc:mariadb://" : "jdbc:mysql://";
    }

    /**
     * {@inheritDoc}
     */
    public boolean isClosed() {
        return hikari == null || hikari.isClosed();
    }

    /**
     * {@inheritDoc}
     */
    public void open() {
        try {
            Class.forName(driverClassName);
        } catch (final ClassNotFoundException e) {
            log.error("JDBC Driver was not loaded: " + driverClassName, e);
            return;
        }
        if (isClosed()) {
            log.info("Connecting to (" + mySqlHost + ", " + mySqlPort + " ," + mySqlDatabase + ")...");
            try {
                hikari = HikariDataSourceFactory.create(
                        jdbcScheme + mySqlHost + ":" + mySqlPort + "/" + mySqlDatabase,
                        "LoriTime-Databasepool",
                        mySqlUser,
                        mySqlPassword,
                        null,
                        null,
                        null);
            } catch (final Exception e) {
                log.error("Probably wrong login data for MySQL-Server!");
                return;
            }

            if (!hikari.isClosed()) {
                log.info("Successfully connected to the MySQL-Server!");
                return;
            }
            log.error("Could not connect to the MySQL-Server!");
            return;
        }
        log.error("The MySQL connection is already open!");
    }

    /**
     * {@inheritDoc}
     */
    public Connection getConnection() throws SQLException {
        if (hikari == null) {
            throw new SQLException("HikariDataSource is not initialized.");
        }
        return hikari.getConnection();
    }

    @Override
    /** {@inheritDoc} */
    public void close() {
        if (hikari != null) {
            log.info("Closing connection to (" + mySqlHost + ", " + mySqlPort + " ," + mySqlDatabase + ")...");
            hikari.close();
            hikari = null;
            log.info("Successfully closed the connection to the MySQL-Server!");
            return;
        }
        log.error("Could not disconnect from the MySQL-Server!");
    }

    /**
     * {@inheritDoc}
     */
    public String getTablePrefix() {
        return tablePrefix;
    }

    @Override
    /** {@inheritDoc} */
    public SqlDialect getDialect() {
        return dialect;
    }
}
