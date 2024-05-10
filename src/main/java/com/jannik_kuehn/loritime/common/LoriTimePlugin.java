package com.jannik_kuehn.loritime.common;

import com.jannik_kuehn.loritime.api.PluginTask;
import com.jannik_kuehn.loritime.common.config.YamlConfiguration;
import com.jannik_kuehn.loritime.common.exception.StorageException;
import com.jannik_kuehn.loritime.common.module.afk.AfkHandling;
import com.jannik_kuehn.loritime.common.module.afk.AfkStatusProvider;
import com.jannik_kuehn.loritime.common.storage.AccumulatingTimeStorage;
import com.jannik_kuehn.loritime.common.storage.database.DatabaseStorage;
import com.jannik_kuehn.loritime.common.storage.file.FileTimeStorage;
import com.jannik_kuehn.loritime.common.storage.file.FileNameStorage;
import com.jannik_kuehn.loritime.common.storage.NameStorage;
import com.jannik_kuehn.loritime.api.CommonServer;
import com.jannik_kuehn.loritime.api.PluginScheduler;
import com.jannik_kuehn.loritime.common.config.Configuration;
import com.jannik_kuehn.loritime.common.config.localization.Localization;
import com.jannik_kuehn.loritime.api.CommonLogger;
import com.jannik_kuehn.loritime.common.utils.FileStorageProvider;
import com.jannik_kuehn.loritime.common.utils.TimeParser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class LoriTimePlugin {
    private static LoriTimePlugin instance;
    private final CommonLogger logger;
    private final File dataFolder;
    private final PluginScheduler scheduler;
    private final CommonServer server;
    private Configuration config;
    private Localization localization;
    private NameStorage nameStorage;
    private AccumulatingTimeStorage timeStorage;
    private TimeParser parser;
    private int saveInterval;
    private PluginTask flushCacheTask;
    private AfkStatusProvider afkStatusProvider;
    private boolean errorDisable;
    private final String pluginVersion;

    public static LoriTimePlugin getInstance() {
        return instance;
    }

    public LoriTimePlugin(CommonLogger logger, File dataFolder, PluginScheduler scheduler, CommonServer server) {
        instance = this;

        this.logger = logger;
        this.dataFolder = dataFolder;
        this.scheduler = scheduler;
        this.server = server;
        this.errorDisable = false;
        this.pluginVersion = "LoriTime-1.3.0";
    }

    public void enable() {
        loadOrCreateConfigs();
        try {
            loadStorage();
        } catch (StorageException e) {
            logger.error("An error occurred while enabling the storage", e);
            errorDisable = true;
        }
        flushCacheTask = scheduler.scheduleAsync(saveInterval / 2L, saveInterval, this::flushOnlineTimeCache);

        if (errorDisable) {
            logger.severe("Disabling the plugin because of an issue.");
            disable();
        }
        server.setServerMode(getServerModeFromConfig());
    }

    private String getServerModeFromConfig() {
        String serverMode;
        if (!config.getBoolean("multiSetup.enabled", false)) {
            serverMode = "master";
        } else {
            serverMode = config.getString("multiSetup.mode", "master");
        }
        return serverMode;
    }

    public void enableAfkFeature(AfkHandling afkHandling) {
        afkStatusProvider = new AfkStatusProvider(this, afkHandling);
    }

    public boolean isAfkEnabled() {
        return config.getBoolean("afk.enabled", false);
    }

    public void disable() {
        flushCacheTask.cancel();
        flushOnlineTimeCache();

        closeStorages();
    }

    public void reload() throws StorageException {
        flushCacheTask.cancel();
        flushOnlineTimeCache();
        timeStorage.close();
        timeStorage = null;
        nameStorage.close();
        nameStorage = null;

        flushCacheTask = null;

        config.reload();
        localization.reloadTranslation();

        closeStorages();
        loadStorage();
        afkStatusProvider.reloadConfigValues();

        flushCacheTask = scheduler.scheduleAsync(saveInterval / 2L, saveInterval, this::flushOnlineTimeCache);
    }

    private void closeStorages() {
        if (nameStorage != null) {
            try {
                nameStorage.close();
            } catch (StorageException e) {
                logger.error("An exception occurred while closing the nameStorage", e);
            }
        }
        if (timeStorage != null) {
            try {
                timeStorage.close();
            } catch (StorageException e) {
                logger.error("An exception occurred while closing the timeStorage", e);
            }
        }
    }

    private void loadOrCreateConfigs() {
        File directory = new File(dataFolder.toString());
        if (!directory.exists()) {
            directory.mkdir();
        }

        this.config = getOrCreateFile(dataFolder.toString(),"config.yml", true);
        Configuration localizationFile = getOrCreateFile(dataFolder.toString(), config.getString("general.language", "en") + ".yml", true);
        this.localization = new Localization(localizationFile);

        if (!config.isLoaded() || !localization.getLangFile().isLoaded()) {
            logger.severe("The plugins localization and config didn't load correctly. Pls delete the files and try again! Stop starting plugin..");
            errorDisable = true;
            return;
        }
        try {
            parser = new TimeParser.Builder()
                    .addUnit(1, getUnits(localization, "second"))
                    .addUnit(60, getUnits(localization, "minute"))
                    .addUnit(60 * 60, getUnits(localization, "hour"))
                    .addUnit(60 * 60 * 24, getUnits(localization, "day"))
                    .addUnit(60 * 60 * 24 * 7, getUnits(localization, "week"))
                    .addUnit(60 * 60 * 24 * 30, getUnits(localization, "month"))
                    .addUnit(60 * 60 * 24 * 30 * 12, getUnits(localization, "year"))
                    .build();
        } catch (IllegalArgumentException ex) {
            logger.error("Could not create time parser.", ex);
        }

        saveInterval = config.getInt("general.saveInterval");
    }

    private Configuration getOrCreateFile(String folder, String fileName, boolean needCopy) {
        File file = new File(folder, fileName);
        if (!file.exists()) {
            try {
                file.createNewFile();
                logger.info("Creating new File '" + fileName + "'.");
                if (needCopy) copyDataFromResource(file.toPath(), fileName);
            } catch (IOException e) {
                logger.error("An exception occurred while creating the file '" + fileName + "' on startup.", e);
            }
        }
        Configuration configurationFile = new YamlConfiguration(file.toString());
        if (!configurationFile.isLoaded()) {
            logger.severe("An issue occurred while loading the file '" + fileName + "'. The File is null, there should be data.");
            return null;
        }
        logger.info("Successfully loaded '" + fileName + "'.");
        return configurationFile;
    }

    private void copyDataFromResource(Path configFile, String nameFromSourceOfReplacement) {
        try {
            Files.copy(this.getClass().getClassLoader().getResource(nameFromSourceOfReplacement).openStream(), configFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logger.error("Could not copy the file content to the file '" + nameFromSourceOfReplacement + "'. Pls delete the file and try again.", e);
        }
    }

    private String[] getUnits(Localization langConfig, String unit) {
        String singular = langConfig.getRawMessage("unit." + unit + ".singular");
        String plural = langConfig.getRawMessage("unit." + unit + ".plural");
        List<?> identifier = langConfig.getLangArray("unit." + unit + ".identifier");
        Set<String> units = new HashSet<>();
        units.add(singular);
        units.add(plural);
        for (Object content : identifier) {
            if (content instanceof String) {
                units.add((String) content);
            } else {
                logger.warning("dangerous identifier definition in language file. Path: " + "unit." + unit + "identifier: " + content.toString());
                units.add(content.toString());
            }
        }
        return units.toArray(new String[0]);
    }

    private void loadStorage() throws StorageException {
        String storageMethod = config.getString("general.storage", "file");
        switch (storageMethod.toLowerCase(Locale.ROOT)) {
            case "yml" -> loadFileStorage();
            case "sql", "mysql" -> loadDatabaseStorage();
            default -> logger.severe("illegal storage method " + storageMethod);
        }
    }

    private void loadFileStorage() {
        File directory = new File(dataFolder.toString() + "/data/");
        if (!directory.exists()) {
            directory.mkdir();
        }

        Configuration nameFile = getOrCreateFile(dataFolder + "/data/","names.yml", false);
        Configuration timeFile = getOrCreateFile(dataFolder + "/data/","time.yml", false);
        this.nameStorage = new FileNameStorage(new FileStorageProvider(this, nameFile));
        this.timeStorage = new AccumulatingTimeStorage(new FileTimeStorage(new FileStorageProvider(this, timeFile)));
    }

    private void loadDatabaseStorage() throws StorageException {
        DatabaseStorage databaseStorage = new DatabaseStorage(config, this);
        this.nameStorage = databaseStorage;
        this.timeStorage = new AccumulatingTimeStorage(databaseStorage);
    }

    public void flushOnlineTimeCache() {
        try {
            timeStorage.flushOnlineTimeCache();
        } catch (StorageException ex) {
            logger.error("could not flush online time cache", ex);
        }
    }

    public CommonLogger getLogger() {
        return logger;
    }

    public PluginScheduler getScheduler() {
        return scheduler;
    }

    public CommonServer getServer() {
        return server;
    }

    public Configuration getConfig() {
        return config;
    }

    public Localization getLocalization() {
        return localization;
    }

    public TimeParser getParser() {
        return parser;
    }

    public NameStorage getNameStorage() {
        return nameStorage;
    }

    public AccumulatingTimeStorage getTimeStorage() {
        return timeStorage;
    }

    public String getPluginVersion() {
        return pluginVersion;
    }

    public AfkStatusProvider getAfkStatusProvider() {
        return afkStatusProvider;
    }
}
