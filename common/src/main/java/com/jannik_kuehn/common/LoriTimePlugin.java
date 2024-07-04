package com.jannik_kuehn.common;

import com.jannik_kuehn.common.api.common.CommonLogger;
import com.jannik_kuehn.common.api.common.CommonServer;
import com.jannik_kuehn.common.api.scheduler.PluginScheduler;
import com.jannik_kuehn.common.api.storage.AccumulatingTimeStorage;
import com.jannik_kuehn.common.api.storage.NameStorage;
import com.jannik_kuehn.common.config.Configuration;
import com.jannik_kuehn.common.config.YamlConfiguration;
import com.jannik_kuehn.common.config.localization.Localization;
import com.jannik_kuehn.common.exception.StorageException;
import com.jannik_kuehn.common.module.afk.AfkHandling;
import com.jannik_kuehn.common.module.afk.AfkStatusProvider;
import com.jannik_kuehn.common.module.updater.UpdateCheck;
import com.jannik_kuehn.common.storage.DataStorageManager;
import com.jannik_kuehn.common.utils.TimeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LoriTimePlugin {
    private static final Logger log = LoggerFactory.getLogger(LoriTimePlugin.class);

    private static LoriTimePlugin instance;

    private final CommonLogger logger;

    private final CommonServer server;

    private final PluginScheduler scheduler;

    private final File dataFolder;

    private final String pluginVersion;

    private final DataStorageManager dataStorageManager;

    private Configuration config;

    private Localization localization;

    private TimeParser parser;

    private AfkStatusProvider afkStatusProvider;

    private boolean errorDisable;

    private UpdateCheck updateCheck;

    public LoriTimePlugin(final CommonLogger logger, final File dataFolder, final PluginScheduler scheduler, final CommonServer server) {
        instance = this;
        this.logger = logger;
        this.dataFolder = dataFolder;
        this.scheduler = scheduler;
        this.server = server;
        this.errorDisable = false;
        this.pluginVersion = "LoriTime-1.4.0";
        this.dataStorageManager = new DataStorageManager(this, dataFolder);
    }

    public static LoriTimePlugin getInstance() {
        return instance;
    }

    public void enable() {
        loadOrCreateConfigs();
        try {
            dataStorageManager.loadStorages();
        } catch (final StorageException e) {
            logger.error("An error occurred while enabling the storage", e);
            errorDisable = true;
            return;
        }

        if (errorDisable) {
            logger.severe("Disabling the plugin because of an issue.");
            disable();
            return;
        }
        server.setServerMode(getServerModeFromConfig());

        if (server.getServerMode().equalsIgnoreCase("master")) {
            updateCheck = new UpdateCheck(this);
            updateCheck.startCheck();
        }
        dataStorageManager.startCache();
    }

    private String getServerModeFromConfig() {
        final String serverMode;
        if (!config.getBoolean("multiSetup.enabled", false)) {
            serverMode = "master";
        } else {
            serverMode = config.getString("multiSetup.mode", "master");
        }
        return serverMode;
    }

    public void enableAfkFeature(final AfkHandling afkHandling) {
        afkStatusProvider = new AfkStatusProvider(this, afkHandling);
    }

    public boolean isAfkEnabled() {
        return config.getBoolean("afk.enabled", false);
    }

    public void disable() {
        updateCheck.stopCheck();
        dataStorageManager.disableCache();
        dataStorageManager.closeStorages();
    }

    public void reload() throws StorageException {
        updateCheck.stopCheck();
        config.reload();
        localization.reloadTranslation();
        dataStorageManager.disableCache();
        dataStorageManager.reloadStorages();
        afkStatusProvider.reloadConfigValues();
        dataStorageManager.startCache();
        updateCheck.startCheck();
    }

    private void loadOrCreateConfigs() {
        final File directory = new File(dataFolder.toString());
        if (!directory.exists()) {
            final boolean created = directory.mkdir();
            if (!created) {
                log.error("Could not create the data folder for plugin.");
            }
        }

        this.config = getOrCreateFile(dataFolder.toString(), "config.yml", true);
        final Configuration localizationFile = getOrCreateFile(dataFolder.toString(), config.getString("general.language", "en") + ".yml", true);
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
        } catch (final IllegalArgumentException ex) {
            logger.error("Could not create time parser.", ex);
        }
    }

    public Configuration getOrCreateFile(final String folder, final String fileName, final boolean needCopy) {
        final File file = new File(folder, fileName);
        boolean created = false;
        if (!file.exists()) {
            try {
                created = file.createNewFile();
                logger.info("Creating new File '" + fileName + "'.");
                if (needCopy) {
                    copyDataFromResource(file.toPath(), fileName);
                }
            } catch (final IOException e) {
                logger.error("An exception occurred while creating the file '" + fileName + "' on startup.", e);
            }
        }
        final Configuration configurationFile = new YamlConfiguration(file.toString());
        if (!configurationFile.isLoaded()) {
            logger.severe("An issue occurred while loading the file '" + fileName + "'. The File is null, there should be data.");
            return null;
        }
        if (created) {
            logger.info("The file '" + fileName + "' was created successfully.");
        }
        logger.info("Successfully loaded '" + fileName + "'.");
        return configurationFile;
    }

    private void copyDataFromResource(final Path configFile, final String nameFromSourceOfReplacement) {
        try {
            Files.copy(this.getClass().getClassLoader().getResource(nameFromSourceOfReplacement).openStream(), configFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (final IOException e) {
            logger.error("Could not copy the file content to the file '" + nameFromSourceOfReplacement + "'. Pls delete the file and try again.", e);
        }
    }

    private String[] getUnits(final Localization langConfig, final String unit) {
        final String singular = langConfig.getRawMessage("unit." + unit + ".singular");
        final String plural = langConfig.getRawMessage("unit." + unit + ".plural");
        final List<?> identifier = langConfig.getLangArray("unit." + unit + ".identifier");
        final Set<String> units = new HashSet<>();
        units.add(singular);
        units.add(plural);
        for (final Object content : identifier) {
            if (content instanceof String) {
                units.add((String) content);
            } else {
                logger.warning("dangerous identifier definition in language file. Path: " + "unit." + unit + "identifier: " + content.toString());
                units.add(content.toString());
            }
        }
        return units.toArray(new String[0]);
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
        return dataStorageManager.getNameStorage();
    }

    public AccumulatingTimeStorage getTimeStorage() {
        return dataStorageManager.getTimeStorage();
    }

    public String getPluginVersion() {
        return pluginVersion;
    }

    public AfkStatusProvider getAfkStatusProvider() {
        return afkStatusProvider;
    }

    public UpdateCheck getUpdateCheck() {
        return updateCheck;
    }

    public DataStorageManager getDataStorageManager() {
        return dataStorageManager;
    }
}
