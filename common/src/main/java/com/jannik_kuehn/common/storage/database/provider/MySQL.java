package com.jannik_kuehn.common.storage.database.provider;

import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import com.jannik_kuehn.common.config.Configuration;
import com.jannik_kuehn.common.storage.database.DatabaseDialect;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;

/**
 * Provides a MySQL-based implementation of the BrotkrumenConnectionProvider interface for managing database connections.
 * Responsible for opening, closing, and retrieving database connections using HikariCP as the connection pooling library.
 * This class supports multiple database engines (e.g., MySQL, MariaDB) and allows for configurable database connection
 * settings through a provided configuration section.
 */
public class MySQL implements LoriTimeConnectionProvider {

    /**
     * The configuration section containing the database settings.
     */
    private final Configuration config;

    /**
     * The logger instance used for logging operations.
     */
    private final WrappedLogger log;

    /**
     * The database dialect used for connecting to the database.
     */
    private final DatabaseDialect dialect;

    /**
     * The fully qualified class name of the JDBC driver used for connecting to the database.
     */
    private final String driverClassName;

    /**
     * The HikariCP data source used for managing database connections.
     */
    private HikariDataSource hikari;

    /**
     * Constructs a new MySQL database connection provider.
     *
     * @param config          the configuration section containing the database settings
     * @param log             the logger instance used for logging operations
     * @param dialect         the database engine (e.g., MYSQL, MARIADB)
     * @param driverClassName the fully qualified class name of the JDBC driver
     */
    public MySQL(final Configuration config, final WrappedLogger log, final DatabaseDialect dialect, final String driverClassName) {
        this.config = config;
        this.log = log;
        this.dialect = dialect;
        this.driverClassName = driverClassName;
    }

    @Override
    public void open() {
        try {
            Class.forName(driverClassName);
        } catch (final ClassNotFoundException e) {
            log.error("JDBC Driver was not loaded: " + driverClassName, e);
            return;
        }
        if (isClosed()) {
            log.info("Connecting to database...");
            final String jdbcUrl = "jdbc:" + dialect.name().toLowerCase(Locale.ROOT) + "://";
            this.hikari = HikariDataSourceFactory.create(log, config, jdbcUrl);

            if (hikari == null) {
                log.error("Could not connect to the database server!");
                return;
            }
            if (!hikari.isClosed()) {
                log.info("Successfully connected to the server!");
                return;
            }
            log.error("Could not connect to the database server!");
            return;
        }
        log.error("The connection is already open!");
    }

    @Override
    public Connection getConnection() {
        final Connection con;
        try {
            con = hikari.getConnection();
        } catch (final SQLException e) {
            log.error("Failed to get database connection", e);
            return null;
        }
        return con;
    }

    @Override
    public boolean isClosed() {
        return hikari == null || hikari.isClosed();
    }

    @Override
    public void close() {
        if (hikari != null) {
            log.info("Closing database connection");
            hikari.close();
            hikari = null;
            log.info("Successfully closed the database connection");
            return;
        }
        log.error("Could not disconnect from database, as it was already closed!");
    }
}
