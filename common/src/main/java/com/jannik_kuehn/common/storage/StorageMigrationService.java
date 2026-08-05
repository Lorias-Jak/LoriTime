package com.jannik_kuehn.common.storage;

import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.storage.TimeScope;
import com.jannik_kuehn.common.config.Configuration;
import com.jannik_kuehn.common.config.YamlConfiguration;
import com.jannik_kuehn.common.exception.StorageException;
import com.jannik_kuehn.common.storage.database.DatabaseStorage;
import com.jannik_kuehn.common.storage.database.UnifiedDatabaseStorage;
import com.jannik_kuehn.common.storage.database.migration.DatabaseMigrationPreflight;
import com.jannik_kuehn.common.storage.database.table.ManualAdjustmentTable;
import com.jannik_kuehn.common.storage.database.table.PlayerTable;
import com.jannik_kuehn.common.storage.database.table.ServerTable;
import com.jannik_kuehn.common.storage.database.table.TimeTable;
import com.jannik_kuehn.common.storage.database.table.WorldTable;
import com.jannik_kuehn.common.storage.model.ManualTimeAdjustment;
import com.jannik_kuehn.common.storage.model.SessionContextDefaults;
import com.jannik_kuehn.common.storage.model.TimeEntryReason;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles storage migration before runtime storages are loaded.
 */
@SuppressWarnings({"PMD.GodClass", "PMD.TooManyMethods"})
public class StorageMigrationService {

    /**
     * Legacy file containing player names.
     */
    private static final String NAMES_FILE = "names.yml";

    /**
     * Legacy file containing player times.
     */
    private static final String TIME_FILE = "time.yml";

    /**
     * Legacy subdirectory used by older flat-file storage.
     */
    private static final String LEGACY_DATA_DIRECTORY = "data";

    /**
     * Config marker that requests a legacy flat-file import after config migration.
     */
    private static final String LEGACY_FLAT_FILE_IMPORT_PATH = "storageMigration.legacyFlatFileImport";

    /**
     * SQLite storage method value.
     */
    private static final String SQLITE_STORAGE_METHOD = "sqlite";

    /**
     * Canonical table prefix used when importing legacy flat-file data.
     */
    private static final String CANONICAL_TABLE_PREFIX = "loritime";

    /**
     * Actor name used for migration-created adjustment rows.
     */
    private static final String MIGRATION_ACTOR = "MIGRATION";

    /**
     * The plugin instance.
     */
    private final LoriTimePlugin loriTime;

    /**
     * The plugin data folder.
     */
    private final File dataFolder;

    /**
     * The logger.
     */
    private final WrappedLogger log;

    /**
     * Creates a new storage migration service.
     *
     * @param loriTime   the plugin instance
     * @param dataFolder the plugin data folder
     */
    public StorageMigrationService(final LoriTimePlugin loriTime, final File dataFolder) {
        this.loriTime = loriTime;
        this.dataFolder = dataFolder;
        this.log = loriTime.getLoggerFactory().create(StorageMigrationService.class);
    }

    /**
     * Runs all storage migrations needed before normal storage loading.
     *
     * @throws StorageException if migration fails
     */
    public void migrateIfNecessary() throws StorageException {
        final String storageMethod = loriTime.getConfig().getString("storageMethod", SQLITE_STORAGE_METHOD);
        switch (storageMethod.toLowerCase(Locale.ROOT)) {
            case SQLITE_STORAGE_METHOD -> migrateSqliteStorageIfNecessary();
            case "mysql", "mariadb" -> {
                logDetectedFlatFilesSkippedForSql();
                migrateSqlDatabaseIfNecessary();
            }
            default -> throw new StorageException("Unsupported storage method: " + storageMethod);
        }
    }

    /**
     * Queues legacy flat-file storage for the normal startup backup.
     */
    public void addLegacyFilesToStartupBackup() {
        final File namesFile = findLegacyFile(NAMES_FILE);
        final File timeFile = findLegacyFile(TIME_FILE);
        if (!namesFile.exists() && !timeFile.exists()) {
            return;
        }

        loriTime.getFileManager().addToBackup(namesFile);
        loriTime.getFileManager().addToBackup(timeFile);
    }

    private void migrateSqlDatabaseIfNecessary() throws StorageException {
        final String storageMethod = loriTime.getConfig().getString("storageMethod", SQLITE_STORAGE_METHOD);
        switch (storageMethod.toLowerCase(Locale.ROOT)) {
            case "mysql", "mariadb", SQLITE_STORAGE_METHOD -> {
                final DatabaseStorage databaseStorage = new DatabaseStorage(loriTime.getLoggerFactory(), loriTime.getConfig(), dataFolder);
                try {
                    new DatabaseMigrationPreflight(databaseStorage, log).migrateIfNecessary();
                } finally {
                    databaseStorage.shutdown();
                }
            }
            default -> throw new StorageException("Unsupported storage method: " + storageMethod);
        }
    }

    private void migrateSqliteStorageIfNecessary() throws StorageException {
        final File namesFile = findLegacyFile(NAMES_FILE);
        final File timeFile = findLegacyFile(TIME_FILE);
        final boolean importRequested = loriTime.getConfig().getBoolean(LEGACY_FLAT_FILE_IMPORT_PATH, false);
        if (!importRequested && !namesFile.exists() && !timeFile.exists()) {
            migrateSqlDatabaseIfNecessary();
            return;
        }
        if (!namesFile.exists() && !timeFile.exists()) {
            log.warn("Legacy flat-file import was requested, but no legacy names.yml or time.yml file was found.");
            loriTime.getConfig().setValue(LEGACY_FLAT_FILE_IMPORT_PATH, false);
            migrateSqlDatabaseIfNecessary();
            return;
        }

        log.info("Detected legacy flat-file storage. Migrating files to SQLite.");

        final DatabaseStorage databaseStorage = new DatabaseStorage(
                loriTime.getLoggerFactory(), loriTime.getConfig(), dataFolder, CANONICAL_TABLE_PREFIX);
        try {
            new DatabaseMigrationPreflight(databaseStorage, log).migrateIfNecessary();
            importLegacyFiles(databaseStorage, namesFile, timeFile);
            markMigrated(namesFile);
            markMigrated(timeFile);
            loriTime.getConfig().setValue(LEGACY_FLAT_FILE_IMPORT_PATH, false);
        } finally {
            databaseStorage.shutdown();
        }
    }

    private void logDetectedFlatFilesSkippedForSql() {
        final File namesFile = findLegacyFile(NAMES_FILE);
        final File timeFile = findLegacyFile(TIME_FILE);
        if (namesFile.exists() || timeFile.exists()) {
            log.warn("Detected legacy flat-file storage, but storageMethod is configured for SQL. "
                    + "The files were queued for startup backup and will not be imported into SQLite.");
        }
    }

    private File findLegacyFile(final String fileName) {
        final File rootFile = new File(dataFolder, fileName);
        if (rootFile.exists()) {
            return rootFile;
        }
        return new File(new File(dataFolder, LEGACY_DATA_DIRECTORY), fileName);
    }

    @SuppressWarnings("PMD.CloseResource")
    private void importLegacyFiles(final DatabaseStorage databaseStorage, final File namesFile, final File timeFile) throws StorageException {
        final PlayerTable playerTable = new PlayerTable(databaseStorage.getTablePrefix() + "_player");
        final ServerTable serverTable = new ServerTable(databaseStorage.getTablePrefix() + "_server");
        final WorldTable worldTable = new WorldTable(databaseStorage.getTablePrefix() + "_world", serverTable);
        final TimeTable timeTable = new TimeTable(databaseStorage.getTablePrefix() + "_time", playerTable, databaseStorage.getDialect());
        final ManualAdjustmentTable adjustmentTable = new ManualAdjustmentTable(databaseStorage.getTablePrefix() + "_time_adjustment", playerTable);
        final UnifiedDatabaseStorage storage = new UnifiedDatabaseStorage(databaseStorage.getProvider(),
                databaseStorage.getTablePrefix(), playerTable, serverTable, worldTable, timeTable, adjustmentTable,
                databaseStorage.getDialect());

        importNames(storage, namesFile);
        importTimes(storage, timeFile);
    }

    private void importNames(final UnifiedDatabaseStorage storage, final File namesFile) throws StorageException {
        if (!namesFile.exists()) {
            return;
        }
        final Configuration names = loadFlatDataFile(namesFile);
        for (final Map.Entry<String, Object> entry : names.getAll().entrySet()) {
            final Optional<UUID> uuid = parseUuid(entry.getValue());
            if (uuid.isPresent()) {
                storage.setPlayerName(uuid.get(), entry.getKey());
            }
        }
    }

    private void importTimes(final UnifiedDatabaseStorage storage, final File timeFile) throws StorageException {
        if (!timeFile.exists()) {
            return;
        }
        final Configuration times = loadFlatDataFile(timeFile);
        for (final Map.Entry<String, Object> entry : times.getAll().entrySet()) {
            final Optional<UUID> uuid = parseUuid(entry.getKey());
            final Optional<Long> time = parseLong(entry.getValue());
            if (uuid.isPresent() && time.isPresent()) {
                storage.addTime(new ManualTimeAdjustment(uuid.get(), time.get(), TimeEntryReason.LEGACY_IMPORT,
                        MIGRATION_ACTOR, TimeScope.world(SessionContextDefaults.SERVER, SessionContextDefaults.WORLD)));
            }
        }
    }

    private Configuration loadFlatDataFile(final File file) throws StorageException {
        final Configuration configuration = new YamlConfiguration(file.toString(), loriTime.getLoggerFactory());
        if (!configuration.isLoaded()) {
            throw new StorageException("Could not load legacy flat data file: " + file.getName());
        }
        return configuration;
    }

    private Optional<UUID> parseUuid(final Object value) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(value.toString()));
        } catch (final IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private Optional<Long> parseLong(final Object value) {
        if (value instanceof final Number number) {
            return Optional.of(number.longValue());
        }
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(value.toString()));
        } catch (final NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    private void markMigrated(final File file) throws StorageException {
        if (!file.exists()) {
            return;
        }
        final File migrated = new File(file.getParentFile(), file.getName() + ".migrated");
        try {
            Files.move(file.toPath(), migrated.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (final IOException ex) {
            throw new StorageException("Could not mark legacy file as migrated: " + file.getName(), ex);
        }
    }
}
