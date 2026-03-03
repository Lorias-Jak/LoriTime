package com.jannik_kuehn.common.storage.database;

import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import com.github.roleplaycauldron.spellbook.database.updater.DatabaseUpdater;
import com.github.roleplaycauldron.spellbook.database.updater.DatabaseVersion;
import com.github.roleplaycauldron.spellbook.database.updater.DefaultVersionRepository;
import com.jannik_kuehn.common.config.Configuration;
import com.jannik_kuehn.common.exception.StorageException;
import com.jannik_kuehn.common.storage.database.migration.MariaDBMigration;
import com.jannik_kuehn.common.storage.database.migration.MySQLMigration;
import com.jannik_kuehn.common.storage.database.migration.SQLiteMigration;
import com.jannik_kuehn.common.storage.database.provider.LoriTimeConnectionProvider;
import com.jannik_kuehn.common.storage.database.provider.MySQL;
import com.jannik_kuehn.common.storage.database.provider.SQLite;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Database-backed storage implementation for player names and time tracking.
 */
public class DatabaseStorage {

    /**
     * The logger factory used for creating loggers.
     */
    private final LoggerFactory loggerFactory;

    /**
     * The logger instance used for logging messages.
     */
    private final WrappedLogger log;

    /**
     * The configuration instance used for accessing plugin settings.
     */
    private final Configuration config;

    /**
     * The table prefix used for database table naming within the storage system.
     */
    private final String tablePrefix;

    /**
     * The data folder of the plugin.
     */
    private final File dataFolder;

    /**
     * The connection provider used for database operations.
     */
    private LoriTimeConnectionProvider provider;

    /**
     * The database dialect used for database operations.
     */
    private DatabaseDialect dialect;

    /**
     * Creates a new instance of {@link DatabaseStorage} with the provided configuration and logger factory.
     *
     * @param loggerFactory the {@link LoggerFactory} instance
     * @param config        the {@link Configuration} instance
     * @param dataFolder    the data folder of the plugin
     */
    public DatabaseStorage(final LoggerFactory loggerFactory, final Configuration config, final File dataFolder) {
        this.loggerFactory = loggerFactory;
        this.log = loggerFactory.create(DatabaseStorage.class);
        this.config = config;
        this.tablePrefix = config.getString("data.tablePrefix", "loritime");
        this.dataFolder = dataFolder;
    }

    /**
     * Initializes the storage system by opening the database connection and performing necessary
     * setup operations such as creating tables and applying database updates. If the connection
     * provider is already open, a log message will indicate that the database is already connected.
     *
     * @throws StorageException if an error occurs during the initialization process
     */
    public void initialize() throws StorageException {
        final String engineString = config.getString("data.storageMethod");
        if (engineString == null || engineString.isBlank()) {
            throw new IllegalArgumentException("Database engine configuration is missing");
        }
        if (provider == null || provider.isClosed()) {
            dialect = DatabaseDialect.getEngineByName(engineString);
            provider = initializeProvider();
            provider.open();

            final DatabaseUpdater updater = DatabaseUpdater.builder()
                    .logger(loggerFactory.create(DatabaseUpdater.class))
                    .connectionProvider(provider)
                    .versionRepository(new DefaultVersionRepository(getDatabaseMigrationVersionList(dialect)))
                    .versionTable(tablePrefix + "_version",
                            "SELECT MAX(version_no) AS latest_version FROM `" + tablePrefix + "_version`;",
                            "INSERT INTO `" + tablePrefix + "_version` (`version_no`) VALUES (?);")
                    .build();

            updater.firstStartup();
            return;
        }

        log.error("Database is already connected!");
    }

    private LoriTimeConnectionProvider initializeProvider() throws StorageException {
        LoriTimeConnectionProvider provider = null;
        switch (dialect) {
            case MYSQL ->
                    provider = new MySQL(config, loggerFactory.create(MySQL.class), dialect, "com.mysql.cj.jdbc.Driver");
            case MARIADB ->
                    provider = new MySQL(config, loggerFactory.create(MySQL.class), dialect, "org.mariadb.jdbc.Driver");
            case SQLITE -> provider = new SQLite(loggerFactory.create(SQLite.class), dataFolder.getPath());
        }

        if (provider == null) {
            throw new StorageException("Unknown database dialect");
        }
        return provider;
    }

    /**
     * Shuts down the storage by closing the connection provider if it is open.
     * <p>
     * This method checks whether the connection provider is already closed. If the provider
     * is not null and not closed, it attempts to close the provider. If an {@link IOException}
     * occurs during this process, it wraps the exception in a {@link StorageException} and throws it.
     *
     * @throws StorageException if an I/O error occurs while closing the provider
     */
    public void shutdown() throws StorageException {
        if (provider == null || provider.isClosed()) {
            return;
        }
        try {
            provider.close();
        } catch (final IOException e) {
            throw new StorageException("Failed to close database connection", e);
        }
    }

    /**
     * Retrieves the current connection provider for storage operations.
     *
     * @return the {@link LoriTimeConnectionProvider} instance used for database connections
     */
    public LoriTimeConnectionProvider getProvider() {
        return provider;
    }

    /**
     * Retrieves the table prefix used for database table naming within the storage system.
     *
     * @return the table prefix as a string
     */
    public String getTablePrefix() {
        return tablePrefix;
    }

    public DatabaseDialect getDialect() {
        return dialect;
    }

    private List<DatabaseVersion> getDatabaseMigrationVersionList(final DatabaseDialect dialect) {
        return switch (dialect) {
            case MYSQL -> MySQLMigration.build(tablePrefix);
            case MARIADB -> MariaDBMigration.build(tablePrefix);
            case SQLITE -> SQLiteMigration.build(tablePrefix);
        };
    }
}
