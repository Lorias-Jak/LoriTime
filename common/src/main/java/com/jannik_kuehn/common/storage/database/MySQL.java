package com.jannik_kuehn.common.storage.database;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.logger.LoriTimeLogger;
import com.jannik_kuehn.common.config.Configuration;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;

public class MySQL implements Closeable, AutoCloseable {

    private static boolean loadedJDBCDriver = true;

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (final ClassNotFoundException e) {
            loadedJDBCDriver = false;
        }
    }

    private final String mySqlHost;

    private final Integer mySqlPort;

    private final String mySqlDatabase;

    private final String mySqlUser;

    private final String mySqlPassword;

    private final String tablePrefix;

    private final LoriTimeLogger log;

    private HikariDataSource hikari;

    /**
     * @param config The {@link Configuration} for the connection
     */
    public MySQL(final Configuration config, final LoriTimePlugin loriTimePlugin) {
        this.log = loriTimePlugin.getLoggerFactory().create(MySQL.class);

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

    }

    public boolean isClosed() {
        return hikari == null || hikari.isClosed();
    }

    public void open() {
        if (!loadedJDBCDriver) {
            log.error("JDBC Driver was not loaded!");
            return;
        }
        if (isClosed()) {
            log.info("Connecting to (" + mySqlHost + ", " + mySqlPort + " ," + mySqlDatabase + ")...");
            final HikariConfig databaseConfig = getHikariConfig();

            try {
                hikari = new HikariDataSource(databaseConfig);
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

    private HikariConfig getHikariConfig() {
        final HikariConfig databaseConfig = new HikariConfig();
        databaseConfig.setJdbcUrl("jdbc:mysql://" + mySqlHost + ":" + mySqlPort + "/" + mySqlDatabase);
        databaseConfig.setUsername(mySqlUser);
        databaseConfig.setPassword(mySqlPassword);
        databaseConfig.setPoolName("LoriTime-Databasepool");

        final ThreadFactoryBuilder builder = new ThreadFactoryBuilder();
        builder.setNameFormat("HikariThread-%d");
        databaseConfig.setThreadFactory(builder.build());
        return databaseConfig;
    }

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

    public String getTablePrefix() {
        return tablePrefix;
    }
}
