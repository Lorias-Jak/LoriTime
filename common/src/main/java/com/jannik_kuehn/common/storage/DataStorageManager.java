package com.jannik_kuehn.common.storage;

import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.exception.StorageException;
import com.jannik_kuehn.common.scheduler.PluginTask;
import com.jannik_kuehn.common.storage.contract.AccumulatingTimeStorage;
import com.jannik_kuehn.common.storage.contract.AdminStorageMaintenance;
import com.jannik_kuehn.common.storage.contract.StatisticsStorage;
import com.jannik_kuehn.common.storage.contract.TimeAccumulator;
import com.jannik_kuehn.common.storage.contract.UnifiedStorage;
import com.jannik_kuehn.common.storage.database.DatabaseStorage;
import com.jannik_kuehn.common.storage.database.UnifiedDatabaseStorage;
import com.jannik_kuehn.common.storage.database.migration.DatabaseMigrationPreflight;
import com.jannik_kuehn.common.storage.database.table.ManualAdjustmentTable;
import com.jannik_kuehn.common.storage.database.table.PlayerTable;
import com.jannik_kuehn.common.storage.database.table.ServerTable;
import com.jannik_kuehn.common.storage.database.table.TimeTable;
import com.jannik_kuehn.common.storage.database.table.WorldTable;
import com.jannik_kuehn.common.storage.model.StorageMode;

import java.io.File;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

/**
 * The {@link DataStorageManager} is responsible for holding the
 * {@link UnifiedStorage} and {@link TimeAccumulator} runtime contracts.
 * It also manages the loading and reloading of the storages.
 * You're able to inject custom storages by calling {@link #injectCustomStorage(UnifiedStorage, TimeAccumulator)}.
 * The {@link DataStorageManager} also handles the cache flushing for the time storage.
 */
@SuppressWarnings("PMD.TooManyMethods")
public class DataStorageManager {
    /**
     * The {@link LoriTimePlugin} instance.
     */
    private final LoriTimePlugin loriTime;

    /**
     * The {@link LoggerFactory} instance.
     */
    private final LoggerFactory loggerFactory;

    /**
     * The {@link WrappedLogger} instance.
     */
    private final WrappedLogger log;

    /**
     * The data folder of the plugin.
     */
    private final File dataFolder;

    /**
     * The {@link UnifiedStorage}.
     */
    private UnifiedStorage storage;

    /**
     * Runtime storage contract that includes active session totals.
     */
    private UnifiedStorage runtimeStorage;

    /**
     * The active session accumulator.
     */
    private TimeAccumulator accumulator;

    /**
     * The configured storage responsibility mode.
     */
    private StorageMode storageMode;

    /**
     * {@code true} if an external storage is injected, otherwise {@code false}.
     */
    private boolean externalStorage;

    /**
     * The {@link PluginTask} for the cache flushing.
     */
    private PluginTask flushCacheTask;

    /**
     * Creates a new {@link DataStorageManager} instance.
     *
     * @param loriTime   the {@link LoriTimePlugin} instance.
     * @param dataFolder the {@link File} of the data folder from the plugin.
     */
    public DataStorageManager(final LoriTimePlugin loriTime, final File dataFolder) {
        this.loriTime = loriTime;
        this.loggerFactory = loriTime.getLoggerFactory();
        this.log = loggerFactory.create(DataStorageManager.class);
        this.dataFolder = dataFolder;
        this.externalStorage = false;
    }

    /**
     * Injects custom storage contracts to the plugin.
     * If the {@link UnifiedStorage} or {@link TimeAccumulator} is {@code null}, the injection will fail.
     * The {@link DataStorageManager} will use the injected contracts instead of the default ones.
     * The default storages will not be able to load or save data anymore.
     * On plugin reload, the injected storages will not be reloaded on {@link #reloadStorages()}.
     * Note that the {@link DataStorageManager} will close the injected accumulator when storages are closed.
     *
     * @param storage     the runtime {@link UnifiedStorage}.
     * @param accumulator the {@link TimeAccumulator}.
     */
    public void injectCustomStorage(final UnifiedStorage storage, final TimeAccumulator accumulator) {
        if (storage == null || accumulator == null) {
            log.error("Custom storage injection failed: storage and accumulator must not be null!");
            return;
        }
        this.storage = storage;
        this.runtimeStorage = storage;
        this.accumulator = accumulator;
        externalStorage = true;
    }

    /**
     * Starts a {@link PluginTask} and repeatedly call the {@link #flushOnlineTimeCache()}.
     * The calling interval is defined in the config.yml.
     */
    public void startCache() {
        final int saveInterval = loriTime.getConfig().getInt("general.saveInterval");
        flushCacheTask = loriTime.getScheduler().scheduleAsync(saveInterval / 2L, saveInterval, this::flushOnlineTimeCache);
    }

    /**
     * Disables the cache flushing {@link PluginTask} and calls {@link #flushOnlineTimeCache()}.
     */
    public void disableCache() {
        if (flushCacheTask != null) {
            flushCacheTask.cancel();
            flushOnlineTimeCache();
            flushCacheTask = null;
        }
    }

    /**
     * Loads the default plugin storages.
     * It will not load the storages if an external {@link UnifiedStorage}
     * or {@link TimeAccumulator} is injected.
     *
     * @throws StorageException if an exception occurred while loading the storages.
     */
    public void loadStorages() throws StorageException {
        if (storage != null || accumulator != null) {
            log.info("External storage detected, skipping LoriTime's default storage loading.");
            return;
        }
        storageMode = StorageMode.parse(loriTime.getServer().getServerMode());
        if (storageMode == StorageMode.SLAVE) {
            log.info("Slave mode detected, skipping canonical storage loading.");
            return;
        }
        final String storageMethod = loriTime.getConfig().getString("storageMethod", "sqlite");
        switch (storageMethod.toLowerCase(Locale.ROOT)) {
            case "mysql", "mariadb", "sqlite" -> loadDatabaseStorage();
            default -> {
                log.error("Illegal storage method '" + storageMethod
                        + "'. Supported values: mysql, mariadb, sqlite (legacy: yml, sql).");
                throw new StorageException("Unsupported storage method: " + storageMethod);
            }
        }
    }

    /**
     * Reloads the {@link UnifiedStorage} and {@link TimeAccumulator} if no external storage is injected.
     */
    public void reloadStorages() {
        if (externalStorage) {
            log.info("External storage detected, skipping storage reloading.");
            return;
        }
        try {
            closeStorages();
            loadStorages();
        } catch (final StorageException e) {
            log.error("An exception occurred while reloading the storage", e);
        }
    }

    /**
     * Closes the {@link UnifiedStorage} and {@link TimeAccumulator}.
     * External storages will be closed too through the injected accumulator.
     * If you want to load the custom storages again, you have to inject them again.
     */
    public void closeStorages() {
        getStatisticsStorage().ifPresent(statistics -> {
            try {
                statistics.recoverOpenAfkPeriods(Instant.now());
            } catch (final StorageException e) {
                log.error("Could not close open AFK periods during shutdown", e);
            }
        });
        if (accumulator != null) {
            try {
                accumulator.close();
            } catch (final StorageException e) {
                log.error("An exception occurred while closing the accumulator", e);
            }
        } else if (storage != null) {
            try {
                storage.close();
            } catch (final StorageException e) {
                log.error("An exception occurred while closing the storage", e);
            }
        }
        storage = null;
        runtimeStorage = null;
        accumulator = null;
    }

    /**
     * Flushes the online time cache of the {@link TimeAccumulator}.
     */
    public void flushOnlineTimeCache() {
        try {
            if (accumulator != null) {
                accumulator.flushOnlineTimeCache();
            }
        } catch (final StorageException ex) {
            log.error("could not flush online time cache", ex);
        }
    }

    @SuppressWarnings("PMD.CloseResource")
    private void loadDatabaseStorage() throws StorageException {
        final DatabaseStorage dbStorage = new DatabaseStorage(loggerFactory, loriTime.getConfig(), dataFolder);
        dbStorage.initializeRuntime();
        final UnifiedDatabaseStorage nameAndTimeStorage = createUnifiedDatabaseStorage(dbStorage);
        final AccumulatingTimeStorage accumulatingStorage = new AccumulatingTimeStorage(
                loriTime.getLoggerFactory().create(AccumulatingTimeStorage.class), nameAndTimeStorage);
        this.storage = nameAndTimeStorage;
        this.runtimeStorage = accumulatingStorage;
        this.accumulator = accumulatingStorage;
        try {
            nameAndTimeStorage.recoverOpenAfkPeriods(Instant.now());
        } catch (final StorageException ex) {
            log.error("Could not recover stale AFK periods as SHUTDOWN", ex);
        }
        runStorageCleanupIfEnabled();
    }

    private void runStorageCleanupIfEnabled() {
        if (!loriTime.getConfig().getBoolean("storageCleanup.enabled", false)) {
            return;
        }
        final long inactiveDays = loriTime.getConfig().getInt("storageCleanup.inactiveAfterDays", 365);
        try {
            final int deletedRows = storage.deleteInactiveHistory(inactiveDays);
            log.info("Storage cleanup removed " + deletedRows + " history rows for players inactive for more than "
                    + inactiveDays + " days.");
        } catch (final StorageException ex) {
            log.error("Could not run storage cleanup", ex);
        }
    }

    /**
     * Getter of the {@link UnifiedStorage}.
     *
     * @return the {@link UnifiedStorage}.
     */
    public UnifiedStorage getStorage() {
        return runtimeStorage == null ? storage : runtimeStorage;
    }

    /**
     * Getter of the {@link TimeAccumulator}.
     *
     * @return the {@link TimeAccumulator}.
     */
    public TimeAccumulator getAccumulator() {
        return accumulator;
    }

    /** Returns canonical statistics/AFK storage when supported. */
    public Optional<StatisticsStorage> getStatisticsStorage() {
        return getStorage() instanceof final StatisticsStorage statistics ? Optional.of(statistics) : Optional.empty();
    }

    /**
     * Returns optional admin storage maintenance support.
     *
     * @return maintenance contract when the active canonical storage supports it
     */
    public Optional<AdminStorageMaintenance> getAdminStorageMaintenance() {
        if (!ownsCanonicalStorage() || !(storage instanceof final AdminStorageMaintenance maintenance)) {
            return Optional.empty();
        }
        return Optional.of(maintenance);
    }

    /**
     * Returns whether this runtime owns canonical storage.
     *
     * @return true when storage maintenance may be attempted locally
     */
    public boolean ownsCanonicalStorage() {
        return storageMode != StorageMode.SLAVE && storage != null;
    }

    /**
     * Getter of the storage mode.
     *
     * @return the storage mode
     */
    public StorageMode getStorageMode() {
        return storageMode;
    }

    /**
     * Creates a temporary admin maintenance target for a storage-type transfer.
     *
     * @param storageMethod target storage method
     * @return initialized transfer target
     * @throws StorageException if the target cannot be initialized
     */
    public StorageTransferTarget createStorageTransferTarget(final String storageMethod) throws StorageException {
        final DatabaseStorage dbStorage = new DatabaseStorage(loggerFactory, loriTime.getConfig(), dataFolder,
                loriTime.getConfig().getString("data.tablePrefix", "loritime"), storageMethod);
        new DatabaseMigrationPreflight(dbStorage, loggerFactory.create(DatabaseMigrationPreflight.class)).migrateIfNecessary();
        return new StorageTransferTarget(dbStorage, createUnifiedDatabaseStorage(dbStorage));
    }

    private UnifiedDatabaseStorage createUnifiedDatabaseStorage(final DatabaseStorage dbStorage) {
        final PlayerTable playerTable = new PlayerTable(dbStorage.getTablePrefix() + "_player");
        final ServerTable serverTable = new ServerTable(dbStorage.getTablePrefix() + "_server");
        final WorldTable worldTable = new WorldTable(dbStorage.getTablePrefix() + "_world", serverTable);
        final TimeTable timeTable = new TimeTable(dbStorage.getTablePrefix() + "_time", playerTable, dbStorage.getDialect());
        final ManualAdjustmentTable adjustmentTable = new ManualAdjustmentTable(dbStorage.getTablePrefix() + "_time_adjustment", playerTable);
        return new UnifiedDatabaseStorage(dbStorage.getProvider(), dbStorage.getTablePrefix(), playerTable,
                serverTable, worldTable, timeTable, adjustmentTable, dbStorage.getDialect());
    }

    /**
     * Temporary storage transfer target.
     *
     * @param databaseStorage backing database storage
     * @param maintenance     admin maintenance implementation
     */
    public record StorageTransferTarget(DatabaseStorage databaseStorage,
                                        AdminStorageMaintenance maintenance) implements AutoCloseable {

        @Override
        public void close() throws StorageException {
            databaseStorage.shutdown();
        }
    }
}
