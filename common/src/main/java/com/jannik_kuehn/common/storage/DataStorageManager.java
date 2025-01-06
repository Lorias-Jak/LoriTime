package com.jannik_kuehn.common.storage;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.logger.LoriTimeLogger;
import com.jannik_kuehn.common.api.scheduler.PluginTask;
import com.jannik_kuehn.common.api.storage.AccumulatingTimeStorage;
import com.jannik_kuehn.common.api.storage.NameStorage;
import com.jannik_kuehn.common.config.Configuration;
import com.jannik_kuehn.common.exception.ConfigurationException;
import com.jannik_kuehn.common.exception.StorageException;
import com.jannik_kuehn.common.storage.database.DatabaseStorage;
import com.jannik_kuehn.common.storage.file.FileNameStorage;
import com.jannik_kuehn.common.storage.file.FileTimeStorage;

import java.io.File;
import java.util.Locale;

/**
 * The {@link DataStorageManager} is responsible for holding the
 * {@link NameStorage} and {@link AccumulatingTimeStorage}.
 * It also manages the loading and reloading of the storages.
 * You're able to inject custom storages by calling {@link #injectCustomStorage(NameStorage, AccumulatingTimeStorage)}.
 * The {@link DataStorageManager} also handles the cache flushing for the time storage.
 */
public class DataStorageManager {
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
     * The {@link NameStorage}.
     */
    private NameStorage nameStorage;

    /**
     * The {@link AccumulatingTimeStorage}.
     */
    private AccumulatingTimeStorage timeStorage;

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
        this.log = loriTime.getLoggerFactory().create(DataStorageManager.class);
        this.dataFolder = dataFolder;
        this.externalStorage = false;
    }

    /**
     * Injects a custom {@link NameStorage} and {@link AccumulatingTimeStorage} to the plugin.
     * If the {@link NameStorage} or {@link AccumulatingTimeStorage} is {@code null}, the injection will fail.
     * The {@link DataStorageManager} will use the injected storages instead of the default ones.
     * The default storages will not be able to load or save data anymore.
     * On plugin reload, the injected storages will not be reloaded on {@link #reloadStorages()}.
     * Note that the {@link DataStorageManager} will not close the injected storages when the plugin is disabled.
     *
     * @param nameStorage the {@link NameStorage}.
     * @param timeStorage the {@link AccumulatingTimeStorage}.
     */
    public void injectCustomStorage(final NameStorage nameStorage, final AccumulatingTimeStorage timeStorage) {
        if (nameStorage == null || timeStorage == null) {
            log.error("Custom storage injection failed: nameStorage and timeStorage must not be null!");
            return;
        }
        this.nameStorage = nameStorage;
        this.timeStorage = timeStorage;
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
     * It will not load the storages if an external {@link NameStorage}
     * or {@link AccumulatingTimeStorage} is injected.
     *
     * @throws StorageException if an exception occurred while loading the storages.
     */
    public void loadStorages() throws StorageException {
        if (nameStorage != null || timeStorage != null) {
            log.info("External storage detected, skipping LoriTime's default storage loading.");
            return;
        }
        final String storageMethod = loriTime.getConfig().getString("general.storage", "file");
        switch (storageMethod.toLowerCase(Locale.ROOT)) {
            case "yml" -> loadFileStorage();
            case "sql", "mysql" -> loadDatabaseStorage();
            default -> log.error("illegal storage method " + storageMethod);
        }
    }

    /**
     * Reloads the {@link NameStorage} and {@link AccumulatingTimeStorage} if no external storage is injected.
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
     * Closes the {@link NameStorage} and {@link AccumulatingTimeStorage}.
     * External storages will be closed too.
     * If you want to load the custom storages again, you have to inject them again.
     */
    public void closeStorages() {
        if (nameStorage != null) {
            try {
                nameStorage.close();
            } catch (final StorageException e) {
                log.error("An exception occurred while closing the nameStorage", e);
            }
        }
        if (timeStorage != null) {
            try {
                timeStorage.close();
            } catch (final StorageException e) {
                log.error("An exception occurred while closing the timeStorage", e);
            }
        }
        nameStorage = null;
        timeStorage = null;
    }

    /**
     * Flushes the online time cache of the {@link AccumulatingTimeStorage}.
     */
    public void flushOnlineTimeCache() {
        try {
            timeStorage.flushOnlineTimeCache();
        } catch (final StorageException ex) {
            log.error("could not flush online time cache", ex);
        }
    }

    private void loadFileStorage() {
        final File directory = new File(dataFolder.toString() + "/data/");
        if (!directory.exists()) {
            final boolean created = directory.mkdir();
            if (!created) {
                log.error("Exception while creating the data directory. Could not create data directory for saving player data!");
            }
        }
        try {
            final File nameFile = loriTime.getFileManager().getOrCreateFile(dataFolder + "/data/", "names.yml", false);
            final File timeFile = loriTime.getFileManager().getOrCreateFile(dataFolder + "/data/", "time.yml", false);
            final Configuration nameConfiguration = loriTime.getFileManager().getConfiguration(nameFile);
            final Configuration timeConfiguration = loriTime.getFileManager().getConfiguration(timeFile);
            this.nameStorage = new FileNameStorage(new FileStorageProvider(nameConfiguration));
            this.timeStorage = new AccumulatingTimeStorage(loriTime.getLoggerFactory().create(AccumulatingTimeStorage.class),
                    new FileTimeStorage(new FileStorageProvider(timeConfiguration)));
        } catch (final ConfigurationException e) {
            log.error("An exception occurred while loading the file storages", e);
        }
    }

    @SuppressWarnings("PMD.CloseResource")
    private void loadDatabaseStorage() {
        final DatabaseStorage databaseStorage = new DatabaseStorage(loriTime.getConfig(), loriTime);
        this.nameStorage = databaseStorage;
        this.timeStorage = new AccumulatingTimeStorage(loriTime.getLoggerFactory().create(AccumulatingTimeStorage.class), databaseStorage);
    }

    /**
     * Getter of the {@link NameStorage}.
     *
     * @return the {@link NameStorage}.
     */
    public NameStorage getNameStorage() {
        return nameStorage;
    }

    /**
     * Getter of the {@link AccumulatingTimeStorage}.
     *
     * @return the {@link AccumulatingTimeStorage}.
     */
    public AccumulatingTimeStorage getTimeStorage() {
        return timeStorage;
    }
}
