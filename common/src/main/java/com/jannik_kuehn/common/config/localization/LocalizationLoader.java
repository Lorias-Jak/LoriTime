package com.jannik_kuehn.common.config.localization;

import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reads and validates localization files from plugin storage.
 */
final class LocalizationLoader {
    /**
     * The schema version of the localization file format.
     */
    private static final int SUPPORTED_SCHEMA_VERSION = 3;

    /**
     * The key for the prefix field in the localization file.
     */
    private static final String PREFIX_KEY = "prefix";

    /**
     * The path to the messages section in the localization file.
     */
    private static final String MESSAGES_PATH = "messages";

    /**
     * Prefix for rejected locale file messages.
     */
    private static final String INVALID_FILE_PREFIX = "Rejected locale file '";

    /**
     * Logger for localization file diagnostics.
     */
    private final WrappedLogger log;

    /**
     * The plugin data folder.
     */
    private final File dataFolder;

    /**
     * Creates a localization file loader.
     *
     * @param log        logger for rejected locale files
     * @param dataFolder plugin data folder
     */
    /* default */ LocalizationLoader(final WrappedLogger log, final File dataFolder) {
        this.log = log;
        this.dataFolder = dataFolder;
    }

    /**
     * Loads locale data for a language tag.
     *
     * @param languageTag language tag to load
     * @return parsed locale data, or null when the file is missing or invalid
     */
    /* default */ LocaleData load(final String languageTag) {
        final String normalizedTag = LocalizationTags.normalize(languageTag);
        final File languageFile = new File(languageFolder(), normalizedTag + ".yml");
        if (!languageFile.exists()) {
            log.warn("Language file '" + languageFile.getName() + "' does not exist.");
            return null;
        }
        final LocaleData data = parseLocaleFile(languageFile);
        if (data != null) {
            log.info("Loaded language: " + data.tag() + " (" + data.messages().size() + " messages)");
        }
        return data;
    }

    private File languageFolder() {
        final File langDir = new File(dataFolder, "language");
        if (!langDir.exists() && !langDir.mkdirs()) {
            log.error("Could not create languages directory.");
        }
        return langDir;
    }

    @SuppressWarnings("PMD.CyclomaticComplexity")
    private LocaleData parseLocaleFile(final File file) {
        final Map<?, ?> config = loadYaml(file);
        if (config == null) {
            return null;
        }
        final int schemaVersion = config.get("schema_version") instanceof final Number version ? version.intValue() : -1;
        if (schemaVersion != SUPPORTED_SCHEMA_VERSION) {
            log.error(INVALID_FILE_PREFIX + file.getName() + "': unsupported schema_version=" + schemaVersion);
            return null;
        }

        final Object localeValue = config.get("locale");
        final String configuredTag = localeValue instanceof final String value ? value : null;
        if (configuredTag == null || configuredTag.isBlank()) {
            log.error(INVALID_FILE_PREFIX + file.getName() + "': missing locale field.");
            return null;
        }

        final Object prefixValue = config.get(PREFIX_KEY);
        final String prefix = prefixValue instanceof final String value ? value : null;
        if (prefix == null || prefix.isBlank()) {
            log.error(INVALID_FILE_PREFIX + file.getName() + "': missing prefix field.");
            return null;
        }

        final Object messagesSection = config.get(MESSAGES_PATH);
        if (!(messagesSection instanceof final Map<?, ?> messagesMap)) {
            log.error(INVALID_FILE_PREFIX + file.getName() + "': missing '" + MESSAGES_PATH + "' section.");
            return null;
        }

        final Map<String, String> localeMessages = new ConcurrentHashMap<>();
        final Map<String, List<String>> localeLists = new ConcurrentHashMap<>();
        localeMessages.put(PREFIX_KEY, prefix);
        flattenMessages("", messagesMap, localeMessages, localeLists);
        return new LocaleData(LocalizationTags.normalize(configuredTag), localeMessages, localeLists);
    }

    @SuppressWarnings("PMD.ReturnEmptyCollectionRatherThanNull")
    private Map<?, ?> loadYaml(final File file) {
        try (InputStream inputStream = Files.newInputStream(file.toPath())) {
            final Object loaded = new Yaml().load(inputStream);
            if (loaded instanceof final Map<?, ?> map) {
                return map;
            }
            log.error(INVALID_FILE_PREFIX + file.getName() + "': root must be a YAML section.");
        } catch (final IOException | YAMLException ex) {
            log.error(INVALID_FILE_PREFIX + file.getName() + "': could not read YAML.", ex);
        }
        return null;
    }

    private void flattenMessages(final String prefix, final Map<?, ?> source, final Map<String, String> messages,
                                 final Map<String, List<String>> lists) {
        for (final Map.Entry<?, ?> entry : source.entrySet()) {
            if (!(entry.getKey() instanceof final String key)) {
                continue;
            }
            final String path = prefix.isBlank() ? key : prefix + "." + key;
            if (entry.getValue() instanceof final String value) {
                messages.put(path, value);
            } else if (entry.getValue() instanceof final List<?> values) {
                lists.put(path, values.stream()
                        .filter(Objects::nonNull)
                        .map(Object::toString)
                        .toList());
            } else if (entry.getValue() instanceof final Map<?, ?> child) {
                flattenMessages(path, child, messages, lists);
            }
        }
    }

    /**
     * Represents a container for a specific set of localized messages associated
     * with a particular language tag.
     *
     * @param tag      the language tag for which the messages are intended.
     * @param messages a map of message keys to their corresponding localized
     *                 strings for the specified language tag.
     * @param lists    a map of message keys to localized string lists.
     */
    /* default */ record LocaleData(String tag, Map<String, String> messages, Map<String, List<String>> lists) {
    }
}
