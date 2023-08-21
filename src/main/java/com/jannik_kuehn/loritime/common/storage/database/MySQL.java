package com.jannik_kuehn.loritime.common.storage.database;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.jannik_kuehn.loritime.common.LoriTimePlugin;
import com.jannik_kuehn.loritime.common.config.Configuration;
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

    private LoriTimePlugin plugin;

    private HikariDataSource hikari;

    /**
     * @param config   The {@link Configuration} for the connection
     */
    public MySQL(final Configuration config, LoriTimePlugin plugin) {
        this.mySqlHost = config.getString("mysql.host", "localhost");
        this.mySqlPort = config.getInt("mysql.port", 3306);
        this.mySqlDatabase = config.getString("mysql.database");
        this.mySqlUser = config.getString("mysql.user");
        this.mySqlPassword = config.getString("mysql.password");
        String uncheckedTablePrefix = config.getString("mysql.tablePrefix", "lori_time");
        if (uncheckedTablePrefix.toLowerCase(Locale.ROOT).contains("select") || uncheckedTablePrefix.toLowerCase(Locale.ROOT).contains("insert") ||
                uncheckedTablePrefix.toLowerCase(Locale.ROOT).contains("drop") || uncheckedTablePrefix.toLowerCase(Locale.ROOT).contains("create")) {
            plugin.getLogger().severe("Unsafe database table name detected! Going back to default.");
            uncheckedTablePrefix = "lori_time";
        }
        this.tablePrefix = uncheckedTablePrefix;
        this.plugin = plugin;
    }

    public boolean isClosed() {
        return hikari == null || hikari.isClosed();
    }

    public boolean open() {
        if (!loadedJDBCDriver) {
            plugin.getLogger().severe("JDBC Driver was not loaded!");
            return false;
        }
        if (isClosed()) {
            plugin.getLogger().info("Connecting to (" + mySqlHost + ", " + mySqlPort + " ," + mySqlDatabase + ")...");
            final HikariConfig databaseConfig = new HikariConfig();
            databaseConfig.setJdbcUrl("jdbc:mysql://" + mySqlHost + ":" + mySqlPort + "/" + mySqlDatabase);
            databaseConfig.setUsername(mySqlUser);
            databaseConfig.setPassword(mySqlPassword);
            databaseConfig.setPoolName("LoriTime-Databasepool");

            final ThreadFactoryBuilder builder = new ThreadFactoryBuilder();
            builder.setNameFormat("HikariThread-%d");
            databaseConfig.setThreadFactory(builder.build());

            try {
                hikari = new HikariDataSource(databaseConfig);
            } catch (final Exception e) {
                plugin.getLogger().severe("Probably wrong login data for MySQL-Server!");
                return false;
            }

            if (!hikari.isClosed()) {
                plugin.getLogger().info("Successfully connected to the MySQL-Server!");
                return true;
            }
            plugin.getLogger().severe("Could not connect to the MySQL-Server!");
            return false;
        }
        plugin.getLogger().severe("The MySQL connection is already open!");
        return false;
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
            plugin.getLogger().info("Closing connection to (" + mySqlHost + ", " + mySqlPort + " ," + mySqlDatabase + ")...");
            hikari.close();
            hikari = null;
            plugin.getLogger().info("Successfully closed the connection to the MySQL-Server!");
            return;
        }
        plugin.getLogger().severe("Could not disconnect from the MySQL-Server!");
    }

    public String getTablePrefix() {
        return tablePrefix;
    }
}
