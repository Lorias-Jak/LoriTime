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
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LoriTimePlugin {
    private static final Logger log = LoggerFactory.getLogger(LoriTimePlugin.class);

    private static LoriTimePlugin instance;

    private final CommonLogger logger;

    private final CommonServer server;

    private final PluginScheduler scheduler;

    private final File dataFolder;

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
        this.dataStorageManager = new DataStorageManager(this, dataFolder);
    }

    public static LoriTimePlugin getInstance() {
        return instance;
    }

    public void enable() {
        loadOrCreateConfigs();
        server.setServerMode(getServerModeFromConfig());
        updateCheck = new UpdateCheck(this);

        if (server.getServerMode().equalsIgnoreCase("master")) {
            enableAsMaster();
        }
    }

    private void enableAsMaster() {
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
        } else {
            // Aktualisiere die Konfiguration, indem fehlende Schlüssel ergänzt werden, ohne benutzerdefinierte Werte zu überschreiben
            try {
                updateConfigFromResource(file.getPath(), fileName);
            } catch (final IOException e) {
                logger.error("An error occurred while updating the configuration file '" + fileName + "'.", e);
            }
        }

        logger.info("Successfully loaded '" + fileName + "'.");
        return configurationFile;
    }

    private void copyDataFromResource(final Path configFile, final String nameFromSourceOfReplacement) {
        try {
            Files.copy(this.getClass().getClassLoader().getResource(nameFromSourceOfReplacement).openStream(), configFile, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Successfully copied data from resource to '" + configFile.getFileName() + "'.");
        } catch (final IOException e) {
            logger.error("Could not copy the file content to the file '" + nameFromSourceOfReplacement + "'. Please delete the file and try again.", e);
        }
    }

    private void updateConfigFromResource(final String oldConfigPath, final String resourceFileName) throws IOException {
        final File oldConfigFile = new File(oldConfigPath);

        // Schritt 1: Lese die alte Konfiguration und speichere die Key-Value-Paare in einer Map
        final Map<String, Object> oldConfig = loadConfigToMap(oldConfigFile);

        // Schritt 2: Lese die neue Konfiguration aus der Ressource zeilenweise
        List<String> newConfigLines;
        final Path tempFile = Files.createTempFile("temp", ".yml");
        try (InputStream resourceStream = this.getClass().getClassLoader().getResourceAsStream(resourceFileName)) {
            if (resourceStream == null) {
                throw new IOException("Resource file '" + resourceFileName + "' not found.");
            }
            Files.copy(resourceStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            newConfigLines = Files.readAllLines(tempFile);
        } finally {
            Files.deleteIfExists(tempFile);
        }

        // Schritt 3: Aktualisiere die neue Konfiguration, ohne benutzerdefinierte Werte zu überschreiben
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(oldConfigFile))) {
            for (final String line : newConfigLines) {
                final String trimmedLine = line.trim();

                if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
                    // Schreibe Kommentare und Leerzeilen unverändert
                    writer.write(line);
                } else if (trimmedLine.contains(":")) {
                    // Splitte die Zeile in Key und (evtl.) Value
                    final String[] keyValue = trimmedLine.split(":", 2);
                    final String key = keyValue[0].trim();

                    if (oldConfig.containsKey(key)) {
                        // Behalte den benutzerdefinierten Wert bei
                        writer.write(key + ": " + oldConfig.get(key));
                    } else {
                        // Schreibe den Standardwert aus der neuen Konfiguration
                        writer.write(line);
                    }
                } else {
                    // Schreibe Zeilen, die keinen Key-Value-Paar darstellen, unverändert
                    writer.write(line);
                }
                writer.newLine();
            }
        }
    }

    private Map<String, Object> loadConfigToMap(final File configFile) throws IOException {
        final Yaml yaml = new Yaml();
        final Map<String, Object> configMap = new HashMap<>();

        try (FileInputStream fis = new FileInputStream(configFile)) {
            final Map<String, Object> yamlMap = yaml.load(fis);

            if (yamlMap != null) {
                flattenMap("", yamlMap, configMap);
            }
        }

        return configMap;
    }

    @SuppressWarnings("unchecked")
    private void flattenMap(final String prefix, final Map<String, Object> source, final Map<String, Object> target) {
        for (final Map.Entry<String, Object> entry : source.entrySet()) {
            final String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();

            if (entry.getValue() instanceof Map) {
                flattenMap(key, (Map<String, Object>) entry.getValue(), target);
            } else {
                target.put(key, entry.getValue());
            }
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
