package com.jannik_kuehn.common.config;

import com.github.roleplaycauldron.spellbook.core.file.FileBackupService;
import com.github.roleplaycauldron.spellbook.core.file.FileException;
import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import com.jannik_kuehn.common.config.migration.ConfigMigrationPipeline;
import com.jannik_kuehn.common.config.migration.ConfigMigrationResult;
import com.jannik_kuehn.common.config.migration.ConfigSchema;
import com.jannik_kuehn.common.config.migration.ConfigTemplateMerger;
import com.jannik_kuehn.common.exception.ConfigurationException;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * The {@link FileManager} is responsible for managing the files of the plugin including updating and backing up.
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.GodClass"})
public class FileManager {
    /**
     * Name of the main plugin config resource.
     */
    private static final String CONFIG_FILE_NAME = "config.yml";

    /**
     * Name of the command aliases resource.
     */
    private static final String COMMANDS_FILE_NAME = "commands.yml";

    /**
     * Folder for localization files.
     */
    private static final String LANGUAGE_FOLDER = "language";

    /**
     * The {@link WrappedLogger} instance.
     */
    private final WrappedLogger log;

    /**
     * The {@link LoggerFactory} used by loaded configuration files.
     */
    private final LoggerFactory loggerFactory;

    /**
     * The data folder of the plugin.
     */
    private final File dataFolder;

    /**
     * The {@link FileBackupService} instance.
     */
    private final FileBackupService backupService;

    /**
     * If backups are enabled.
     */
    private final boolean backupsEnabled;

    /**
     * Creates a new {@link FileManager} object.
     *
     * @param loggerFactory the {@link LoggerFactory} instance
     * @param dataFolder    the data folder of the plugin
     */
    @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
    public FileManager(final LoggerFactory loggerFactory, final File dataFolder) {
        this.loggerFactory = loggerFactory;
        this.log = loggerFactory.create(FileManager.class);
        this.dataFolder = dataFolder;

        final Configuration temp = tempLoadConfiguration();

        this.backupsEnabled = temp.getBoolean("backup.enabled", true);
        final int maxBackups = temp.getInt("backup.maxBackups", 5);
        this.backupService = new FileBackupService(loggerFactory.create(FileBackupService.class), dataFolder, "backups", maxBackups);
    }

    private Configuration tempLoadConfiguration() {
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            log.error("An error occurred while creating the plugin folder.");
        }
        Configuration tempConfig = null;
        final File file = new File(dataFolder.toString(), CONFIG_FILE_NAME);
        try {
            if (doesFileNotExist(file)) {
                createFile(file);
                createFromResources(file.toPath(), CONFIG_FILE_NAME);
            }
            tempConfig = getHumanConfiguration(file);
        } catch (final ConfigurationException e) {
            log.error("An error occurred while loading the backup manager configuration.", e);
        }
        return tempConfig;
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
        return getOrCreateFile(folder, fileName, fileName, needCopy);
    }

    /**
     * Returns a language file from the canonical language folder.
     *
     * @param language language id
     * @return language file
     * @throws ConfigurationException if the file cannot be created or updated
     */
    @SuppressWarnings("PMD.CyclomaticComplexity")
    public File getOrCreateLanguageFile(final String language) throws ConfigurationException {
        final String languageId = language == null || language.isBlank()
                ? "en-us"
                : language.trim().toLowerCase(Locale.ROOT);
        final String fileName = languageId + ".yml";
        final File languageFolder = new File(dataFolder, LANGUAGE_FOLDER);
        if (!languageFolder.exists() && !languageFolder.mkdirs()) {
            throw new ConfigurationException("Could not create language folder.");
        }

        final File canonical = new File(languageFolder, fileName);
        final File legacy = new File(dataFolder, fileName);
        if (!canonical.exists() && legacy.exists()) {
            addToBackup(legacy);
            try {
                Files.move(legacy.toPath(), canonical.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (final IOException ex) {
                throw new ConfigurationException("Could not migrate language file '" + fileName + "' to language folder.", ex);
            }
        }
        final String resourceFileName = LANGUAGE_FOLDER + "/" + fileName;
        if (resourceExists(resourceFileName)) {
            return getOrCreateFile(languageFolder.toString(), fileName, resourceFileName, true);
        }
        if (!canonical.exists()) {
            log.warn("Custom language file '" + fileName + "' does not exist in the language folder.");
        }
        return canonical;
    }

    /**
     * Ensures the bundled hard fallback language exists.
     *
     * @throws ConfigurationException if the bundled language file cannot be created
     */
    public void ensureBundledFallbackLanguage() throws ConfigurationException {
        getOrCreateLanguageFile("en-us");
    }

    private File getOrCreateFile(final String folder,
                                 final String fileName,
                                 final String resourceFileName,
                                 final boolean needCopy) throws ConfigurationException {
        final File file = new File(folder, fileName);
        if (doesFileNotExist(file) && needCopy) {
            createFile(file);
            createFromResources(file.toPath(), resourceFileName);
        } else if (doesFileNotExist(file)) {
            createFile(file);
        } else if (needCopy) {
            updateHumanConfigFromResourceIfNeeded(file, resourceFileName);
        }
        return file;
    }

    private boolean doesFileNotExist(final File file) {
        return !file.exists();
    }

    private void createFile(final File file) throws ConfigurationException {
        try {
            final File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new ConfigurationException("Could not create folder '" + parent.getName() + "'.");
            }
            if (file.createNewFile()) {
                log.info("Created file '" + file.getName() + "'.");
            }
        } catch (final IOException e) {
            throw new ConfigurationException("An exception occurred while creating the file '" + file.getName() + "' on startup.", e);
        }
    }

    private void createFromResources(final Path createdFile, final String nameFromSourceOfReplacement) throws ConfigurationException {
        try (InputStream resourceStream = openResourceStream(nameFromSourceOfReplacement)) {
            Files.copy(resourceStream, createdFile, StandardCopyOption.REPLACE_EXISTING);
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
        return getHumanConfiguration(fileToChange);
    }

    /**
     * Loads a human-managed configuration file.
     *
     * @param fileToChange the file to load
     * @return the loaded configuration
     * @throws ConfigurationException if the file could not be loaded
     */
    public Configuration getHumanConfiguration(final File fileToChange) throws ConfigurationException {
        final Configuration configurationFile = new YamlConfiguration(fileToChange.toString(), loggerFactory);
        if (configurationFile.isLoaded()) {
            return configurationFile;
        } else {
            throw new ConfigurationException("An issue occurred while loading the file '" + fileToChange.getName() + "'. The File is null, there should be data.");
        }
    }

    /**
     * Loads a flat data file without applying human config template migrations.
     *
     * @param fileToChange the file to load
     * @return the loaded configuration
     * @throws ConfigurationException if the file could not be loaded
     */
    public Configuration getFlatDataConfiguration(final File fileToChange) throws ConfigurationException {
        final Configuration configurationFile = new YamlConfiguration(fileToChange.toString(), loggerFactory);
        if (configurationFile.isLoaded()) {
            return configurationFile;
        } else {
            throw new ConfigurationException("An issue occurred while loading the file '" + fileToChange.getName() + "'. The File is null, there should be data.");
        }
    }

    private void updateHumanConfigFromResourceIfNeeded(final File oldConfigFile, final String resourceFileName) throws ConfigurationException {
        final StructuredConfigurationDocument current = loadDocument(oldConfigFile.toPath(), oldConfigFile.getName());
        final StructuredConfigurationDocument template = loadResourceDocument(resourceFileName);
        final StructuredConfigurationDocument updated;
        final boolean changed;

        final ConfigSchema schema = schemaForResource(resourceFileName);
        if (schema != null) {
            final ConfigMigrationResult migrationResult = new ConfigMigrationPipeline(schema).migrate(current);
            updated = new ConfigTemplateMerger().merge(template, migrationResult.document());
            changed = migrationResult.changed() || !Objects.equals(updated.asMap(), current.asMap());
        } else {
            updated = new ConfigTemplateMerger().merge(template, current);
            changed = !Objects.equals(updated.asMap(), current.asMap());
        }

        if (!changed) {
            return;
        }

        backupService.addFileToBackup(oldConfigFile);
        writeDocument(oldConfigFile.toPath(), updated);
    }

    private ConfigSchema schemaForResource(final String resourceFileName) {
        if (CONFIG_FILE_NAME.equals(resourceFileName)) {
            return ConfigSchema.loriTimeConfig();
        }
        if (COMMANDS_FILE_NAME.equals(resourceFileName)) {
            return ConfigSchema.commands();
        }
        if (resourceFileName.startsWith(LANGUAGE_FOLDER + "/") && resourceFileName.endsWith(".yml")) {
            return ConfigSchema.localization();
        }
        return null;
    }

    private StructuredConfigurationDocument loadResourceDocument(final String resourceFileName) throws ConfigurationException {
        try (InputStream resourceStream = openResourceStream(resourceFileName)) {
            return loadDocument(resourceStream, resourceFileName);
        } catch (final IOException e) {
            throw new ConfigurationException("An exception occurred while loading the resource file '" + resourceFileName + "'.", e);
        }
    }

    @SuppressWarnings("PMD.UseProperClassLoader")
    private boolean resourceExists(final String resourceFileName) {
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        return contextClassLoader != null && contextClassLoader.getResource(resourceFileName) != null
                || FileManager.class.getClassLoader().getResource(resourceFileName) != null;
    }

    @SuppressWarnings("PMD.UseProperClassLoader")
    private InputStream openResourceStream(final String resourceFileName) throws ConfigurationException {
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        InputStream resourceStream = contextClassLoader == null ? null : contextClassLoader.getResourceAsStream(resourceFileName);
        if (resourceStream == null) {
            resourceStream = FileManager.class.getClassLoader().getResourceAsStream(resourceFileName);
        }
        if (resourceStream == null) {
            throw new ConfigurationException("Resource file '" + resourceFileName + "' not found.");
        }
        return resourceStream;
    }

    private StructuredConfigurationDocument loadDocument(final Path path, final String displayName) throws ConfigurationException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            return loadDocument(inputStream, displayName);
        } catch (final IOException e) {
            throw new ConfigurationException("An exception occurred while loading the file '" + displayName + "'.", e);
        }
    }

    private StructuredConfigurationDocument loadDocument(final InputStream inputStream, final String displayName) throws ConfigurationException {
        final Object loaded;
        try {
            loaded = new Yaml().load(inputStream);
        } catch (final YAMLException ex) {
            throw new ConfigurationException("Malformed YAML in '" + displayName + "'.", ex);
        }
        if (loaded == null) {
            return new StructuredConfigurationDocument();
        }
        if (!(loaded instanceof Map<?, ?> map)) {
            throw new ConfigurationException("YAML file '" + displayName + "' must contain a section at the root.");
        }
        return new StructuredConfigurationDocument(map);
    }

    private void writeDocument(final Path path, final StructuredConfigurationDocument document) throws ConfigurationException {
        final DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setIndent(2);
        dumperOptions.setPrettyFlow(true);

        final Yaml yaml = new Yaml(dumperOptions);
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            yaml.dump(document.asMap(), writer);
        } catch (final IOException e) {
            throw new ConfigurationException("An exception occurred while updating the file '" + path.getFileName() + "'.", e);
        }
    }

    /**
     * Starts the backup process.
     */
    public void startBackup() {
        if (backupsEnabled) {
            backupService.startBackup();
        }
    }

    /**
     * Adds a file in form of an actual file or path to the backup list.
     *
     * @param file the {@link File} to add to the backup list
     * @throws FileException if the file could not be added to the backup lis
     */
    public void addToBackup(final File file) {
        if (backupsEnabled) {
            backupService.addFileToBackup(file);
        }
    }
}
