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
public class MySQL implements SqlConnectionProvider {

    /**
     * Represents the hostname.
     */
    private final String mySqlHost;

    /**
     * Represents the port.
     */
    private final Integer mySqlPort;

    /**
     * Represents the name.
     */
    private final String mySqlDatabase;

    /**
     * Represents the username.
     */
    private final String mySqlUser;

    /**
     * Represents the password.
     */
    private final String mySqlPassword;

    /**
     * The table prefix.
     */
    private final String tablePrefix;

    /**
     * The SQL dialect.
     */
    private final SqlDialect dialect;

    /**
     * The JDBC driver.
     */
    private final String driverClassName;

    /**
     * The JDBC scheme.
     */
    private final String jdbcScheme;

    /**
     * The {@link LoriTimeLogger} instance.
     */
    private final LoriTimeLogger log;

    /**
     * The HikariCP connection pool.
     */
    private HikariDataSource hikari;

    /**
     * Creates a MySQL/MariaDB provider from configuration.
     *
     * @param config         the configuration to read connection settings from
     * @param loriTimePlugin the plugin instance for logging
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
     * Checks if the current MySQL connection pool is closed.
     * <p>
     * This method evaluates the state of the Hikari connection pool
     * and determines if it is either uninitialized or closed.
     *
     * @return true if the Hikari connection pool is not initialized
     * or has been closed; false otherwise
     */
    @Override
    public boolean isClosed() {
        return hikari == null || hikari.isClosed();
    }

    /**
     * Opens a connection to the MySQL database if it is not already open.
     * <p>
     * The method attempts to load the JDBC driver specified by `driverClassName`.
     * If the driver cannot be loaded, an error is logged and the method returns.
     * <p>
     * If the connection is closed, it initiates a connection to the database
     * by creating a Hikari connection pool using `HikariDataSourceFactory`.
     * <p>
     * Logs appropriate information about the connection status:
     * - Logs an info message if the connection is successfully established.
     * - Logs an error if the connection could not be established or if it is
     * already open.
     */
    @Override
    public void open() {
        try {
            Class.forName(driverClassName);
        } catch (final ClassNotFoundException e) {
            log.error("JDBC Driver was not loaded: " + driverClassName, e);
            return;
        }
        if (isClosed()) {
            log.info("Connecting to (" + mySqlHost + ", " + mySqlPort + " ," + mySqlDatabase + ")...");
            hikari = HikariDataSourceFactory.create(
                    jdbcScheme + mySqlHost + ":" + mySqlPort + "/" + mySqlDatabase,
                    "LoriTime-Databasepool",
                    mySqlUser,
                    mySqlPassword,
                    null,
                    null,
                    null);

            if (!hikari.isClosed()) {
                log.info("Successfully connected to the MySQL-Server!");
                return;
            }
            log.error("Could not connect to the MySQL-Server!");
            return;
        }
        log.error("The MySQL connection is already open!");
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (hikari == null) {
            throw new SQLException("HikariDataSource is not initialized.");
        }
        return hikari.getConnection();
    }

    @Override
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

    @Override
    public String getTablePrefix() {
        return tablePrefix;
    }

    @Override
    public SqlDialect getDialect() {
        return dialect;
    }
}
