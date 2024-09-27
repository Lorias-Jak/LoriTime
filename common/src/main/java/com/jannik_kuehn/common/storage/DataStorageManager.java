package com.jannik_kuehn.common.storage;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.logger.LoriTimeLogger;
import com.jannik_kuehn.common.api.scheduler.PluginTask;
import com.jannik_kuehn.common.api.storage.AccumulatingTimeStorage;
import com.jannik_kuehn.common.api.storage.NameStorage;
import com.jannik_kuehn.common.config.Configuration;
import com.jannik_kuehn.common.exception.StorageException;
import com.jannik_kuehn.common.storage.database.DatabaseStorage;
import com.jannik_kuehn.common.storage.file.FileNameStorage;
import com.jannik_kuehn.common.storage.file.FileTimeStorage;
import com.jannik_kuehn.common.utils.FileStorageProvider;

import java.io.File;
import java.util.Locale;

public class DataStorageManager {

    private final LoriTimePlugin loriTime;

    private final LoriTimeLogger log;

    private final File dataFolder;

    private NameStorage nameStorage;

    private AccumulatingTimeStorage timeStorage;

    private boolean externalStorage;

    private PluginTask flushCacheTask;

    public DataStorageManager(final LoriTimePlugin loriTime, final File dataFolder) {
        this.loriTime = loriTime;
        this.log = loriTime.getLoggerFactory().create(DataStorageManager.class);
        this.dataFolder = dataFolder;
        this.externalStorage = false;
    }

    public void injectCustomStorage(final NameStorage nameStorage, final AccumulatingTimeStorage timeStorage) {
        if (nameStorage == null || timeStorage == null) {
            log.error("Custom storage injection failed: nameStorage and timeStorage must not be null!");
            return;
        }
        this.nameStorage = nameStorage;
        this.timeStorage = timeStorage;
        externalStorage = true;
    }

    public void startCache() {
        final int saveInterval = loriTime.getConfig().getInt("general.saveInterval");
        flushCacheTask = loriTime.getScheduler().scheduleAsync(saveInterval / 2L, saveInterval, this::flushOnlineTimeCache);
    }

    public void disableCache() {
        if (flushCacheTask != null) {
            flushCacheTask.cancel();
            flushOnlineTimeCache();
            flushCacheTask = null;
        }
    }

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

        final Configuration nameFile = loriTime.getOrCreateFile(dataFolder + "/data/", "names.yml", false);
        final Configuration timeFile = loriTime.getOrCreateFile(dataFolder + "/data/", "time.yml", false);
        this.nameStorage = new FileNameStorage(new FileStorageProvider(nameFile));
        this.timeStorage = new AccumulatingTimeStorage(new FileTimeStorage(new FileStorageProvider(timeFile)));
    }

    private void loadDatabaseStorage() {
        final DatabaseStorage databaseStorage = new DatabaseStorage(loriTime.getConfig(), loriTime);
        this.nameStorage = databaseStorage;
        this.timeStorage = new AccumulatingTimeStorage(databaseStorage);
    }

    public NameStorage getNameStorage() {
        return nameStorage;
    }

    public AccumulatingTimeStorage getTimeStorage() {
        return timeStorage;
    }
}
