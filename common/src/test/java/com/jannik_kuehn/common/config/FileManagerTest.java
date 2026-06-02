package com.jannik_kuehn.common.config;

import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class FileManagerTest {

    @TempDir
    private File dataFolder;

    @Test
    void languageFileMigratesFromRootToLanguageFolderAndCamelCaseKeys() throws Exception {
        final File rootLanguage = new File(dataFolder, "en-us.yml");
        Files.writeString(rootLanguage.toPath(), """
                message:
                  nopermission: custom permission
                  command:
                    loritime:
                      notfound: custom not found
                  customPlugin:
                    custom_key: custom value
                configSchemaVersion: 1
                """);
        final FileManager fileManager = new FileManager(new LoggerFactory(Logger.getLogger("test")), dataFolder);

        final File languageFile = fileManager.getOrCreateLanguageFile("en-us");
        final Configuration configuration = fileManager.getConfiguration(languageFile);

        assertFalse(rootLanguage.exists(), "Expected root language file to be moved");
        assertEquals(new File(dataFolder, "language/en-us.yml"), languageFile, "Expected canonical language path");
        assertEquals("custom permission", configuration.getString("message.noPermission"),
                "Expected migrated noPermission key to preserve value");
        assertEquals("custom not found", configuration.getString("message.command.loritime.notFound"),
                "Expected migrated notFound key to preserve value");
        assertEquals("custom value", configuration.getString("message.customPlugin.custom_key"),
                "Expected custom language key to be preserved");
        assertNull(configuration.getString("message.nopermission"), "Expected legacy key to be removed");
    }
}
