package com.jannik_kuehn.common;

import com.jannik_kuehn.common.api.LoriTimePlayerConverter;
import com.jannik_kuehn.common.api.common.CommonServer;
import com.jannik_kuehn.common.api.logger.LoggerFactory;
import com.jannik_kuehn.common.api.logger.LoriTimeLogger;
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings({"PMD.CommentRequired", "PMD.AvoidDuplicateLiterals", "PMD.UseProperClassLoader",
        "PMD.ConfusingTernary", "PMD.LiteralsFirstInComparisons", "PMD.AssignmentToNonFinalStatic", "PMD.TooManyMethods"})
public class LoriTimePlugin {
    private static LoriTimePlugin instance;

    private final LoggerFactory loggerFactory;

    private final LoriTimeLogger log;

    private final CommonServer server;

    private final PluginScheduler scheduler;

    private final File dataFolder;

    private final DataStorageManager dataStorageManager;

    private final LoriTimePlayerConverter playerConverter;

    private Configuration config;

    private Localization localization;

    private TimeParser parser;

    private AfkStatusProvider afkStatusProvider;

    private boolean errorDisable;

    private UpdateCheck updateCheck;

    public LoriTimePlugin(final File dataFolder, final PluginScheduler scheduler, final CommonServer server, final String loggerTopic) {
        instance = this;
        this.dataFolder = dataFolder;
        this.scheduler = scheduler;
        this.server = server;
        this.errorDisable = false;

        this.loggerFactory = new LoggerFactory(this);
        this.log = loggerFactory.create(LoriTimePlugin.class, loggerTopic);

        this.dataStorageManager = new DataStorageManager(this, dataFolder);
        this.playerConverter = new LoriTimePlayerConverter(loggerFactory, this);
    }

    public static LoriTimePlugin getInstance() {
        return instance;
    }

    public void enable() {
        loadOrCreateConfigs();
        log.debug("Enabling LoriTime main class");
        server.setServerMode(getServerModeFromConfig());
        updateCheck = new UpdateCheck(this);

        if (server.getServerMode().equalsIgnoreCase("master")) {
            enableAsMaster();
        }

        log.debug("Enabled main class of the plugin, enabling the rest of the plugin..");
    }

    private void enableAsMaster() {
        try {
            dataStorageManager.loadStorages();
        } catch (final StorageException e) {
            log.error("An error occurred while enabling the storage", e);
            errorDisable = true;
            return;
        }

        if (errorDisable) {
            log.error("Disabling the plugin because of an issue.");
            disable();
            return;
        }

        updateCheck.startCheck();
        dataStorageManager.startCache();
    }

    private String getServerModeFromConfig() {
        final String serverMode;
        if (!isMultiSetupEnabled()) {
            serverMode = "master";
        } else {
            serverMode = config.getString("multiSetup.mode", "master");
        }
        return serverMode;
    }

    public void enableAfkFeature(final AfkHandling afkHandling) {
        afkStatusProvider = new AfkStatusProvider(this, afkHandling);
    }

    public boolean isMultiSetupEnabled() {
        return config.getBoolean("multiSetup.enabled", false);
    }

    public boolean isAfkEnabled() {
        return config.getBoolean("afk.enabled", false);
    }

    public void disable() {
        updateCheck.stopCheck();
        dataStorageManager.disableCache();
        dataStorageManager.closeStorages();
    }

    public void reload() {
        updateCheck.stopCheck();
        config.reload();
        localization.reloadTranslation();
        afkStatusProvider.reloadConfigValues();
        updateCheck.startCheck();

        reloadMasteredFunctions();
    }

    private void reloadMasteredFunctions() {
        dataStorageManager.disableCache();
        dataStorageManager.reloadStorages();
        dataStorageManager.startCache();
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
            log.error("The plugins localization and config didn't load correctly. Pls delete the files and try again! Stop starting plugin..");
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
            log.error("Could not create time parser.", ex);
        }
    }

    public Configuration getOrCreateFile(final String folder, final String fileName, final boolean needCopy) {
        final File file = new File(folder, fileName);
        boolean created = false;
        if (!file.exists()) {
            try {
                created = file.createNewFile();
                log.info("Creating new File '" + fileName + "'.");
                if (needCopy) {
                    copyDataFromResource(file.toPath(), fileName);
                }
            } catch (final IOException e) {
                log.error("An exception occurred while creating the file '" + fileName + "' on startup.", e);
            }
        }
        final Configuration configurationFile = new YamlConfiguration(file.toString());
        if (!configurationFile.isLoaded()) {
            log.error("An issue occurred while loading the file '" + fileName + "'. The File is null, there should be data.");
            return null;
        }
        if (created) {
            log.info("The file '" + fileName + "' was created successfully.");
        }
        log.info("Successfully loaded '" + fileName + "'.");
        return configurationFile;
    }

    private void copyDataFromResource(final Path configFile, final String nameFromSourceOfReplacement) {
        try {
            Files.copy(this.getClass().getClassLoader().getResource(nameFromSourceOfReplacement).openStream(), configFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (final IOException e) {
            log.error("Could not copy the file content to the file '" + nameFromSourceOfReplacement + "'. Pls delete the file and try again.", e);
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
                log.warn("dangerous identifier definition in language file. Path: " + "unit." + unit + "identifier: " + content.toString());
                units.add(content.toString());
            }
        }
        return units.toArray(new String[0]);
    }

    public LoggerFactory getLoggerFactory() {
        return loggerFactory;
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

    public AfkStatusProvider getAfkStatusProvider() {
        return afkStatusProvider;
    }

    public UpdateCheck getUpdateCheck() {
        return updateCheck;
    }

    public DataStorageManager getDataStorageManager() {
        return dataStorageManager;
    }

    public LoriTimePlayerConverter getPlayerConverter() {
        return playerConverter;
    }
}
