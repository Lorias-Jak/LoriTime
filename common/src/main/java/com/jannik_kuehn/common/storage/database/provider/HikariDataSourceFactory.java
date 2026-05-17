package com.jannik_kuehn.common.storage.database.provider;

import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.jannik_kuehn.common.config.ConfigSection;
import com.jannik_kuehn.common.config.Configuration;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Factory class for creating and configuring HikariDataSource instances.
 * <p>
 * This class provides a method to construct a fully configured
 * {@link HikariDataSource} object based on the given
 * configuration parameters.
 * <p>
 * The factory uses the HikariCP connection pooling library, which is
 * designed to optimize JDBC connection pool performance and management.
 */
public final class HikariDataSourceFactory {

    /**
     * Prefix for database connection configuration keys.
     */
    private static final String DATA_SECTION = "data.";

    /**
     * Prefix for Hikari connection pool configuration keys.
     */
    private static final String POOL_SECTION = DATA_SECTION + "poolSettings.";

    private HikariDataSourceFactory() {
    }

    /**
     * Creates and configures a {@link HikariDataSource} instance using the provided
     * configuration section, JDBC URL, and pool name.
     *
     * @param config       the configuration section containing necessary properties such as
     *                     user credentials, pool size, and timing configurations
     * @param startJdbcUrl the JDBC URL to connect to the database
     * @return a fully configured {@link HikariDataSource} instance
     */
    /* default */
    static HikariDataSource create(final WrappedLogger log, final Configuration config,
                                   final String startJdbcUrl) {
        final HikariConfig databaseConfig = new HikariConfig();

        final String jdbcUrl = String.format("%s%s:%s/%s",
                startJdbcUrl,
                config.getString(DATA_SECTION + "host"),
                config.getInt(DATA_SECTION + "port"),
                config.getString(DATA_SECTION + "database"));

        databaseConfig.setJdbcUrl(jdbcUrl);

        final String username = config.getString(DATA_SECTION + "user");
        if (username == null || username.isBlank()) {
            log.error("The database username is missing. Please check your config.yml.");
            return null;
        }
        databaseConfig.setUsername(username);

        final String password = config.getString(DATA_SECTION + "password");
        if (password != null) {
            databaseConfig.setPassword(password);
        }

        databaseConfig.setPoolName("LoriTime-ConnectionPool");

        final ConfigSection poolSettings = config.getSection(DATA_SECTION + "poolSettings")
                .orElse(config.getRootSection());

        final int maximumPoolSize = poolSettings.getInt("maximumPoolSize", config.getInt(POOL_SECTION + "maximumPoolSize", 10));
        databaseConfig.setMaximumPoolSize(maximumPoolSize);

        final int minimumIdle = poolSettings.getInt("minimumIdle", config.getInt(POOL_SECTION + "minimumIdle", 10));
        databaseConfig.setMinimumIdle(minimumIdle);

        final int maxLifetime = poolSettings.getInt("maximumLifetime", config.getInt(POOL_SECTION + "maximumLifetime", 1_800_000));
        databaseConfig.setMaxLifetime(maxLifetime);

        final int keepAliveTime = poolSettings.getInt("keepAliveTime", config.getInt(POOL_SECTION + "keepAliveTime", 0));
        databaseConfig.setKeepaliveTime(keepAliveTime);

        final int connectionTimeout = poolSettings.getInt("connectionTimeout", config.getInt(POOL_SECTION + "connectionTimeout", 5_000));
        databaseConfig.setConnectionTimeout(connectionTimeout);

        final ThreadFactoryBuilder builder = new ThreadFactoryBuilder();
        builder.setNameFormat("HikariThread-%d");
        databaseConfig.setThreadFactory(builder.build());

        return new HikariDataSource(databaseConfig);
    }
}
