package com.jannik_kuehn.common.storage;

import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.scheduler.PluginTask;
import com.jannik_kuehn.common.api.storage.AccumulatingTimeStorage;
import com.jannik_kuehn.common.api.storage.StorageMode;
import com.jannik_kuehn.common.api.storage.UnifiedStorage;
import com.jannik_kuehn.common.exception.StorageException;
import com.jannik_kuehn.common.storage.database.DatabaseStorage;
import com.jannik_kuehn.common.storage.database.DatabaseTimeAndNameStorage;
import com.jannik_kuehn.common.storage.database.table.PlayerTable;
import com.jannik_kuehn.common.storage.database.table.ServerTable;
import com.jannik_kuehn.common.storage.database.table.TimeTable;
import com.jannik_kuehn.common.storage.database.table.WorldTable;

import java.io.File;
import java.util.Locale;

/**
 * The {@link DataStorageManager} is responsible for holding the
 * {@link UnifiedStorage} and {@link AccumulatingTimeStorage}.
 * It also manages the loading and reloading of the storages.
 * You're able to inject custom storages by calling {@link #injectCustomStorage(UnifiedStorage, AccumulatingTimeStorage)}.
 * The {@link DataStorageManager} also handles the cache flushing for the time storage.
 */
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
     * The {@link AccumulatingTimeStorage}.
     */
    private AccumulatingTimeStorage accumulator;

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
     * Injects a custom {@link UnifiedStorage} and {@link AccumulatingTimeStorage} to the plugin.
     * If the {@link UnifiedStorage} or {@link AccumulatingTimeStorage} is {@code null}, the injection will fail.
     * The {@link DataStorageManager} will use the injected storages instead of the default ones.
     * The default storages will not be able to load or save data anymore.
     * On plugin reload, the injected storages will not be reloaded on {@link #reloadStorages()}.
     * Note that the {@link DataStorageManager} will not close the injected storages when the plugin is disabled.
     *
     * @param storage     the {@link UnifiedStorage}.
     * @param accumulator the {@link AccumulatingTimeStorage}.
     */
    public void injectCustomStorage(final UnifiedStorage storage, final AccumulatingTimeStorage accumulator) {
        if (storage == null || accumulator == null) {
            log.error("Custom storage injection failed: storage and accumulator must not be null!");
            return;
        }
        this.storage = storage;
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
     * or {@link AccumulatingTimeStorage} is injected.
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
     * Reloads the {@link UnifiedStorage} and {@link AccumulatingTimeStorage} if no external storage is injected.
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
     * Closes the {@link UnifiedStorage} and {@link AccumulatingTimeStorage}.
     * External storages will be closed too.
     * If you want to load the custom storages again, you have to inject them again.
     */
    public void closeStorages() {
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
        accumulator = null;
    }

    /**
     * Flushes the online time cache of the {@link AccumulatingTimeStorage}.
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

        final PlayerTable playerTable = new PlayerTable(dbStorage.getTablePrefix() + "_player");
        final ServerTable serverTable = new ServerTable(dbStorage.getTablePrefix() + "_server");
        final WorldTable worldTable = new WorldTable(dbStorage.getTablePrefix() + "_world", serverTable);
        final TimeTable timeTable = new TimeTable(dbStorage.getTablePrefix() + "_time", playerTable, dbStorage.getDialect());

        final DatabaseTimeAndNameStorage nameAndTimeStorage = new DatabaseTimeAndNameStorage(dbStorage.getProvider(), playerTable, serverTable, worldTable, timeTable);
        this.storage = nameAndTimeStorage;
        this.accumulator = new AccumulatingTimeStorage(loriTime.getLoggerFactory().create(AccumulatingTimeStorage.class), nameAndTimeStorage);
    }

    /**
     * Getter of the {@link UnifiedStorage}.
     *
     * @return the {@link UnifiedStorage}.
     */
    public UnifiedStorage getStorage() {
        return storage;
    }

    /**
     * Getter of the {@link AccumulatingTimeStorage}.
     *
     * @return the {@link AccumulatingTimeStorage}.
     */
    public AccumulatingTimeStorage getAccumulator() {
        return accumulator;
    }

    /**
     * Getter of the storage mode.
     *
     * @return the storage mode
     */
    public StorageMode getStorageMode() {
        return storageMode;
    }
}
