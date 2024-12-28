package com.jannik_kuehn.common.config;

import com.jannik_kuehn.common.api.logger.LoggerFactory;
import com.jannik_kuehn.common.api.logger.LoriTimeLogger;
import com.jannik_kuehn.common.config.backup.FileBackupManager;
import com.jannik_kuehn.common.exception.ConfigurationException;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The {@link FileManager} is responsible for managing the files of the plugin including updating and backing up.
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.GodClass"})
public class FileManager {

    /**
     * The {@link LoriTimeLogger} instance.
     */
    private final LoriTimeLogger log;

    /**
     * The data folder of the plugin.
     */
    private final File dataFolder;

    /**
     * The {@link FileBackupManager} instance.
     */
    private final FileBackupManager fileBackupManager;

    /**
     * Creates a new {@link FileManager} object.
     *
     * @param loggerFactory the {@link LoggerFactory} instance
     * @param dataFolder    the data folder of the plugin
     */
    @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
    public FileManager(final LoggerFactory loggerFactory, final File dataFolder) {
        this.log = loggerFactory.create(FileManager.class);
        this.dataFolder = dataFolder;

        final Configuration temp = tempLoadConfiguration();

        final boolean backupsEnabled = temp.getBoolean("backup.enabled", true);
        final int maxBackups = temp.getInt("backup.maxBackups", 5);
        this.fileBackupManager = new FileBackupManager(loggerFactory.create(FileBackupManager.class), dataFolder, backupsEnabled, maxBackups);
    }

    private Configuration tempLoadConfiguration() {
        Configuration tempConfig = null;
        File file = new File(dataFolder.toString(), "config.yml");
        try {
            if (doesFileNotExist(file)) {
                file = getOrCreateFile(dataFolder.toString(), "config.yml", true);
            }
            tempConfig = getConfiguration(file);
        } catch (final ConfigurationException e) {
            log.error("An error occurred while loading the backup manager configuration.", e);
        }
        return tempConfig;
    }

    @SuppressWarnings("PMD.UseProperClassLoader")
    private boolean checkFileForUpdate(final File oldFile, final String newFileName) throws ConfigurationException {
        final Yaml yaml = new Yaml();
        final Set<String> oldKeys;

        try (InputStream oldFileStream = Files.newInputStream(oldFile.toPath())) {
            final Map<String, Object> oldConfig = yaml.load(oldFileStream);
            oldKeys = extractKeys(oldConfig);
        } catch (final IOException e) {
            throw new ConfigurationException("An error occurred while loading the file: " + oldFile.getName() + " to check it for updates", e);
        }

        final Set<String> newKeys;
        try (InputStream resourceStream = getClass().getClassLoader().getResourceAsStream(newFileName)) {
            if (resourceStream == null) {
                throw new ConfigurationException("Resource file '" + newFileName + "' not found.");
            }
            final Map<String, Object> newConfig = yaml.load(resourceStream);
            newKeys = extractKeys(newConfig);
        } catch (final IOException e) {
            throw new ConfigurationException("An error occurred while loading the file: " + newFileName + " to check it for updates", e);
        }

        return !oldKeys.equals(newKeys);
    }

    private Set<String> extractKeys(final Map<String, Object> config) {
        final Set<String> keys = new HashSet<>();
        if (config != null) {
            flattenKeys("", config, keys);
        }
        return keys;
    }

    private void flattenKeys(final String parentKey, final Map<String, Object> map, final Set<String> keys) {
        for (final Map.Entry<String, Object> entry : map.entrySet()) {
            final String fullKey = parentKey.isEmpty() ? entry.getKey() : parentKey + "." + entry.getKey();
            keys.add(fullKey);

            if (entry.getValue() instanceof Map) {
                @SuppressWarnings("unchecked") final Map<String, Object> childMap = (Map<String, Object>) entry.getValue();
                flattenKeys(fullKey, childMap, keys);
            }
        }
    }

    /**
     * Returns a {@link File} object for the given folder and file name.
     *
     * @param folder   the folder where the file is located
     * @param fileName the name of the file
     * @param needCopy if {@code true} the file will be copied from the resources if it does not exist.
     *                 If it exists, it will be in the backup and automatically updated.
     * @return the {@link File} object.
     * @throws ConfigurationException if the file could not be created, copied or updated.
     */
    public File getOrCreateFile(final String folder, final String fileName, final boolean needCopy) throws ConfigurationException {
        final File file = new File(folder, fileName);
        if (doesFileNotExist(file) && needCopy) {
            createFile(file);
            createFromResources(file.toPath(), fileName);
        } else if (doesFileNotExist(file)) {
            createFile(file);
        } else if (needCopy && checkFileForUpdate(file, fileName)) {
            fileBackupManager.addFileToBackup(file);
            updateConfigFromResource(file, fileName);
        }
        return file;
    }

    private boolean doesFileNotExist(final File file) {
        return !file.exists();
    }

    private void createFile(final File file) throws ConfigurationException {
        try {
            file.createNewFile();
        } catch (final IOException e) {
            throw new ConfigurationException("An exception occurred while creating the file '" + file.getName() + "' on startup.", e);
        }
    }

    @SuppressWarnings("PMD.UseProperClassLoader")
    private void createFromResources(final Path createdFile, final String nameFromSourceOfReplacement) throws ConfigurationException {
        try {
            Files.copy(this.getClass().getClassLoader().getResource(nameFromSourceOfReplacement).openStream(), createdFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (final IOException e) {
            throw new ConfigurationException("Could not copy the file content to the file '" + nameFromSourceOfReplacement + "'. Pls delete the file and try again.", e);
        }
    }

    /**
     * Takes a {@link File} and returns a {@link Configuration} object.
     * The {@link File} has to be a .yml file
     *
     * @param fileToChange the {@link File} to convert to a {@link Configuration} object
     * @return the {@link Configuration} object
     * @throws ConfigurationException if the file could not be converted to a {@link Configuration} object
     */
    public Configuration getConfiguration(final File fileToChange) throws ConfigurationException {
        final Configuration configurationFile = new YamlConfiguration(fileToChange.toString());
        if (configurationFile.isLoaded()) {
            return configurationFile;
        } else {
            throw new ConfigurationException("An issue occurred while loading the file '" + fileToChange.getName() + "'. The File is null, there should be data.");
        }
    }

    @SuppressWarnings({"PMD.UseProperClassLoader", "PMD.CyclomaticComplexity"})
    private void updateConfigFromResource(final File oldConfigFile, final String resourceFileName) throws ConfigurationException {
        Yaml yaml = new Yaml();
        Map<String, Object> oldConfig;
        try (InputStream oldConfigStream = Files.newInputStream(oldConfigFile.toPath())) {
            oldConfig = yaml.load(oldConfigStream);
        } catch (final IOException e) {
            throw new ConfigurationException("An exception occurred while loading the file '" + oldConfigFile.getName() + "'.", e);
        }

        if (oldConfig == null) {
            oldConfig = new HashMap<>();
        }

        final Map<String, Object> newConfig;
        try (InputStream resourceStream = getClass().getClassLoader().getResourceAsStream(resourceFileName)) {
            if (resourceStream == null) {
                throw new ConfigurationException("Resource file '" + resourceFileName + "' not found.");
            }
            newConfig = yaml.load(resourceStream);
        } catch (final IOException e) {
            throw new ConfigurationException("An exception occurred while loading the resource file '" + resourceFileName + "'.", e);
        }

        if (newConfig == null) {
            throw new ConfigurationException("Resource file '" + resourceFileName + "' is empty or invalid.");
        }

        overwriteConfigs(newConfig, oldConfig);

        final DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setIndent(2);
        dumperOptions.setPrettyFlow(true);

        yaml = new Yaml(dumperOptions);
        try (BufferedWriter writer = Files.newBufferedWriter(oldConfigFile.toPath())) {
            yaml.dump(newConfig, writer);
        } catch (final IOException e) {
            throw new ConfigurationException("An exception occurred while updating the file '" + oldConfigFile.getName() + "'.", e);
        }
    }

    private void overwriteConfigs(final Map<String, Object> newConfig, final Map<String, Object> oldConfig) {
        for (final Map.Entry<String, Object> entry : newConfig.entrySet()) {
            final String key = entry.getKey();
            final Object newValue = entry.getValue();

            if (oldConfig.containsKey(key)) {
                final Object oldValue = oldConfig.get(key);

                if (newValue instanceof Map && oldValue instanceof Map) {
                    @SuppressWarnings("unchecked") final Map<String, Object> newChild = (Map<String, Object>) newValue;
                    @SuppressWarnings("unchecked") final Map<String, Object> oldChild = (Map<String, Object>) oldValue;
                    overwriteConfigs(newChild, oldChild);
                } else {
                    newConfig.put(key, oldValue);
                }
            }
        }
    }

    /**
     * Starts the backup process.
     */
    public void startBackup() {
        fileBackupManager.startBackup();
    }

    /**
     * Adds a file in form of an actual file or path to the backup list.
     *
     * @param file the {@link File} to add to the backup list
     * @throws ConfigurationException if the file could not be added to the backup list
     */
    public void addToBackup(final File file) throws ConfigurationException {
        fileBackupManager.addFileToBackup(file);
    }
}
