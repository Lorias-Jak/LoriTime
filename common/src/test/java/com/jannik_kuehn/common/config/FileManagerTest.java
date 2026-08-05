package com.jannik_kuehn.common.config;

import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class FileManagerTest {

    @TempDir
    private File dataFolder;

    @Test
    void languageFileMigratesFromRootToLanguageFolderAndCurrentSchema() throws Exception {
        final File rootLanguage = new File(dataFolder, "en-us.yml");
        Files.writeString(rootLanguage.toPath(), """
                schema_version: 1
                locale: 'en-us'
                prefix: '<gray>LoriTime '
                messages:
                  message.noPermission: custom permission
                """);
        final FileManager fileManager = new FileManager(new LoggerFactory(Logger.getLogger("test")), dataFolder);

        final File languageFile = fileManager.getOrCreateLanguageFile("en-us");
        final Configuration configuration = fileManager.getConfiguration(languageFile);

        assertFalse(rootLanguage.exists(), "Expected root language file to be moved");
        assertEquals(new File(dataFolder, "language/en-us.yml"), languageFile, "Expected canonical language path");
        assertEquals(3, configuration.getInt("schema_version"), "Expected current localization schema");
        assertEquals("en-us", configuration.getString("locale"), "Expected locale metadata to be preserved");
        assertEquals("custom permission", configuration.getString("messages.message.noPermission"),
                "Expected current-schema message to be preserved");
        assertTrue(configuration.containsKey("messages.message.command.loritimeadmin.transfer.warning"),
                "Expected localization migration to add transfer warning");
        assertTrue(configuration.containsKey("messages.message.command.loritimeadmin.deleteHistory.warning"),
                "Expected localization migration to add deleteHistory warning");
    }

    @Test
    void customLanguageFileIsNotCreatedFromBundledResources() throws Exception {
        final FileManager fileManager = new FileManager(new LoggerFactory(Logger.getLogger("test")), dataFolder);

        final File languageFile = fileManager.getOrCreateLanguageFile("pirate");

        assertEquals(new File(dataFolder, "language/pirate.yml"), languageFile, "Expected canonical custom language path");
        assertFalse(languageFile.exists(), "Expected missing custom language to stay missing");
    }

    @Test
    void bundledFallbackLanguageIsCreatedWithCurrentSchema() throws Exception {
        final FileManager fileManager = new FileManager(new LoggerFactory(Logger.getLogger("test")), dataFolder);

        fileManager.ensureBundledFallbackLanguage();

        final Configuration configuration = fileManager.getConfiguration(new File(dataFolder, "language/en-us.yml"));
        assertEquals(3, configuration.getInt("schema_version"), "Expected current localization schema");
        assertEquals("en-us", configuration.getString("locale"), "Expected bundled locale metadata");
        assertTrue(configuration.containsKey("messages.message.noPermission"), "Expected messages section");
    }

    @Test
    void bundledLanguageIsCreatedWhenMatchingResourceExists() throws Exception {
        final FileManager fileManager = new FileManager(new LoggerFactory(Logger.getLogger("test")), dataFolder);

        final File languageFile = fileManager.getOrCreateLanguageFile("de-de");
        final Configuration configuration = fileManager.getConfiguration(languageFile);

        assertEquals(new File(dataFolder, "language/de-de.yml"), languageFile, "Expected canonical bundled language path");
        assertEquals(3, configuration.getInt("schema_version"), "Expected current localization schema");
        assertEquals("de-de", configuration.getString("locale"), "Expected bundled locale metadata");
    }

    @Test
    void bundledLocalizationFilesContainTheSameMessageKeys() throws Exception {
        final FileManager fileManager = new FileManager(new LoggerFactory(Logger.getLogger("test")), dataFolder);
        final Set<String> englishKeys = messageKeys(fileManager, "en-us");

        assertEquals(englishKeys, messageKeys(fileManager, "de-de"), "Expected German bundle to include all message keys");
        assertEquals(englishKeys, messageKeys(fileManager, "zh-cn"), "Expected Chinese bundle to include all message keys");
    }

    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    private Set<String> messageKeys(final FileManager fileManager, final String language) throws Exception {
        final Configuration configuration = fileManager.getConfiguration(fileManager.getOrCreateLanguageFile(language));
        final Set<String> keys = new HashSet<>(configuration.getKeys());
        keys.remove("schema_version");
        keys.remove("locale");
        keys.remove("prefix");
        return keys;
    }
}
