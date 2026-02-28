package com.jannik_kuehn.common.storage.database;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.jannik_kuehn.common.config.Configuration;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Factory for creating configured {@link HikariDataSource} instances.
 */
final class HikariDataSourceFactory {

    /**
     * The data path.
     */
    private static final String DATA_PATH = "data";

    /**
     * The pool path.
     */
    private static final String POOL_PATH = DATA_PATH + ".poolSettings";

    private HikariDataSourceFactory() {
    }

    /**
     * Creates a HikariCP data source with optional credentials and tuning settings.
     *
     * @param config   the configuration
     * @param jdbcUrl  the JDBC URL to connect to
     * @param poolName the pool name for diagnostics
     * @return the configured data source
     */
    /* default */
    static HikariDataSource create(final Configuration config,
                                   final String jdbcUrl,
                                   final String poolName) {
        final HikariConfig databaseConfig = new HikariConfig();
        databaseConfig.setJdbcUrl(jdbcUrl);
        final String username = config.getString(DATA_PATH + ".user");
        if (username != null) {
            databaseConfig.setUsername(username);
        }
        final String password = config.getString(DATA_PATH + ".password");
        if (password != null) {
            databaseConfig.setPassword(password);
        }

        databaseConfig.setPoolName(poolName);

        final int maximumPoolSize = config.getInt(POOL_PATH + ".maximumPoolSize");
        databaseConfig.setMaximumPoolSize(maximumPoolSize);
        final int minimumIdle = config.getInt(POOL_PATH + ".minimumIdle");
        databaseConfig.setMinimumIdle(minimumIdle);
        final int maxLifetime = config.getInt(POOL_PATH + ".maximumLifetime");
        databaseConfig.setMaxLifetime(maxLifetime);
        final int keepAliveTime = config.getInt(POOL_PATH + ".keepAliveTime");
        databaseConfig.setKeepaliveTime(keepAliveTime);
        final int connectionTimeout = config.getInt(POOL_PATH + ".connectionTimeout");
        databaseConfig.setConnectionTimeout(connectionTimeout);

        final ThreadFactoryBuilder builder = new ThreadFactoryBuilder();
        builder.setNameFormat("HikariThread-%d");
        databaseConfig.setThreadFactory(builder.build());

        return new HikariDataSource(databaseConfig);
    }
}
