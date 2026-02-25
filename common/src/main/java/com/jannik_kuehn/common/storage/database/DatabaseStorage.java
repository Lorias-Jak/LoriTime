package com.jannik_kuehn.common.storage.database;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.logger.LoriTimeLogger;
import com.jannik_kuehn.common.api.storage.NameStorage;
import com.jannik_kuehn.common.api.storage.TimeStorage;
import com.jannik_kuehn.common.config.Configuration;
import com.jannik_kuehn.common.exception.StorageException;
import com.jannik_kuehn.common.storage.database.table.PlayerTable;
import com.jannik_kuehn.common.storage.database.table.ServerTable;
import com.jannik_kuehn.common.storage.database.table.StatisticTable;
import com.jannik_kuehn.common.storage.database.table.TimeTable;
import com.jannik_kuehn.common.storage.database.table.WorldTable;
import com.jannik_kuehn.common.utils.UuidUtil;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Database-backed storage implementation for player names and time tracking.
 */
@SuppressWarnings({"PMD.CommentRequired", "PMD.TooManyMethods", "PMD.CouplingBetweenObjects"})
public class DatabaseStorage implements NameStorage, TimeStorage {

    private static final String SQLITE_STORAGE_TYPE = "sqlite";

    private static final String MYSQL_STORAGE_TYPE = "mysql";

    private static final String MARIADB_STORAGE_TYPE = "mariadb";

    private static final String DEFAULT_SERVER_NAME = "default";

    private static final String DEFAULT_WORLD_NAME = "global";

    private final SqlConnectionProvider databaseProvider;

    private final LoriTimeLogger log;

    private final ReadWriteLock poolLock;

    private final String legacyTable;

    private final PlayerTable playerTable;

    private final ServerTable serverTable;

    private final WorldTable worldTable;

    private final TimeTable timeTable;

    private final StatisticTable statisticTable;

    private boolean initialized;

    /**
     * Creates a database storage instance and initializes the schema.
     *
     * @param config         the configuration to read database settings from
     * @param loriTimePlugin the plugin instance for logging and context
     * @param dataFolder     the plugin data folder (used for SQLite paths)
     */
    public DatabaseStorage(final Configuration config, final LoriTimePlugin loriTimePlugin, final File dataFolder) {
        this(config, loriTimePlugin, dataFolder, true);
    }

    /**
     * Creates a database storage instance.
     *
     * @param config         the configuration to read database settings from
     * @param loriTimePlugin the plugin instance for logging and context
     * @param dataFolder     the plugin data folder (used for SQLite paths)
     * @param autoInitialize if {@code true}, opens the provider and initializes schema immediately
     */
    public DatabaseStorage(final Configuration config,
                           final LoriTimePlugin loriTimePlugin,
                           final File dataFolder,
                           final boolean autoInitialize) {
        this.log = loriTimePlugin.getLoggerFactory().create(DatabaseStorage.class);
        this.databaseProvider = createProvider(config, loriTimePlugin, dataFolder, log);
        this.poolLock = new ReentrantReadWriteLock();
        this.initialized = false;

        final String tablePrefix = initializeTables(databaseProvider.getTablePrefix(), databaseProvider.getDialect());
        this.legacyTable = tablePrefix;

        if (autoInitialize) {
            initialize();
        }
    }

    /**
     * Constructor intended for injection-based usage.
     *
     * @param databaseProvider connection provider implementation
     * @param log logger to be used by this storage
     * @param autoInitialize if {@code true}, opens the provider and initializes schema immediately
     */
    public DatabaseStorage(final SqlConnectionProvider databaseProvider,
                           final LoriTimeLogger log,
                           final boolean autoInitialize) {
        this.databaseProvider = Objects.requireNonNull(databaseProvider);
        this.log = Objects.requireNonNull(log);
        this.poolLock = new ReentrantReadWriteLock();
        this.initialized = false;

        final String tablePrefix = initializeTables(databaseProvider.getTablePrefix(), databaseProvider.getDialect());
        this.legacyTable = tablePrefix;

        if (autoInitialize) {
            initialize();
        }
    }

    private String initializeTables(final String tablePrefix, final SqlDialect dialect) {
        final String playerTableName = tablePrefix + "_player";
        final String serverTableName = tablePrefix + "_server";
        final String worldTableName = tablePrefix + "_world";
        final String timeTableName = tablePrefix + "_time";
        final String statisticTableName = tablePrefix + "_statistic";

        this.playerTable = new PlayerTable(playerTableName, dialect);
        this.serverTable = new ServerTable(serverTableName, dialect);
        this.worldTable = new WorldTable(worldTableName, serverTable, dialect);
        this.timeTable = new TimeTable(timeTableName, playerTableName, worldTableName, dialect);
        this.statisticTable = new StatisticTable(statisticTableName, dialect);
        return tablePrefix;
    }

    /**
     * Opens the provider and initializes schema/migration.
     */
    public synchronized void initialize() {
        if (initialized) {
            return;
        }
        databaseProvider.open();
        if (databaseProvider.isClosed()) {
            log.error("Database provider is closed after open attempt. Initialization aborted.");
            return;
        }
        initialized = initializeSchemaAndMigrate();
    }

    /**
     * Creates a connection provider based on the configured storage type.
     *
     * @param config     the configuration to read storage settings from
     * @param plugin     the plugin instance for logging
     * @param dataFolder the plugin data folder
     * @return a provider for the selected database backend
     */
    private static SqlConnectionProvider createProvider(final Configuration config,
                                                        final LoriTimePlugin plugin,
                                                        final File dataFolder,
                                                        final LoriTimeLogger log) {
        final String storageType = config.getString("general.storage", "yml").toLowerCase(Locale.ROOT);
        return switch (storageType) {
            case SQLITE_STORAGE_TYPE -> new SqliteDatabase(config, plugin, dataFolder);
            case MARIADB_STORAGE_TYPE -> new MySQL(config, plugin, MySQL.Engine.MARIADB);
            case MYSQL_STORAGE_TYPE -> new MySQL(config, plugin, MySQL.Engine.MYSQL);
            case "yml" -> {
                log.warn("Legacy storage type 'yml' is no longer supported directly. Using SQLite provider for migration/fallback.");
                yield new SqliteDatabase(config, plugin, dataFolder);
            }
            case "sql" -> {
                final String legacyDialect = config.getString("mysql.dialect", "mariadb").toLowerCase(Locale.ROOT);
                final MySQL.Engine legacyEngine = MYSQL_STORAGE_TYPE.equals(legacyDialect)
                        ? MySQL.Engine.MYSQL
                        : MySQL.Engine.MARIADB;
                log.warn("The storage type 'sql' is deprecated. Please use 'mysql', 'mariadb' or 'sqlite' in general.storage.");
                yield new MySQL(config, plugin, legacyEngine);
            }
            default -> {
                log.warn("Unknown SQL storage type '" + storageType
                        + "'. Falling back to SQLite. Supported: mysql, mariadb, sqlite.");
                yield new SqliteDatabase(config, plugin, dataFolder);
            }
        };
    }

    /**
     * Initializes the schema and migrates legacy data in a single transaction.
     */
    private boolean initializeSchemaAndMigrate() {
        try (Connection connection = databaseProvider.getConnection()) {
            final boolean previousAutoCommit = connection.getAutoCommit();
            try {
                connection.setAutoCommit(false);
                createSchema(connection);
                migrateLegacyData(connection);
                connection.commit();
                return true;
            } catch (final SQLException ex) {
                try {
                    connection.rollback();
                } catch (final SQLException rollbackEx) {
                    log.error("Rollback after migration failure failed", rollbackEx);
                }
                log.error("Error creating schema or migrating legacy data", ex);
            } finally {
                try {
                    connection.setAutoCommit(previousAutoCommit);
                } catch (final SQLException ex) {
                    log.error("Failed to restore autoCommit state", ex);
                }
            }
        } catch (final SQLException ex) {
            log.error("Error obtaining connection for schema initialization", ex);
        }
        return false;
    }

    /**
     * Creates all required schema tables.
     *
     * @param connection an open connection
     * @throws SQLException if schema creation fails
     */
    private void createSchema(final Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(playerTable.createTableSql())) {
            statement.execute();
        }
        try (PreparedStatement statement = connection.prepareStatement(serverTable.createTableSql())) {
            statement.execute();
        }
        try (PreparedStatement statement = connection.prepareStatement(worldTable.createTableSql())) {
            statement.execute();
        }
        try (PreparedStatement statement = connection.prepareStatement(timeTable.createTableSql())) {
            statement.execute();
        }
        try (PreparedStatement statement = connection.prepareStatement(statisticTable.createTableSql())) {
            statement.execute();
        }
    }

    /**
     * Migrates data from the legacy table into the new schema.
     *
     * @param connection an open connection
     * @throws SQLException if migration fails
     */
    private void migrateLegacyData(final Connection connection) throws SQLException {
        if (!tableExists(connection, legacyTable)) {
            return;
        }
        if (tableExists(connection, playerTable.getTableName()) && playerTable.hasAnyData(connection)) {
            return;
        }
        log.info("Migrating legacy LoriTime table to the new schema ...");
        final long worldId = worldTable.ensureWorld(connection, DEFAULT_SERVER_NAME, DEFAULT_WORLD_NAME);

        try (PreparedStatement selectLegacy = connection.prepareStatement(
                "SELECT `uuid`, `name`, `time` FROM `" + legacyTable + "`")) {
            try (ResultSet result = selectLegacy.executeQuery()) {
                while (result.next()) {
                    final UUID uuid = UuidUtil.fromBytes(result.getBytes("uuid"));
                    final Optional<String> name = Optional.ofNullable(result.getString("name"));
                    final long timeSeconds = result.getLong("time");
                    final long playerId = playerTable.ensurePlayer(connection, uuid, name);
                    timeTable.insertDuration(connection, playerId, worldId, timeSeconds);
                }
            }
        }

        try (Statement dropLegacy = connection.createStatement()) {
            dropLegacy.execute("DROP TABLE IF EXISTS `" + legacyTable + "`");
        }
        log.info("Legacy migration completed.");
    }

    /**
     * Checks whether a table exists in the current database schema.
     *
     * @param connection an open connection
     * @param tableName  the table name to check
     * @return {@code true} if the table exists
     * @throws SQLException if the lookup fails
     */
    private boolean tableExists(final Connection connection, final String tableName) throws SQLException {
        try (ResultSet tables = connection.getMetaData().getTables(null, null, tableName, new String[]{"TABLE"})) {
            return tables.next();
        }
    }

    @Override
    public Optional<UUID> getUuid(final String name) throws StorageException {
        Objects.requireNonNull(name);
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = databaseProvider.getConnection()) {
                return playerTable.findUuidByName(connection, name);
            }
        } catch (final SQLException ex) {
            throw new StorageException(ex);
        } finally {
            poolLock.readLock().unlock();
        }
    }

    @Override
    public Optional<String> getName(final UUID uniqueId) throws StorageException {
        Objects.requireNonNull(uniqueId);
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = databaseProvider.getConnection()) {
                return playerTable.findNameByUuid(connection, uniqueId);
            }
        } catch (final SQLException ex) {
            throw new StorageException(ex);
        } finally {
            poolLock.readLock().unlock();
        }
    }

    @Override
    public OptionalLong getTime(final UUID uniqueId) throws StorageException {
        Objects.requireNonNull(uniqueId);
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = databaseProvider.getConnection()) {
                return timeTable.sumForPlayer(connection, uniqueId);
            }
        } catch (final SQLException ex) {
            throw new StorageException(ex);
        } finally {
            poolLock.readLock().unlock();
        }
    }

    @Override
    public void addTime(final UUID uuid, final long additionalTime) throws StorageException {
        Objects.requireNonNull(uuid);
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = databaseProvider.getConnection()) {
                final long worldId = worldTable.ensureWorld(connection, DEFAULT_SERVER_NAME, DEFAULT_WORLD_NAME);
                final long playerId = playerTable.ensurePlayer(connection, uuid, Optional.empty());
                timeTable.insertDuration(connection, playerId, worldId, additionalTime);
            }
        } catch (final SQLException ex) {
            throw new StorageException(ex);
        } finally {
            poolLock.readLock().unlock();
        }
    }

    @Override
    public void addTimes(final Map<UUID, Long> additionalTimes) throws StorageException {
        if (additionalTimes == null || additionalTimes.isEmpty()) {
            return;
        }
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = databaseProvider.getConnection()) {
                final long worldId = worldTable.ensureWorld(connection, DEFAULT_SERVER_NAME, DEFAULT_WORLD_NAME);
                for (final Map.Entry<UUID, Long> entry : additionalTimes.entrySet()) {
                    final UUID uuid = entry.getKey();
                    final long playerId = playerTable.ensurePlayer(connection, uuid, Optional.empty());
                    timeTable.insertDuration(connection, playerId, worldId, entry.getValue());
                }
            }
        } catch (final SQLException ex) {
            throw new StorageException(ex);
        } finally {
            poolLock.readLock().unlock();
        }
    }

    @Override
    public void setEntry(final UUID uuid, final String name) throws StorageException {
        Objects.requireNonNull(uuid);
        Objects.requireNonNull(name);
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = databaseProvider.getConnection()) {
                playerTable.ensurePlayer(connection, uuid, Optional.of(name));
            }
        } catch (final SQLException ex) {
            throw new StorageException(ex);
        } finally {
            poolLock.readLock().unlock();
        }
    }

    @Override
    public void setEntry(final UUID uniqueId, final String name, final boolean override) throws StorageException {
        setEntry(uniqueId, name);
    }

    @Override
    public void setEntries(final Map<UUID, String> entries) throws StorageException {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = databaseProvider.getConnection()) {
                for (final Map.Entry<UUID, String> entry : entries.entrySet()) {
                    playerTable.ensurePlayer(connection, entry.getKey(), Optional.ofNullable(entry.getValue()));
                }
            }
        } catch (final SQLException ex) {
            throw new StorageException(ex);
        } finally {
            poolLock.readLock().unlock();
        }
    }

    @Override
    public Set<String> getNameEntries() throws StorageException {
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = databaseProvider.getConnection()) {
                return playerTable.getAllNames(connection);
            }
        } catch (final SQLException ex) {
            throw new StorageException(ex);
        } finally {
            poolLock.readLock().unlock();
        }
    }

    @Override
    public Map<String, ?> getAllTimeEntries() throws StorageException {
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = databaseProvider.getConnection()) {
                return timeTable.getAllTotals(connection);
            }
        } catch (final SQLException ex) {
            throw new StorageException(ex);
        } finally {
            poolLock.readLock().unlock();
        }
    }

    @Override
    public void removeUser(final UUID uniqueId) throws StorageException, SQLException {
        deleteUser(uniqueId);
    }

    @Override
    public void removeTimeHolder(final UUID uniqueId) throws StorageException, SQLException {
        deleteUser(uniqueId);
    }

    /**
     * Deletes a player entry by UUID.
     *
     * @param uuid the player UUID
     * @throws StorageException if the deletion fails
     */
    private void deleteUser(final UUID uuid) throws StorageException {
        if (uuid == null) {
            return;
        }
        poolLock.readLock().lock();
        try {
            checkClosed();
            try (Connection connection = databaseProvider.getConnection()) {
                playerTable.deleteByUuid(connection, uuid);
            }
        } catch (final SQLException ex) {
            throw new StorageException(ex);
        } finally {
            poolLock.readLock().unlock();
        }
    }

    @Override
    public void close() throws StorageException {
        if (!databaseProvider.isClosed()) {
            try {
                databaseProvider.close();
            } catch (final IOException e) {
                throw new StorageException("The database could not be closed properly.", e);
            }
        }
    }

    /**
     * Throws an exception if the storage is already closed.
     *
     * @throws StorageException if the storage is closed
     */
    private void checkClosed() throws StorageException {
        if (!initialized) {
            throw new StorageException("database storage is not initialized");
        }
        if (databaseProvider.isClosed()) {
            throw new StorageException("closed");
        }
    }
}
