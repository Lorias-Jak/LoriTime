package com.jannik_kuehn.common.config;

import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.common.CommonServer;
import com.jannik_kuehn.common.api.scheduler.PluginScheduler;
import com.jannik_kuehn.common.exception.ConfigurationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@SuppressWarnings({"PMD.UnitTestContainsTooManyAsserts", "PMD.UnitTestAssertionsShouldIncludeMessage"})
class YamlConfigurationTest {
    @TempDir
    private File dataFolder;

    private File configFile;

    private LoggerFactory loggerFactory;

    @BeforeEach
    void setUp() throws IOException {
        loggerFactory = new LoggerFactory(Logger.getLogger("test"));
        new LoriTimePlugin(loggerFactory, dataFolder,
                mock(PluginScheduler.class), mock(CommonServer.class), null);
        configFile = new File(dataFolder, "test.yml");
        Files.writeString(configFile.toPath(), """
                data:
                  poolSettings:
                    maximumPoolSize: 12
                    minimumIdle: 3
                  host: localhost
                command:
                  LoriTime:
                    alias:
                      - lt
                      - loritime
                invalidNumber: wrong
                """);
    }

    @Test
    void sectionLookupKeysAndRelativePathsWork() {
        final Configuration configuration = new YamlConfiguration(configFile.toString(), loggerFactory);

        final Optional<ConfigSection> section = configuration.getSection("data.poolSettings");

        assertTrue(section.isPresent());
        assertEquals("data.poolSettings", section.get().getPath());
        assertEquals(12, section.get().getInt("maximumPoolSize"));
        assertEquals(3, section.get().getInt("minimumIdle"));
        assertEquals(SetFactory.setOf("maximumPoolSize", "minimumIdle"), section.get().getKeys());
        assertTrue(configuration.getSection("data.host").isEmpty());
        assertTrue(configuration.getSection("missing").isEmpty());
    }

    @Test
    void recursiveKeysAndValuesAreRelativeToSection() {
        final Configuration configuration = new YamlConfiguration(configFile.toString(), loggerFactory);
        final ConfigSection data = configuration.getSection("data").orElseThrow();

        assertEquals(SetFactory.setOf("poolSettings", "poolSettings.maximumPoolSize", "poolSettings.minimumIdle", "host"),
                data.getKeys(true));
        assertEquals(Map.of("poolSettings.maximumPoolSize", 12, "poolSettings.minimumIdle", 3, "host", "localhost"),
                data.getValues(true));
    }

    @Test
    void typedDefaultsHandleMissingIncompatibleAndNumericWidening() {
        final Configuration configuration = new YamlConfiguration(configFile.toString(), loggerFactory);

        assertEquals("fallback", configuration.getString("missing", "fallback"));
        assertEquals(7, configuration.getInt("invalidNumber", 7));
        assertEquals(12L, configuration.getLong("data.poolSettings.maximumPoolSize", 99L));
        assertFalse(configuration.getBoolean("missingBoolean"));
    }

    @Test
    void existingDotPathMethodsReadWriteRemoveReloadAndContains() {
        final Configuration configuration = new YamlConfiguration(configFile.toString(), loggerFactory);

        assertEquals(12, configuration.getInt("data.poolSettings.maximumPoolSize"));
        assertEquals(List.of("lt", "loritime"), configuration.getArrayList("command.LoriTime.alias"));
        assertTrue(configuration.containsKey("data.poolSettings.minimumIdle"));

        configuration.setTemporaryValue("data.poolSettings.keepAliveTime", 1000);
        assertEquals(1000, configuration.getInt("data.poolSettings.keepAliveTime"));
        configuration.reload();
        assertFalse(configuration.containsKey("data.poolSettings.keepAliveTime"));

        configuration.setValue("data.poolSettings.maximumPoolSize", 20);
        configuration.remove("data.poolSettings.minimumIdle");
        configuration.reload();

        assertEquals(20, configuration.getInt("data.poolSettings.maximumPoolSize"));
        assertFalse(configuration.containsKey("data.poolSettings.minimumIdle"));
        assertFalse(configuration.containsKey("data.poolSettings.keepAliveTime"));
        assertEquals(Map.of(
                "data.poolSettings.maximumPoolSize", 20,
                "data.host", "localhost",
                "command.LoriTime.alias", List.of("lt", "loritime"),
                "invalidNumber", "wrong"), configuration.getAll());
    }

    @Test
    void fileManagerExposesFlatDataConfigurationWithoutHumanTemplateMigration() throws IOException, ConfigurationException {
        final File dataFile = new File(dataFolder, "names.yml");
        Files.writeString(dataFile.toPath(), "playerName: 44174cf6-e76c-4994-899c-3387284ecd62\n");
        final FileManager fileManager = new FileManager(new LoggerFactory(Logger.getLogger("test")), dataFolder);

        final Configuration flatData = fileManager.getFlatDataConfiguration(dataFile);

        assertEquals("44174cf6-e76c-4994-899c-3387284ecd62", flatData.getString("playerName"));
        assertFalse(flatData.containsKey("configSchemaVersion"));
    }

    @Test
    void fileManagerMigratesExistingConfigAfterBackupServiceBootstrap() throws IOException, ConfigurationException {
        final File bootstrapFolder = new File(dataFolder, "bootstrap");
        Files.createDirectories(bootstrapFolder.toPath());
        Files.writeString(new File(bootstrapFolder, "config.yml").toPath(), """
                general:
                  storage: 'yml'
                  checkForUpdates: false
                multiSetup:
                  enabled: true
                  mode: 'master'
                backup:
                  enabled: true
                  maxBackups: 5
                """);

        final FileManager fileManager = new FileManager(loggerFactory, bootstrapFolder);
        final Configuration migrated = fileManager.getConfiguration(
                fileManager.getOrCreateFile(bootstrapFolder.toString(), "config.yml", true));

        assertEquals(2, migrated.getInt("configSchemaVersion"));
        assertEquals("sqlite", migrated.getString("storageMethod"));
        assertTrue(migrated.getBoolean("storageMigration.legacyFlatFileImport"));
        assertFalse(migrated.getBoolean("updater.checkForUpdates"));
        assertEquals("master", migrated.getString("multiSetup.mode"));
        assertFalse(migrated.containsKey("general.storage"));
        assertFalse(migrated.containsKey("multiSetup.enabled"));
    }

    @Test
    void fileManagerMigratesLocalizationFilesAndPreservesCustomizedMessages() throws IOException, ConfigurationException {
        final File localizationFolder = new File(dataFolder, "localization");
        Files.createDirectories(localizationFolder.toPath());
        Files.writeString(new File(localizationFolder, "config.yml").toPath(), "backup:\n  enabled: true\n  maxBackups: 5\n");
        Files.writeString(new File(localizationFolder, "en.yml").toPath(), """
                unit:
                  second:
                    singular: 'custom second'
                    plural: 'custom seconds'
                    identifier:
                      - 'cs'
                message:
                  nopermission: 'custom no permission'
                """);

        final FileManager fileManager = new FileManager(loggerFactory, localizationFolder);
        final Configuration migrated = fileManager.getConfiguration(
                fileManager.getOrCreateFile(localizationFolder.toString(), "en.yml", true));

        assertEquals(1, migrated.getInt("configSchemaVersion"));
        assertEquals("custom no permission", migrated.getString("message.nopermission"));
        assertEquals("custom second", migrated.getString("unit.second.singular"));
        assertTrue(migrated.containsKey("message.command.loritime.usage"));
    }

    private static final class SetFactory {
        private SetFactory() {
        }

        @SafeVarargs
        private static <T> Set<T> setOf(final T... values) {
            return new LinkedHashSet<>(List.of(values));
        }
    }
}
