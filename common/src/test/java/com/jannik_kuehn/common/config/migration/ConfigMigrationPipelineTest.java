package com.jannik_kuehn.common.config.migration;

import com.jannik_kuehn.common.config.StructuredConfigurationDocument;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"PMD.UnitTestContainsTooManyAsserts", "PMD.UnitTestAssertionsShouldIncludeMessage"})
class ConfigMigrationPipelineTest {
    @Test
    void migrationOperationsHandleAddRenameMoveDeleteTransformValidateAndUnversionedBaseline() {
        final StructuredConfigurationDocument document = new StructuredConfigurationDocument(Map.of(
                "oldName", "value",
                "source", "moved",
                "deleteMe", true,
                "numberAsString", "12",
                "invalid", "bad"));
        final ConfigSchema schema = new ConfigSchema(0, 2, List.of(
                ConfigMigration.from(0)
                        .add("added", "default")
                        .rename("oldName", "newName")
                        .move("source", "target.section")
                        .delete("deleteMe")
                        .transform("numberAsString", value -> Integer.parseInt(value.toString()))
                        .validate("invalid", value -> value instanceof Number, 5)
                        .build(),
                ConfigMigration.from(1).add("second", true).build()));

        final ConfigMigrationResult result = new ConfigMigrationPipeline(schema).migrate(document);

        assertTrue(result.changed());
        assertEquals("default", document.get("added"));
        assertEquals("value", document.get("newName"));
        assertNull(document.get("oldName"));
        assertEquals("moved", document.get("target.section"));
        assertNull(document.get("source"));
        assertNull(document.get("deleteMe"));
        assertEquals(12, document.get("numberAsString"));
        assertEquals(5, document.get("invalid"));
        assertEquals(true, document.get("second"));
        assertEquals(2, document.get(ConfigSchema.VERSION_PATH));
    }

    @Test
    void templateMergePreservesKnownUserValuesAddsDefaultsAndUnknownKeys() {
        final StructuredConfigurationDocument template = new StructuredConfigurationDocument(Map.of(
                "configSchemaVersion", 2,
                "general", Map.of("language", "en", "saveInterval", 30),
                "obsoleteReplacement", true));
        final StructuredConfigurationDocument user = new StructuredConfigurationDocument(Map.of(
                "configSchemaVersion", 2,
                "general", Map.of("language", "de"),
                "customUnknown", "drop"));

        final StructuredConfigurationDocument merged = new ConfigTemplateMerger().merge(template, user);

        assertEquals("de", merged.get("general.language"));
        assertEquals(30, merged.get("general.saveInterval"));
        assertEquals(true, merged.get("obsoleteReplacement"));
        assertEquals("drop", merged.get("customUnknown"));
        assertEquals(2, merged.get(ConfigSchema.VERSION_PATH));
    }

    @Test
    void templateMergeTreatsNestedAndLiteralDottedPathsAsEquivalent() {
        final StructuredConfigurationDocument template = new StructuredConfigurationDocument(Map.of(
                "messages", Map.of(
                        "message", Map.of("noPermission", "default permission"),
                        "unit", Map.of("second", Map.of("singular", "second")))));
        final StructuredConfigurationDocument user = new StructuredConfigurationDocument(Map.of(
                "messages", Map.of(
                        "message.noPermission", "custom permission",
                        "unit.second.singular", "custom second")));

        final StructuredConfigurationDocument merged = new ConfigTemplateMerger().merge(template, user);

        assertEquals("custom permission", merged.get("messages.message.noPermission"));
        assertEquals("custom second", merged.get("messages.unit.second.singular"));
    }

    @Test
    void localizationSchemaAddsTransferWarningWithoutMigratingLegacyLanguageKeys() {
        final StructuredConfigurationDocument document = new StructuredConfigurationDocument(Map.of(
                "schema_version", 1,
                "locale", "en-us",
                "messages", Map.of(
                        "message.nopermission", "legacy permission",
                        "message.customPlugin.custom_key", "custom value")));
        final StructuredConfigurationDocument template = new StructuredConfigurationDocument(Map.of(
                "schema_version", 3,
                "messages", Map.of(
                        "message.noPermission", "default permission")));

        final ConfigMigrationResult result = new ConfigMigrationPipeline(ConfigSchema.localization()).migrate(document);
        final StructuredConfigurationDocument merged = new ConfigTemplateMerger().merge(template, result.document());
        final Map<?, ?> resultMessages = assertInstanceOf(Map.class, result.document().get("messages"));
        final Map<?, ?> mergedMessages = assertInstanceOf(Map.class, merged.get("messages"));

        assertEquals("legacy permission", resultMessages.get("message.nopermission"));
        assertEquals("custom value", resultMessages.get("message.customPlugin.custom_key"));
        assertEquals(3, result.document().get("schema_version"));
        assertNotNull(result.document().get("messages.message.command.loritimeadmin.transfer.warning"));
        assertNotNull(result.document().get("messages.message.command.loritimeadmin.deleteHistory.warning"));
        assertEquals("default permission", mergedMessages.get("message.noPermission"));
        assertEquals(3, merged.get("schema_version"));
    }

    @Test
    void latestVersionSkipsOperationsButStillKeepsTemplateDefaultsSeparate() {
        final StructuredConfigurationDocument document = new StructuredConfigurationDocument(Map.of(
                ConfigSchema.VERSION_PATH, 1,
                "value", "kept"));
        final ConfigSchema schema = new ConfigSchema(0, 1,
                List.of(ConfigMigration.from(0).add("shouldNotRun", true).build()));

        final ConfigMigrationResult result = new ConfigMigrationPipeline(schema).migrate(document);

        assertFalse(result.changed());
        assertEquals("kept", document.get("value"));
        assertNull(document.get("shouldNotRun"));
    }

    @Test
    void loriTimeConfigMigrationMovesLegacyV1ValuesToCurrentPathsAndStandardizesTablePrefix() {
        final StructuredConfigurationDocument document = new StructuredConfigurationDocument(Map.of(
                "general", Map.of(
                        "storage", "yml",
                        "checkForUpdates", false,
                        "language", "de",
                        "saveInterval", 45),
                "mysql", Map.of(
                        "host", "db.example.test",
                        "port", 3307,
                        "database", "loritime",
                        "user", "db_user",
                        "password", "secret",
                        "tablePrefix", "custom_prefix"),
                "multiSetup", Map.of(
                        "enabled", true,
                        "mode", "slave"),
                "afk", Map.of(
                        "enabled", true,
                        "after", "30m"),
                "integrations", Map.of("PlaceholderAPI", true)));

        final ConfigMigrationResult result = new ConfigMigrationPipeline(ConfigSchema.loriTimeConfig()).migrate(document);

        assertTrue(result.changed());
        assertEquals(3, document.get(ConfigSchema.VERSION_PATH));
        assertEquals("sqlite", document.get("storageMethod"));
        assertEquals(true, document.get("storageMigration.legacyFlatFileImport"));
        assertEquals(false, document.get("updater.checkForUpdates"));
        assertEquals("de", document.get("general.language"));
        assertEquals(45, document.get("general.saveInterval"));
        assertEquals("db.example.test", document.get("data.host"));
        assertEquals(3307, document.get("data.port"));
        assertEquals("loritime", document.get("data.database"));
        assertEquals("db_user", document.get("data.user"));
        assertEquals("secret", document.get("data.password"));
        assertEquals("loritime", document.get("data.tablePrefix"));
        assertEquals("slave", document.get("multiSetup.mode"));
        assertNull(document.get("multiSetup.enabled"));
        assertNull(document.get("general.storage"));
        assertNull(document.get("general.checkForUpdates"));
        assertNull(document.get("mysql"));
    }

    @Test
    void versionTwoConfigMigrationAddsStatisticsDefaultsWithoutReplacingCustomValues() {
        final StructuredConfigurationDocument document = new StructuredConfigurationDocument(Map.of(
                ConfigSchema.VERSION_PATH, 2,
                "stats", Map.of("default-range", "14d")));

        final ConfigMigrationResult result = new ConfigMigrationPipeline(ConfigSchema.loriTimeConfig()).migrate(document);

        assertTrue(result.changed());
        assertEquals(3, document.get(ConfigSchema.VERSION_PATH));
        assertEquals("14d", document.get("stats.default-range"));
        assertEquals("3m", document.get("stats.bounce-threshold"));
        assertEquals("system", document.get("stats.calendar-time-zone"));
    }

    @Test
    void versionThreeConfigTemplateAddsCalendarTimezoneWithoutReplacingCustomValue() {
        final StructuredConfigurationDocument template = new StructuredConfigurationDocument(Map.of(
                ConfigSchema.VERSION_PATH, 3, "stats", Map.of("calendar-time-zone", "system")));
        final StructuredConfigurationDocument defaults = new StructuredConfigurationDocument(Map.of(
                ConfigSchema.VERSION_PATH, 3));
        final StructuredConfigurationDocument custom = new StructuredConfigurationDocument(Map.of(
                ConfigSchema.VERSION_PATH, 3, "stats", Map.of("calendar-time-zone", "Europe/Berlin")));

        final StructuredConfigurationDocument mergedDefaults = new ConfigTemplateMerger().merge(template, defaults);
        final StructuredConfigurationDocument mergedCustom = new ConfigTemplateMerger().merge(template, custom);

        assertEquals("system", mergedDefaults.get("stats.calendar-time-zone"));
        assertEquals("Europe/Berlin", mergedCustom.get("stats.calendar-time-zone"));
        assertEquals(3, mergedDefaults.get(ConfigSchema.VERSION_PATH));
        assertEquals(3, mergedCustom.get(ConfigSchema.VERSION_PATH));
    }

    @Test
    void versionTwoLocalizationMigrationAddsStatisticsMessagesWithoutReplacingCustomValues() {
        final StructuredConfigurationDocument document = new StructuredConfigurationDocument(Map.of(
                "schema_version", 2,
                "locale", "de-de",
                "messages", Map.of("message", Map.of("command", Map.of("stats", Map.of(
                        "error", "custom statistics error"))))));

        final ConfigMigrationResult result = new ConfigMigrationPipeline(ConfigSchema.localization()).migrate(document);

        assertTrue(result.changed());
        assertEquals(3, document.get("schema_version"));
        assertEquals("custom statistics error", document.get("messages.message.command.stats.error"));
        assertNotNull(document.get("messages.message.command.stats.commandUsage"));
        assertNotNull(document.get("messages.message.command.stats.overview"));
        assertNotNull(document.get("messages.message.command.stats.users"));
        assertNotNull(document.get("messages.message.command.stats.sessions"));
        assertNotNull(document.get("messages.message.command.stats.usage"));
        assertNotNull(document.get("messages.message.command.stats.top"));
        assertNotNull(document.get("messages.message.command.stats.afk"));
        assertNotNull(document.get("messages.message.command.stats.retention"));
        assertNotNull(document.get("messages.message.command.stats.unsupported"));
    }

    @Test
    void versionTwoChineseLocalizationUsesEnglishStatisticsMessages() {
        final StructuredConfigurationDocument english = new StructuredConfigurationDocument(Map.of(
                "schema_version", 2, "locale", "en-us"));
        final StructuredConfigurationDocument chinese = new StructuredConfigurationDocument(Map.of(
                "schema_version", 2, "locale", "zh-cn"));

        new ConfigMigrationPipeline(ConfigSchema.localization()).migrate(english);
        new ConfigMigrationPipeline(ConfigSchema.localization()).migrate(chinese);

        assertEquals(3, chinese.get("schema_version"));
        for (final String key : List.of("commandUsage", "unsupported", "error", "overview", "users", "sessions",
                "usage", "top", "afk", "retention")) {
            assertEquals(english.get("messages.message.command.stats." + key),
                    chinese.get("messages.message.command.stats." + key));
        }
    }
}
