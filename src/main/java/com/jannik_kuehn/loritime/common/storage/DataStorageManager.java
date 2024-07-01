package com.jannik_kuehn.loritime.common.storage;

import com.jannik_kuehn.loritime.api.common.CommonLogger;
import com.jannik_kuehn.loritime.api.scheduler.PluginTask;
import com.jannik_kuehn.loritime.api.storage.AccumulatingTimeStorage;
import com.jannik_kuehn.loritime.api.storage.NameStorage;
import com.jannik_kuehn.loritime.common.LoriTimePlugin;
import com.jannik_kuehn.loritime.common.config.Configuration;
import com.jannik_kuehn.loritime.common.exception.StorageException;
import com.jannik_kuehn.loritime.common.storage.database.DatabaseStorage;
import com.jannik_kuehn.loritime.common.storage.file.FileNameStorage;
import com.jannik_kuehn.loritime.common.storage.file.FileTimeStorage;
import com.jannik_kuehn.loritime.common.utils.FileStorageProvider;

import java.io.File;
import java.util.Locale;

public class DataStorageManager {

    private final LoriTimePlugin loriTime;

    private final CommonLogger log;

    private final File dataFolder;

    private NameStorage nameStorage;

    private AccumulatingTimeStorage timeStorage;

    private boolean externalStorage;

    private final int saveInterval;

    private PluginTask flushCacheTask;


    public DataStorageManager(LoriTimePlugin loriTime, File dataFolder) {
        this.loriTime = loriTime;
        this.log = loriTime.getLogger();
        this.dataFolder = dataFolder;
        this.externalStorage = false;
        this.saveInterval = loriTime.getConfig().getInt("general.saveInterval");
    }

    public void injectCustomStorage(NameStorage nameStorage, AccumulatingTimeStorage timeStorage) {
        if (nameStorage == null || timeStorage == null) {
            log.severe("Custom storage injection failed: nameStorage and timeStorage must not be null!");
            return;
        }
        this.nameStorage = nameStorage;
        this.timeStorage = timeStorage;
        externalStorage = true;
    }

    public void startCache() {
        flushCacheTask = loriTime.getScheduler().scheduleAsync(saveInterval / 2L, saveInterval, this::flushOnlineTimeCache);
    }

    public void disableCache() {
        flushCacheTask.cancel();
        flushOnlineTimeCache();
    }

    public void reloadCacheTask() {
        flushCacheTask.cancel();
        flushOnlineTimeCache();
        flushCacheTask = null;

        startCache();
    }

    public void loadStorages() throws StorageException {
        if (nameStorage != null || timeStorage != null) {
            log.info("External storage detected, skipping LoriTime's default storage loading.");
            return;
        }
        String storageMethod = loriTime.getConfig().getString("general.storage", "file");
        switch (storageMethod.toLowerCase(Locale.ROOT)) {
            case "yml" -> loadFileStorage();
            case "sql", "mysql" -> loadDatabaseStorage();
            default -> log.severe("illegal storage method " + storageMethod);
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
        } catch (StorageException e) {
            log.error("An exception occurred while reloading the storage", e);
        }
    }

    public void closeStorages() {
        if (nameStorage != null) {
            try {
                nameStorage.close();
            } catch (StorageException e) {
                log.error("An exception occurred while closing the nameStorage", e);
            }
        }
        if (timeStorage != null) {
            try {
                timeStorage.close();
            } catch (StorageException e) {
                log.error("An exception occurred while closing the timeStorage", e);
            }
        }
        nameStorage = null;
        timeStorage = null;
    }

    public void flushOnlineTimeCache() {
        try {
            timeStorage.flushOnlineTimeCache();
        } catch (StorageException ex) {
            log.error("could not flush online time cache", ex);
        }
    }

    private void loadFileStorage() {
        File directory = new File(dataFolder.toString() + "/data/");
        if (!directory.exists()) {
            directory.mkdir();
        }

        Configuration nameFile = loriTime.getOrCreateFile(dataFolder + "/data/","names.yml", false);
        Configuration timeFile = loriTime.getOrCreateFile(dataFolder + "/data/","time.yml", false);
        this.nameStorage = new FileNameStorage(new FileStorageProvider(loriTime, nameFile));
        this.timeStorage = new AccumulatingTimeStorage(new FileTimeStorage(new FileStorageProvider(loriTime, timeFile)));
    }

    private void loadDatabaseStorage() throws StorageException {
        DatabaseStorage databaseStorage = new DatabaseStorage(loriTime.getConfig(), loriTime);
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
