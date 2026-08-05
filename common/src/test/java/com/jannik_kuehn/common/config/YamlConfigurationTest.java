package com.jannik_kuehn.common.config;

import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.command.config.CommandAlias;
import com.jannik_kuehn.common.command.config.CommandAliasConfig;
import com.jannik_kuehn.common.exception.ConfigurationException;
import com.jannik_kuehn.common.platform.CommonServer;
import com.jannik_kuehn.common.scheduler.PluginScheduler;
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
import static org.mockito.Mockito.*;

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

        assertEquals(3, migrated.getInt("configSchemaVersion"));
        assertEquals("sqlite", migrated.getString("storageMethod"));
        assertTrue(migrated.getBoolean("storageMigration.legacyFlatFileImport"));
        assertFalse(migrated.getBoolean("updater.checkForUpdates"));
        assertEquals("master", migrated.getString("multiSetup.mode"));
        assertFalse(migrated.containsKey("general.storage"));
        assertFalse(migrated.containsKey("multiSetup.enabled"));
    }

    @Test
    void fileManagerLoadsCurrentSchemaLocalizationFiles() throws IOException, ConfigurationException {
        final File localizationFolder = new File(dataFolder, "localization");
        Files.createDirectories(localizationFolder.toPath());
        Files.writeString(new File(dataFolder, "config.yml").toPath(), "backup:\n  enabled: true\n  maxBackups: 5\n");
        Files.writeString(new File(localizationFolder, "en-us.yml").toPath(), """
                schema_version: 1
                locale: 'en-us'
                prefix: '<gray>LoriTime '
                messages:
                  unit.second.singular: 'custom second'
                  unit.second.plural: 'custom seconds'
                  unit.second.identifier:
                    - 'cs'
                  message.noPermission: 'custom no permission'
                """);

        final FileManager fileManager = new FileManager(loggerFactory, localizationFolder);
        final Configuration migrated = fileManager.getConfiguration(
                fileManager.getOrCreateLanguageFile("en-us"));

        assertEquals(3, migrated.getInt("schema_version"));
        assertEquals("custom no permission", migrated.getString("messages.message.noPermission"));
        assertEquals("custom second", migrated.getString("messages.unit.second.singular"));
        assertTrue(migrated.containsKey("messages.message.command.loritime.usage"));
        assertTrue(migrated.containsKey("messages.message.command.loritimeadmin.transfer.warning"));
        assertTrue(migrated.containsKey("messages.message.command.loritimeadmin.deleteHistory.warning"));
        assertTrue(migrated.containsKey("messages.message.command.stats.overview"));
    }

    @Test
    void fileManagerCreatesCommandsConfigWithBackendProfiles() throws ConfigurationException {
        final File commandFolder = new File(dataFolder, "commands");
        final FileManager fileManager = new FileManager(loggerFactory, commandFolder);

        final Configuration commands = fileManager.getConfiguration(
                fileManager.getOrCreateFile(commandFolder.toString(), "commands.yml", true));

        assertTrue(new File(commandFolder, "commands.yml").exists());
        assertEquals(1, commands.getInt("commandSchemaVersion"));
        assertEquals("plta", commands.getString("profiles.proxy.admin.name"));
        assertEquals("lta", commands.getString("profiles.backend.canonical.admin.name"));
        assertEquals("lta", commands.getString("profiles.backend.slave.admin.name"));
        assertFalse(commands.containsKey("profiles.backend.slave.modify.name"));
        assertFalse(commands.containsKey("profiles.backend.slave.local.name"));
        assertFalse(commands.containsKey("profiles.paper.canonical.admin.name"));
        assertFalse(commands.containsKey("profiles.backend.slave.stats.name"));
    }

    @Test
    void fileManagerMigratesLegacyCommandsAndPreservesCustomValues() throws IOException, ConfigurationException {
        final File commandFolder = new File(dataFolder, "migrate-commands");
        Files.createDirectories(commandFolder.toPath());
        final File commandsFile = new File(commandFolder, "commands.yml");
        Files.writeString(commandsFile.toPath(), """
                profiles:
                  proxy:
                    admin:
                      name: 'customadmin'
                      aliases: ['custom-admin']
                    stats:
                      name: 'networkstats'
                      aliases: ['network-statistics']
                  backend:
                    canonical:
                      admin:
                        name: 'backendadmin'
                        aliases: []
                    slave:
                      admin:
                        name: 'slaveadmin'
                        aliases: []
                customCommands:
                  retained: true
                """);
        final FileManager fileManager = new FileManager(loggerFactory, commandFolder);

        final Configuration migrated = fileManager.getConfiguration(
                fileManager.getOrCreateFile(commandFolder.toString(), "commands.yml", true));
        final String firstMigration = Files.readString(commandsFile.toPath());
        fileManager.getOrCreateFile(commandFolder.toString(), "commands.yml", true);

        assertEquals(1, migrated.getInt("commandSchemaVersion"));
        assertEquals("customadmin", migrated.getString("profiles.proxy.admin.name"));
        assertEquals("networkstats", migrated.getString("profiles.proxy.stats.name"));
        assertEquals(List.of("network-statistics"), migrated.getArrayList("profiles.proxy.stats.aliases"));
        assertEquals("ltstats", migrated.getString("profiles.backend.canonical.stats.name"));
        assertEquals(List.of("stats", "loritimestats"),
                migrated.getArrayList("profiles.backend.canonical.stats.aliases"));
        assertTrue(migrated.getBoolean("customCommands.retained"));
        assertFalse(migrated.containsKey("profiles.backend.slave.stats.name"));
        assertEquals(firstMigration, Files.readString(commandsFile.toPath()));
    }

    @Test
    void commandAliasConfigResolvesProfileSpecificValues() throws ConfigurationException {
        final File commandFolder = new File(dataFolder, "alias-config");
        final FileManager fileManager = new FileManager(loggerFactory, commandFolder);
        final Configuration commands = fileManager.getConfiguration(
                fileManager.getOrCreateFile(commandFolder.toString(), "commands.yml", true));
        final CommandAliasConfig aliasConfig = new CommandAliasConfig(commands);

        final CommandAlias proxyAdmin = aliasConfig.resolve(CommandAliasConfig.CommandProfile.PROXY,
                CommandAliasConfig.CommandNode.ADMIN, "fallback", List.of());
        final CommandAlias canonicalModify = aliasConfig.resolve(CommandAliasConfig.CommandProfile.BACKEND_CANONICAL,
                CommandAliasConfig.CommandNode.MODIFY, "fallback", List.of());

        assertEquals("plta", proxyAdmin.name());
        assertTrue(proxyAdmin.aliases().contains("loritimeproxyadmin"));
        assertEquals("ltmodify", canonicalModify.name());
        assertTrue(canonicalModify.aliases().contains("ltm"));
    }

    @Test
    void commandAliasConfigReflectsReloadedCommandsFile() throws IOException, ConfigurationException {
        final File commandFolder = new File(dataFolder, "reload-commands");
        final FileManager fileManager = new FileManager(loggerFactory, commandFolder);
        final File commandFile = fileManager.getOrCreateFile(commandFolder.toString(), "commands.yml", true);
        final Configuration commands = fileManager.getConfiguration(commandFile);
        final CommandAliasConfig aliasConfig = new CommandAliasConfig(commands);

        Files.writeString(commandFile.toPath(), """
                profiles:
                  proxy:
                    admin:
                      name: 'changedadmin'
                      aliases:
                        - 'changedalias'
                """);
        commands.reload();

        final CommandAlias alias = aliasConfig.resolve(CommandAliasConfig.CommandProfile.PROXY,
                CommandAliasConfig.CommandNode.ADMIN, "fallback", List.of());

        assertEquals("changedadmin", alias.name());
        assertEquals(List.of("changedalias"), alias.aliases());
    }

    @Test
    void configTemplateContainsDefaultRecentPlayerCompletionWindow() throws ConfigurationException {
        final File configFolder = new File(dataFolder, "completion-config");
        final FileManager fileManager = new FileManager(loggerFactory, configFolder);
        final Configuration config = fileManager.getConfiguration(
                fileManager.getOrCreateFile(configFolder.toString(), "config.yml", true));

        assertEquals(30, config.getInt("command.completion.recentPlayersDays"));
        assertEquals("system", config.getString("stats.calendar-time-zone"));
        assertEquals("calendar:today", config.getString("stats.default-range"));
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
