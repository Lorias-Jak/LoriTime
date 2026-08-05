package com.jannik_kuehn.common.config.localization;

import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"PMD.UnitTestAssertionsShouldIncludeMessage", "PMD.UnitTestContainsTooManyAsserts",
        "PMD.SignatureDeclareThrowsException"})
class LocalizationTest {
    @TempDir
    private File dataFolder;

    @Test
    void loadsDefaultAndLazyLoadsExistingCustomLanguage() throws Exception {
        writeLanguage("en-us", "English");
        writeLanguage("pirate", "Ahoy");
        final Localization localization = localization("en-us");

        assertEquals("English", localization.getRawMessage("message.test"));
        assertEquals("Ahoy", localization.getRawMessage("pirate", "message.test"));
    }

    @Test
    void missingCustomLanguageFallsBackToDefault() throws Exception {
        writeLanguage("en-us", "English");
        final Localization localization = localization("en-us");

        assertEquals("English", localization.getRawMessage("pirate", "message.test"));
    }

    @Test
    void invalidCustomLanguageFallsBackToDefault() throws Exception {
        writeLanguage("en-us", "English");
        final File languageFolder = new File(dataFolder, "language");
        Files.createDirectories(languageFolder.toPath());
        Files.writeString(new File(languageFolder, "pirate.yml").toPath(), """
                locale: 'pirate'
                prefix: '<gray>LoriTime '
                messages:
                  message.test: 'invalid'
                """);
        final Localization localization = localization("en-us");

        assertEquals("English", localization.getRawMessage("pirate", "message.test"));
    }

    @Test
    void reloadKeepsPreviouslyLoadedCustomLanguage() throws Exception {
        writeLanguage("en-us", "English");
        writeLanguage("pirate", "Ahoy");
        final Localization localization = localization("en-us");
        assertEquals("Ahoy", localization.getRawMessage("pirate", "message.test"));

        writeLanguage("pirate", "Yarr");
        writeLanguage("unused", "Unused");
        localization.reload();

        assertEquals("Yarr", localization.getRawMessage("pirate", "message.test"));
        assertEquals("English", localization.getRawMessage("message.test"));
    }

    @Test
    void reloadWithNewDefaultLanguageUsesNewDefaultForNonPlayerMessages() throws Exception {
        writeLanguage("en-us", "English");
        writeLanguage("de-de", "Deutsch");
        final Localization localization = localization("en-us");
        assertEquals("English", localization.getRawMessage("message.test"));

        localization.reload("de-de");

        assertEquals("Deutsch", localization.getRawMessage("message.test"));
    }

    @Test
    void incompleteDefaultLanguageFallsBackToHardFallbackAndReportsDegradedHealth() throws Exception {
        writeLanguage("en-us", "English");
        writeIncompleteLanguage("de-de", "Deutsch");

        final Localization localization = localization("de-de");

        assertEquals(Localization.HealthState.DEGRADED, localization.healthState());
        assertEquals("Deutsch", localization.getRawMessage("message.test"));
        assertEquals("English fallback", localization.getRawMessage("message.fallbackOnly"));
        assertEquals("s", localization.getStringList("unit.second.identifier").getFirst());
    }

    @Test
    void completedLanguageReloadRestoresReadyHealth() throws Exception {
        writeLanguage("en-us", "English");
        writeIncompleteLanguage("de-de", "Deutsch");
        final Localization localization = localization("de-de");
        assertEquals(Localization.HealthState.DEGRADED, localization.healthState());

        writeLanguage("de-de", "Deutsch");
        localization.reload();

        assertEquals(Localization.HealthState.READY, localization.healthState());
        assertEquals("Deutsch fallback", localization.getRawMessage("message.fallbackOnly"));
    }

    private Localization localization(final String language) {
        final LoggerFactory loggerFactory = new LoggerFactory(Logger.getLogger("test"));
        return new Localization(loggerFactory.create(Localization.class), dataFolder, language);
    }

    private void writeLanguage(final String locale, final String message) throws Exception {
        final File languageFolder = new File(dataFolder, "language");
        Files.createDirectories(languageFolder.toPath());
        Files.writeString(new File(languageFolder, locale + ".yml").toPath(), """
                schema_version: 3
                locale: '%s'
                prefix: '<gray>LoriTime '
                messages:
                  message.test: '%s'
                  message.fallbackOnly: '%s fallback'
                  unit.second.identifier: ['s']
                """.formatted(locale, message, message));
    }

    private void writeIncompleteLanguage(final String locale, final String message) throws Exception {
        final File languageFolder = new File(dataFolder, "language");
        Files.createDirectories(languageFolder.toPath());
        Files.writeString(new File(languageFolder, locale + ".yml").toPath(), """
                schema_version: 3
                locale: '%s'
                prefix: '<gray>LoriTime '
                messages:
                  message.test: '%s'
                """.formatted(locale, message));
    }
}
