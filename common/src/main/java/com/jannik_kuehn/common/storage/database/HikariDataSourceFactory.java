package com.jannik_kuehn.common.storage.database;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Factory for creating configured {@link HikariDataSource} instances.
 */
final class HikariDataSourceFactory {

    private HikariDataSourceFactory() {
    }

    /**
     * Creates a HikariCP data source with optional credentials and tuning settings.
     *
     * @param jdbcUrl             the JDBC URL to connect to
     * @param poolName            the pool name for diagnostics
     * @param username            optional username
     * @param password            optional password
     * @param connectionTestQuery optional connection test query
     * @param connectionInitSql   optional initialization SQL
     * @param maximumPoolSize     optional maximum pool size
     * @return the configured data source
     */
    /* default */
    static HikariDataSource create(final String jdbcUrl,
                                   final String poolName,
                                   final String username,
                                   final String password,
                                   final String connectionTestQuery,
                                   final String connectionInitSql,
                                   final Integer maximumPoolSize) {
        final HikariConfig databaseConfig = new HikariConfig();
        databaseConfig.setJdbcUrl(jdbcUrl);
        if (username != null) {
            databaseConfig.setUsername(username);
        }
        if (password != null) {
            databaseConfig.setPassword(password);
        }
        databaseConfig.setPoolName(poolName);
        if (connectionTestQuery != null) {
            databaseConfig.setConnectionTestQuery(connectionTestQuery);
        }
        if (connectionInitSql != null) {
            databaseConfig.setConnectionInitSql(connectionInitSql);
        }
        if (maximumPoolSize != null) {
            databaseConfig.setMaximumPoolSize(maximumPoolSize);
        }

        final ThreadFactoryBuilder builder = new ThreadFactoryBuilder();
        builder.setNameFormat("HikariThread-%d");
        databaseConfig.setThreadFactory(builder.build());

        return new HikariDataSource(databaseConfig);
    }
}
