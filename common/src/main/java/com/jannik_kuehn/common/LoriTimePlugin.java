package com.jannik_kuehn.common;

import com.jannik_kuehn.common.api.LoriTimePlayerConverter;
import com.jannik_kuehn.common.api.common.CommonServer;
import com.jannik_kuehn.common.api.logger.LoggerFactory;
import com.jannik_kuehn.common.api.logger.LoriTimeLogger;
import com.jannik_kuehn.common.api.scheduler.PluginScheduler;
import com.jannik_kuehn.common.api.storage.AccumulatingTimeStorage;
import com.jannik_kuehn.common.api.storage.NameStorage;
import com.jannik_kuehn.common.config.Configuration;
import com.jannik_kuehn.common.config.FileManager;
import com.jannik_kuehn.common.config.localization.Localization;
import com.jannik_kuehn.common.exception.ConfigurationException;
import com.jannik_kuehn.common.exception.StorageException;
import com.jannik_kuehn.common.module.afk.AfkHandling;
import com.jannik_kuehn.common.module.afk.AfkStatusProvider;
import com.jannik_kuehn.common.module.updater.UpdateCheck;
import com.jannik_kuehn.common.storage.DataStorageManager;
import com.jannik_kuehn.common.utils.TimeParser;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The {@link LoriTimePlugin} is the main class of the plugin.
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.UseProperClassLoader",
        "PMD.ConfusingTernary", "PMD.LiteralsFirstInComparisons", "PMD.AssignmentToNonFinalStatic", "PMD.TooManyMethods"})
public class LoriTimePlugin {
    /**
     * The {@link LoriTimePlugin} instance.
     */
    private static LoriTimePlugin instance;

    /**
     * The {@link LoggerFactory} instance.
     */
    private final LoggerFactory loggerFactory;

    /**
     * The {@link LoriTimeLogger} instance.
     */
    private final LoriTimeLogger log;

    /**
     * The {@link CommonServer} instance.
     */
    private final CommonServer server;

    /**
     * The {@link PluginScheduler} instance.
     */
    private final PluginScheduler scheduler;

    /**
     * The data folder of the plugin.
     */
    private final File dataFolder;

    /**
     * The {@link FileManager} instance.
     */
    private final FileManager fileManager;

    /**
     * The {@link DataStorageManager} instance.
     */
    private final DataStorageManager dataStorageManager;

    /**
     * The {@link LoriTimePlayerConverter} instance.
     */
    private final LoriTimePlayerConverter playerConverter;

    /**
     * The {@link Configuration} instance.
     */
    private Configuration config;

    /**
     * The {@link Localization} instance.
     */
    private Localization localization;

    /**
     * The {@link TimeParser} instance.
     */
    private TimeParser parser;

    /**
     * The {@link AfkStatusProvider} instance.
     */
    private AfkStatusProvider afkStatusProvider;

    /**
     * {@code true} if an error occurred and the plugin should be
     */
    private boolean errorDisable;

    /**
     * The {@link UpdateCheck} instance.
     */
    private UpdateCheck updateCheck;

    /**
     * Creates a new {@link LoriTimePlugin} instance.
     *
     * @param dataFolder  the {@link File} data folder where the plugin files will be.
     * @param scheduler   the {@link PluginScheduler} instance.
     * @param server      the {@link CommonServer} instance.
     * @param loggerTopic the logger topic.
     */
    public LoriTimePlugin(final File dataFolder, final PluginScheduler scheduler, final CommonServer server, final String loggerTopic) {
        instance = this;
        this.dataFolder = dataFolder;
        this.scheduler = scheduler;
        this.server = server;
        this.errorDisable = false;

        this.loggerFactory = new LoggerFactory(this);
        this.log = loggerFactory.create(LoriTimePlugin.class, loggerTopic);

        this.fileManager = new FileManager(loggerFactory, dataFolder);
        this.dataStorageManager = new DataStorageManager(this, dataFolder);
        this.playerConverter = new LoriTimePlayerConverter(loggerFactory, this);
    }

    /**
     * Getter of the {@link LoriTimePlugin} instance.
     *
     * @return the {@link LoriTimePlugin} instance.
     */
    public static LoriTimePlugin getInstance() {
        return instance;
    }

    /**
     * Enables the plugin-Core.
     */
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

    /**
     * Enables the afk feature.
     *
     * @param afkHandling the {@link AfkHandling} to use.
     */
    public void enableAfkFeature(final AfkHandling afkHandling) {
        afkStatusProvider = new AfkStatusProvider(this, afkHandling);
    }

    /**
     * Checks if the plugin is disabled due to an error.
     *
     * @return {@code true} if the plugin is disabled, otherwise {@code false}.
     */
    public boolean isMultiSetupEnabled() {
        return config.getBoolean("multiSetup.enabled", false);
    }

    /**
     * Checks if the afk feature is enabled.
     *
     * @return {@code true} if the afk feature is enabled, otherwise {@code false}.
     */
    public boolean isAfkEnabled() {
        return config.getBoolean("afk.enabled", false);
    }

    /**
     * Disables the plugin.
     */
    public void disable() {
        updateCheck.stopCheck();
        dataStorageManager.disableCache();
        dataStorageManager.closeStorages();
    }

    /**
     * Reloads the plugin.
     */
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

        final Configuration localizationFile;
        try {
            this.config = fileManager.getConfiguration(fileManager.getOrCreateFile(dataFolder.toString(), "config.yml", true));
            localizationFile = fileManager.getConfiguration(fileManager.getOrCreateFile(dataFolder.toString(), config.getString("general.language", "en") + ".yml", true));
            fileManager.startBackup();
        } catch (final ConfigurationException e) {
            log.error("An error occurred while loading the config file.", e);
            errorDisable = true;
            return;
        }

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

    /**
     * Getter of the {@link LoggerFactory}.
     *
     * @return the {@link LoggerFactory}.
     */
    public LoggerFactory getLoggerFactory() {
        return loggerFactory;
    }

    /**
     * Getter of the {@link LoriTimeLogger}.
     *
     * @return the {@link LoriTimeLogger}.
     */
    public PluginScheduler getScheduler() {
        return scheduler;
    }

    /**
     * Getter of the {@link LoriTimeLogger}.
     *
     * @return the {@link LoriTimeLogger}.
     */
    public CommonServer getServer() {
        return server;
    }

    /**
     * Getter of the {@link LoriTimeLogger}.
     *
     * @return the {@link LoriTimeLogger}.
     */
    public Configuration getConfig() {
        return config;
    }

    /**
     * Getter of the {@link Localization}.
     *
     * @return the {@link Localization}.
     */
    public Localization getLocalization() {
        return localization;
    }

    /**
     * Getter of the {@link TimeParser}.
     *
     * @return the {@link TimeParser}.
     */
    public TimeParser getParser() {
        return parser;
    }

    /**
     * Getter of the {@link NameStorage}.
     *
     * @return the {@link NameStorage}.
     */
    public NameStorage getNameStorage() {
        return dataStorageManager.getNameStorage();
    }

    /**
     * Getter of the {@link AccumulatingTimeStorage}.
     *
     * @return the {@link AccumulatingTimeStorage}.
     */
    public AccumulatingTimeStorage getTimeStorage() {
        return dataStorageManager.getTimeStorage();
    }

    /**
     * Getter of the {@link AfkStatusProvider}.
     *
     * @return the {@link AfkStatusProvider}.
     */
    public AfkStatusProvider getAfkStatusProvider() {
        return afkStatusProvider;
    }

    /**
     * Getter of the {@link UpdateCheck}.
     *
     * @return the {@link UpdateCheck}.
     */
    public UpdateCheck getUpdateCheck() {
        return updateCheck;
    }

    /**
     * Getter of the {@link FileManager}.
     *
     * @return the {@link FileManager}.
     */
    public FileManager getFileManager() {
        return fileManager;
    }

    /**
     * Getter of the {@link DataStorageManager}.
     *
     * @return the {@link DataStorageManager}.
     */
    public DataStorageManager getDataStorageManager() {
        return dataStorageManager;
    }

    /**
     * Getter of the {@link LoriTimePlayerConverter}.
     *
     * @return the {@link LoriTimePlayerConverter}.
     */
    public LoriTimePlayerConverter getPlayerConverter() {
        return playerConverter;
    }
}
