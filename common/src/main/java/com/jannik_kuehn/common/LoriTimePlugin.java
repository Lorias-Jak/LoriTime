package com.jannik_kuehn.common;

import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import com.jannik_kuehn.common.command.completion.RecentPlayerSuggestionCache;
import com.jannik_kuehn.common.command.completion.ScopeSuggestionCache;
import com.jannik_kuehn.common.command.config.CommandAliasConfig;
import com.jannik_kuehn.common.config.Configuration;
import com.jannik_kuehn.common.config.FileManager;
import com.jannik_kuehn.common.config.localization.ConfiguredDefaultLanguageSelector;
import com.jannik_kuehn.common.config.localization.LanguageSelector;
import com.jannik_kuehn.common.config.localization.Localization;
import com.jannik_kuehn.common.exception.ConfigurationException;
import com.jannik_kuehn.common.exception.StorageException;
import com.jannik_kuehn.common.module.afk.AfkHandling;
import com.jannik_kuehn.common.module.afk.AfkStatusProvider;
import com.jannik_kuehn.common.module.updater.UpdateSourceHandler;
import com.jannik_kuehn.common.module.updater.Updater;
import com.jannik_kuehn.common.module.updater.download.Downloader;
import com.jannik_kuehn.common.module.updater.download.sources.DevUpdateSource;
import com.jannik_kuehn.common.module.updater.download.sources.GitHubReleaseSource;
import com.jannik_kuehn.common.module.updater.download.sources.ModrinthReleaseSource;
import com.jannik_kuehn.common.module.updater.download.sources.ReleaseUpdateSource;
import com.jannik_kuehn.common.module.updater.version.Version;
import com.jannik_kuehn.common.platform.CommonServer;
import com.jannik_kuehn.common.player.LoriTimePlayerConverter;
import com.jannik_kuehn.common.scheduler.PluginScheduler;
import com.jannik_kuehn.common.storage.DataStorageManager;
import com.jannik_kuehn.common.storage.StorageMigrationService;
import com.jannik_kuehn.common.storage.contract.AdminStorageMaintenance;
import com.jannik_kuehn.common.storage.contract.StatisticsStorage;
import com.jannik_kuehn.common.storage.contract.TimeAccumulator;
import com.jannik_kuehn.common.storage.contract.UnifiedStorage;
import com.jannik_kuehn.common.storage.model.AfkPeriodEndReason;
import com.jannik_kuehn.common.storage.model.StorageMode;
import com.jannik_kuehn.common.utils.TimeParser;

import java.io.File;
import java.time.Instant;
import java.time.InstantSource;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The {@link LoriTimePlugin} is the main class of the plugin.
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.CouplingBetweenObjects", "PMD.GodClass",
        "PMD.TooManyMethods"})
public class LoriTimePlugin {
    /**
     * The {@link LoggerFactory} instance.
     */
    private final LoggerFactory loggerFactory;

    /**
     * The {@link WrappedLogger} instance.
     */
    private final WrappedLogger log;

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
     * Players whose next leave/disconnect should be persisted as AFK-caused.
     */
    private final Set<UUID> afkKickMarkers;

    /**
     * Known player names observed during runtime, used by synchronous suggestion paths.
     */
    private final Set<String> knownPlayerNames;

    /**
     * Recent player suggestions used by synchronous command completion.
     */
    private final RecentPlayerSuggestionCache recentPlayerSuggestionCache;

    /**
     * Server/world suggestions used by synchronous command completion.
     */
    private final ScopeSuggestionCache scopeSuggestionCache;

    /**
     * The {@link Configuration} instance.
     */
    private Configuration config;

    /**
     * The command configuration instance.
     */
    private Configuration commandConfig;

    /**
     * The command alias/profile configuration reader.
     */
    private CommandAliasConfig commandAliasConfig;

    /**
     * The {@link Localization} instance.
     */
    private Localization localization;

    /**
     * Selects the language used for sender-facing messages.
     */
    private LanguageSelector languageSelector;

    /**
     * The {@link TimeParser} instance.
     */
    private TimeParser parser;

    /**
     * The {@link AfkStatusProvider} instance.
     */
    private AfkStatusProvider afkStatusProvider;

    /**
     * The {@link Updater} instance.
     */
    private Updater updater;

    /**
     * {@code true} if an error occurred and the plugin should be
     */
    private boolean errorDisable;

    /**
     * Creates a new {@link LoriTimePlugin} instance.
     *
     * @param dataFolder  the {@link File} data folder where the plugin files will be.
     * @param scheduler   the {@link PluginScheduler} instance.
     * @param server      the {@link CommonServer} instance.
     * @param loggerTopic the logger topic.
     */
    public LoriTimePlugin(final LoggerFactory loggerFactory, final File dataFolder, final PluginScheduler scheduler, final CommonServer server, final String loggerTopic) {
        this.dataFolder = dataFolder;
        this.scheduler = scheduler;
        this.server = server;
        this.errorDisable = false;

        this.loggerFactory = loggerFactory;
        this.log = loggerFactory.create(LoriTimePlugin.class, loggerTopic);

        this.fileManager = new FileManager(loggerFactory, dataFolder);
        this.dataStorageManager = new DataStorageManager(this, dataFolder);
        this.playerConverter = new LoriTimePlayerConverter(loggerFactory, this);
        this.afkKickMarkers = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.knownPlayerNames = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.recentPlayerSuggestionCache = new RecentPlayerSuggestionCache();
        this.scopeSuggestionCache = new ScopeSuggestionCache();
    }

    /**
     * Enables the plugin-Core.
     */
    public void enable() {
        loadOrCreateConfigs();
        if (errorDisable) {
            log.error("Disabling the plugin because of an issue.");
            disable();
            return;
        }
        log.debug("Enabling LoriTime main class");
        server.setServerMode(getServerModeFromConfig());
        setupUpdater();

        if (!StorageMode.SLAVE.configValue().equalsIgnoreCase(server.getServerMode())) {
            enableCanonicalStorage();
        }

        log.debug("Enabled main class of the plugin, enabling the rest of the plugin..");
    }

    private void enableCanonicalStorage() {
        try {
            final StorageMigrationService storageMigrationService = new StorageMigrationService(this, dataFolder);
            storageMigrationService.migrateIfNecessary();
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

        dataStorageManager.startCache();
        refreshRecentPlayerSuggestions();
        refreshScopeSuggestions();
    }

    private void setupUpdater() {
        if (config.getBoolean("updater.checkForUpdates", true)) {
            final List<DevUpdateSource> devSource = List.of();
            final ReleaseUpdateSource modrinthSource = new ModrinthReleaseSource("https://api.modrinth.com/v2/project/loritime/version", server.getPluginJarName());
            final ReleaseUpdateSource gitHubReleaseSource = new GitHubReleaseSource("https://api.github.com/repos/lorias-jak/loritime", server.getPluginJarName());
            final List<ReleaseUpdateSource> releaseSource = List.of(gitHubReleaseSource, modrinthSource);

            final UpdateSourceHandler updateSourceHandler = new UpdateSourceHandler(loggerFactory.create(UpdateSourceHandler.class),
                    releaseSource, devSource);
            updater = new Updater(loggerFactory.create(Updater.class), new Version(server.getPluginVersion()), updateSourceHandler,
                    this, InstantSource.system(), new Downloader(new File(dataFolder.getParentFile().toString() + "/update")));
            updater.search();
        }
    }

    private String getServerModeFromConfig() {
        try {
            return StorageMode.parse(config.getString("multiSetup.mode", StorageMode.STANDALONE.configValue())).configValue();
        } catch (final IllegalArgumentException ex) {
            log.warn("Invalid multiSetup.mode configured, falling back to standalone.", ex);
            return StorageMode.STANDALONE.configValue();
        }
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
        return !StorageMode.STANDALONE.configValue().equalsIgnoreCase(getServerModeFromConfig());
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
     * Marks a player whose disconnect is caused by AFK kick enforcement.
     *
     * @param uniqueId player UUID
     */
    public void markAfkKick(final UUID uniqueId) {
        if (uniqueId != null) {
            afkKickMarkers.add(uniqueId);
        }
    }

    /**
     * Consumes the AFK kick marker for a player.
     *
     * @param uniqueId player UUID
     * @return true when the next disconnect should be recorded as AFK-caused
     */
    public boolean consumeAfkKick(final UUID uniqueId) {
        return uniqueId != null && afkKickMarkers.remove(uniqueId);
    }

    /**
     * Remembers a player name observed by a runtime event or cache lookup.
     *
     * @param uniqueId player UUID
     * @param name     player name
     */
    public void rememberPlayerName(final UUID uniqueId, final String name) {
        if (uniqueId != null && name != null && !name.isBlank()) {
            knownPlayerNames.add(name);
            recentPlayerSuggestionCache.remember(uniqueId, name);
        }
    }

    /**
     * Remembers a runtime-observed scope.
     *
     * @param serverName server name
     * @param worldName  world name
     */
    public void rememberScope(final String serverName, final String worldName) {
        scopeSuggestionCache.remember(serverName, worldName);
    }

    /**
     * Gets known runtime player names for synchronous suggestion paths.
     *
     * @return immutable snapshot of known player names
     */
    public Set<String> getKnownPlayerNames() {
        final Set<String> names = new HashSet<>(knownPlayerNames);
        names.addAll(recentPlayerSuggestionCache.names());
        return Set.copyOf(names);
    }

    /**
     * Gets the recent player suggestion cache.
     *
     * @return recent player suggestion cache
     */
    public RecentPlayerSuggestionCache getRecentPlayerSuggestionCache() {
        return recentPlayerSuggestionCache;
    }

    /**
     * Gets the scope suggestion cache.
     *
     * @return scope suggestion cache
     */
    public ScopeSuggestionCache getScopeSuggestionCache() {
        return scopeSuggestionCache;
    }

    /**
     * Disables the plugin.
     */
    public void disable() {
        dataStorageManager.disableCache();
        dataStorageManager.closeStorages();
    }

    /**
     * Reloads the plugin.
     */
    public void reload() {
        config.reload();
        commandConfig.reload();
        final String configuredLanguage = config.getString("general.language", "en-us");
        try {
            fileManager.getOrCreateLanguageFile(configuredLanguage);
        } catch (final ConfigurationException ex) {
            log.error("An error occurred while loading the configured language file.", ex);
            errorDisable = true;
        }
        languageSelector = new ConfiguredDefaultLanguageSelector(configuredLanguage);
        localization.reload(configuredLanguage);
        rebuildTimeParser();
        if (afkStatusProvider != null) {
            afkStatusProvider.reloadConfigValues();
            afkStatusProvider.restartAfkCheck();
        }

        reloadMasteredFunctions();
        refreshRecentPlayerSuggestions();
        refreshScopeSuggestions();
    }

    private void refreshRecentPlayerSuggestions() {
        if (StorageMode.SLAVE.configValue().equalsIgnoreCase(server.getServerMode())) {
            return;
        }
        final long recentDays = config.getLong("command.completion.recentPlayersDays", 30L);
        scheduler.runAsyncOnce(() -> {
            try {
                recentPlayerSuggestionCache.replaceRecentIdentities(getStorage().getRecentPlayerIdentities(recentDays));
            } catch (final StorageException ex) {
                log.warn("Could not refresh recent player suggestions.", ex);
            }
        });
    }

    private void refreshScopeSuggestions() {
        if (StorageMode.SLAVE.configValue().equalsIgnoreCase(server.getServerMode())) {
            return;
        }
        scheduler.runAsyncOnce(() -> {
            try {
                scopeSuggestionCache.replaceStoredNames(getStorage().getKnownServerNames(),
                        getStorage().getKnownWorldNamesByServer());
            } catch (final StorageException ex) {
                log.warn("Could not refresh scope suggestions.", ex);
            }
        });
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

        try {
            this.config = fileManager.getConfiguration(fileManager.getOrCreateFile(dataFolder.toString(), "config.yml", true));
            this.commandConfig = fileManager.getConfiguration(fileManager.getOrCreateFile(dataFolder.toString(), "commands.yml", true));
            this.commandAliasConfig = new CommandAliasConfig(commandConfig);
            fileManager.ensureBundledFallbackLanguage();
            fileManager.getOrCreateLanguageFile(config.getString("general.language", "en-us"));
            new StorageMigrationService(this, dataFolder).addLegacyFilesToStartupBackup();
            fileManager.startBackup();
        } catch (final ConfigurationException e) {
            log.error("An error occurred while loading the config file.", e);
            errorDisable = true;
            return;
        }

        this.localization = new Localization(loggerFactory.create(Localization.class), dataFolder,
                config.getString("general.language", "en-us"));
        this.languageSelector = new ConfiguredDefaultLanguageSelector(config.getString("general.language", "en-us"));

        if (!config.isLoaded() || localization.healthState() == Localization.HealthState.FAILED) {
            log.error("The plugins localization and config didn't load correctly. Pls delete the files and try again! Stop starting plugin..");
            errorDisable = true;
            return;
        }
        rebuildTimeParser();
    }

    private void rebuildTimeParser() {
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
        final List<String> identifier = langConfig.getStringList("unit." + unit + ".identifier");
        final Set<String> units = new HashSet<>();
        units.add(singular);
        units.add(plural);
        units.addAll(identifier);
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
     * Getter of the {@link PluginScheduler}.
     *
     * @return the {@link PluginScheduler}.
     */
    public PluginScheduler getScheduler() {
        return scheduler;
    }

    /**
     * Getter of the {@link CommonServer}.
     *
     * @return the {@link CommonServer}.
     */
    public CommonServer getServer() {
        return server;
    }

    /**
     * Getter of the {@link Configuration}.
     *
     * @return the {@link Configuration}.
     */
    public Configuration getConfig() {
        return config;
    }

    /**
     * Getter of the command configuration.
     *
     * @return the command configuration.
     */
    public Configuration getCommandConfig() {
        return commandConfig;
    }

    /**
     * Getter of the command alias configuration.
     *
     * @return command alias configuration.
     */
    public CommandAliasConfig getCommandAliasConfig() {
        return commandAliasConfig;
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
     * Getter of the sender language selector.
     *
     * @return language selector
     */
    public LanguageSelector getLanguageSelector() {
        return languageSelector;
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
     * Getter of the {@link UnifiedStorage}.
     *
     * @return the {@link UnifiedStorage}.
     */
    public UnifiedStorage getStorage() {
        return dataStorageManager.getStorage();
    }

    /**
     * Getter of the {@link TimeAccumulator}.
     *
     * @return the {@link TimeAccumulator}.
     */
    public TimeAccumulator getAccumulator() {
        return dataStorageManager.getAccumulator();
    }

    /** Returns statistics and AFK-history storage on canonical runtimes. */
    public Optional<StatisticsStorage> getStatisticsStorage() {
        return dataStorageManager.getStatisticsStorage();
    }

    /** Closes a canonical AFK period if one is open. */
    public void closeAfkPeriod(final UUID playerId, final AfkPeriodEndReason reason) {
        getStatisticsStorage().ifPresent(storage -> {
            try {
                storage.closeAfkPeriod(playerId, Instant.now(), reason);
            } catch (final StorageException ex) {
                log.error("Could not close AFK period for " + playerId + " as " + reason, ex);
            }
        });
    }

    /**
     * Returns optional admin storage maintenance support for future admin commands.
     *
     * @return maintenance contract when this runtime owns supported canonical storage
     */
    public Optional<AdminStorageMaintenance> getAdminStorageMaintenance() {
        return dataStorageManager.getAdminStorageMaintenance();
    }

    /**
     * Returns whether this runtime owns canonical storage.
     *
     * @return true when canonical storage operations may run locally
     */
    public boolean ownsCanonicalStorage() {
        return dataStorageManager.ownsCanonicalStorage();
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

    public Updater getUpdater() {
        return updater;
    }
}
