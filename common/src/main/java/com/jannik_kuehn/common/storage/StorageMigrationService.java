package com.jannik_kuehn.common.storage;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.logger.LoriTimeLogger;
import com.jannik_kuehn.common.config.Configuration;
import com.jannik_kuehn.common.config.FileManager;
import com.jannik_kuehn.common.exception.ConfigurationException;
import com.jannik_kuehn.common.exception.StorageException;
import com.jannik_kuehn.common.storage.database.DatabaseStorage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Handles one-time startup migrations for storage/config changes.
 */
public class StorageMigrationService {

    /**
     * The string of the yml storage type
     */
    public static final String YML = "yml";

    /**
     * The current config version.
     */
    private static final int CURRENT_CONFIG_VERSION = 2;

    /**
     * The {@link LoriTimePlugin} instance.
     */
    private final LoriTimePlugin loriTime;

    /**
     * The {@link LoriTimeLogger} instance.
     */
    private final LoriTimeLogger log;

    /**
     * The data folder of the plugin.
     */
    private final File dataFolder;

    /**
     * The {@link FileManager} instance.
     */
    private final FileManager fileManager;

    /**
     * The {@link Configuration} instance.
     */
    private final Configuration config;

    /**
     * Creates a migration service.
     *
     * @param loriTime   plugin instance
     * @param dataFolder data folder of the plugin
     */
    public StorageMigrationService(final LoriTimePlugin loriTime, final File dataFolder) {
        this.loriTime = loriTime;
        this.log = loriTime.getLoggerFactory().create(StorageMigrationService.class);
        this.dataFolder = dataFolder;
        this.fileManager = loriTime.getFileManager();
        this.config = loriTime.getConfig();
    }

    /**
     * Executes pending startup migrations.
     *
     * @throws StorageException if a migration fails
     */
    public void migrateIfNecessary() throws StorageException {
        final int currentVersion = readConfigVersion();
        if (currentVersion >= CURRENT_CONFIG_VERSION) {
            return;
        }

        final String configuredStorage = config.getString("general.storage", "sqlite").toLowerCase(Locale.ROOT);
        if (YML.equals(configuredStorage)) {
            migrateYmlToSqlite();
        }

        config.setValue("general.configVersion", CURRENT_CONFIG_VERSION);
    }

    private int readConfigVersion() {
        final Object configuredVersion = config.getObject("general.configVersion", 0);
        if (configuredVersion instanceof Number) {
            return ((Number) configuredVersion).intValue();
        }
        return 0;
    }

    @SuppressWarnings("PMD.CloseResource")
    private void migrateYmlToSqlite() throws StorageException {
        final String migrationId = String.valueOf(Instant.now().toEpochMilli());
        log.warn("Storage 'yml' is deprecated and will be migrated to SQLite now.");

        final File dataDirectory = new File(dataFolder, "data");
        if (!dataDirectory.exists() && !dataDirectory.mkdirs()) {
            throw new StorageException("Failed to create data directory for yml migration.");
        }

        final File namesFile;
        final File timeFile;
        try {
            namesFile = fileManager.getOrCreateFile(dataFolder + "/data/", "names.yml", false);
            timeFile = fileManager.getOrCreateFile(dataFolder + "/data/", "time.yml", false);
        } catch (final ConfigurationException e) {
            throw new StorageException("Failed preparing yml files for migration.", e);
        }

        final Map<UUID, String> names = loadNames(namesFile);
        final Map<UUID, Long> times = loadTimes(timeFile);

        prepareSqliteFile(migrationId);

        try (DatabaseStorage sqliteStorage = new DatabaseStorage(config, loriTime, dataFolder, false)) {
            sqliteStorage.initialize();
            sqliteStorage.setEntries(names);
            sqliteStorage.addTimes(times);
            config.setValue("general.storage", "sqlite");
            archiveLegacyFile(namesFile, "names", migrationId);
            archiveLegacyFile(timeFile, "time", migrationId);
            log.info("Successfully migrated yml storage to SQLite. Imported " + names.size()
                    + " names and " + times.size() + " time entries.");
        }
    }

    private Map<UUID, String> loadNames(final File namesFile) throws StorageException {
        final Map<UUID, String> names = new HashMap<>();
        try {
            final Configuration namesConfig = fileManager.getConfiguration(namesFile);
            for (final Map.Entry<String, Object> entry : namesConfig.getAll().entrySet()) {
                if (!(entry.getValue() instanceof String)) {
                    continue;
                }
                try {
                    final UUID uuid = UUID.fromString((String) entry.getValue());
                    names.put(uuid, entry.getKey());
                } catch (final IllegalArgumentException ignored) {
                    log.warn("Skipping malformed UUID in names.yml for key '" + entry.getKey() + "'.");
                }
            }
            return names;
        } catch (final ConfigurationException e) {
            throw new StorageException("Failed reading names.yml for migration.", e);
        }
    }

    private Map<UUID, Long> loadTimes(final File timeFile) throws StorageException {
        final Map<UUID, Long> times = new HashMap<>();
        try {
            final Configuration timeConfig = fileManager.getConfiguration(timeFile);
            for (final Map.Entry<String, Object> entry : timeConfig.getAll().entrySet()) {
                final Object value = entry.getValue();
                if (!(value instanceof Number)) {
                    continue;
                }
                try {
                    final UUID uuid = UUID.fromString(entry.getKey());
                    final long seconds = ((Number) value).longValue();
                    if (seconds < 0) {
                        log.warn("Skipping negative time value for UUID '" + uuid + "'.");
                        continue;
                    }
                    times.merge(uuid, seconds, Long::sum);
                } catch (final IllegalArgumentException ignored) {
                    log.warn("Skipping malformed UUID key in time.yml: '" + entry.getKey() + "'.");
                }
            }
            return times;
        } catch (final ConfigurationException e) {
            throw new StorageException("Failed reading time.yml for migration.", e);
        }
    }

    @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
    private void prepareSqliteFile(final String migrationId) throws StorageException {
        final File sqliteFile = resolveSqliteFile();
        if (!sqliteFile.exists()) {
            return;
        }
        final long length = sqliteFile.length();
        if (length == 0L && !sqliteFile.delete()) {
            throw new StorageException("Could not cleanup empty sqlite database file before migration.");
        }
        if (length == 0L) {
            return;
        }

        final Path backupDir = dataFolder.toPath().resolve("migration-backups");
        final Path backupFile = backupDir.resolve("sqlite-pre-yml-migration-" + migrationId + ".db");
        try {
            Files.createDirectories(backupDir);
            Files.move(sqliteFile.toPath(), backupFile, StandardCopyOption.REPLACE_EXISTING);
            log.warn("Existing sqlite database was moved to '" + backupFile + "' before yml migration.");
        } catch (final IOException e) {
            throw new StorageException("Could not backup existing sqlite database before migration.", e);
        }
    }

    private File resolveSqliteFile() {
        final String configuredPath = config.getString("sqlite.file", "loritime.db");
        final File file = new File(configuredPath);
        if (file.isAbsolute()) {
            return file;
        }
        return new File(dataFolder, configuredPath);
    }

    private void archiveLegacyFile(final File source, final String fileType, final String migrationId) throws StorageException {
        final Path archiveDir = dataFolder.toPath().resolve("migration-backups");
        final Path target = archiveDir.resolve(fileType + "-yml-pre-sqlite-" + migrationId + ".yml");
        try {
            Files.createDirectories(archiveDir);
            Files.move(source.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (final IOException e) {
            throw new StorageException("Could not archive legacy file '" + source.getName() + "' after migration.", e);
        }
    }
}
