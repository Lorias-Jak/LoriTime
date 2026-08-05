package com.jannik_kuehn.common.config.migration;

import com.jannik_kuehn.common.config.StructuredConfigurationDocument;

import java.util.Locale;

/**
 * config.yml schema migrations.
 */
final class ConfigSchemaConfigMigrations {
    /**
     * Legacy flat-file storage value.
     */
    private static final String LEGACY_YML_STORAGE_METHOD = "yml";

    /**
     * Current local database storage value.
     */
    private static final String SQLITE_STORAGE_METHOD = "sqlite";

    /**
     * Canonical table prefix for LoriTime 2 migrations.
     */
    private static final String CANONICAL_TABLE_PREFIX = "loritime";

    /**
     * Internal marker consumed by the storage migration service.
     */
    private static final String LEGACY_FLAT_FILE_IMPORT_PATH = "storageMigration.legacyFlatFileImport";

    /**
     * Current standalone multi-setup mode value.
     */
    private static final String STANDALONE_MODE = "standalone";

    /**
     * Legacy dot path storing whether multi-setup is enabled.
     */
    private static final String MULTI_SETUP_ENABLED_PATH = "multiSetup.enabled";

    /**
     * Dot path storing the multi-setup mode.
     */
    private static final String MULTI_SETUP_MODE_PATH = "multiSetup.mode";

    private ConfigSchemaConfigMigrations() {
    }

    /* default */
    static ConfigMigration legacySectionedConfig() {
        return ConfigMigration.from(1)
                .operation(ConfigSchemaConfigMigrations::moveLegacyStorageMethod)
                .move("general.checkForUpdates", "updater.checkForUpdates")
                .move("mysql.host", "data.host")
                .move("mysql.port", "data.port")
                .move("mysql.database", "data.database")
                .move("mysql.user", "data.user")
                .move("mysql.password", "data.password")
                .operation(ConfigSchemaConfigMigrations::standardizeTablePrefix)
                .operation(ConfigSchemaConfigMigrations::moveLegacyMultiSetupMode)
                .delete("mysql")
                .build();
    }

    /* default */
    static ConfigMigration statisticsDefaults() {
        return ConfigMigration.from(2)
                .add("stats.default-range", "calendar:today")
                .add("stats.bounce-threshold", "3m")
                .add("stats.calendar-time-zone", "system")
                .build();
    }

    private static void standardizeTablePrefix(final StructuredConfigurationDocument document) {
        document.set("data.tablePrefix", CANONICAL_TABLE_PREFIX);
    }

    private static void moveLegacyStorageMethod(final StructuredConfigurationDocument document) {
        final Object storageMethod = document.get("general.storage");
        if (storageMethod instanceof final String storageString) {
            document.set(LEGACY_FLAT_FILE_IMPORT_PATH, LEGACY_YML_STORAGE_METHOD.equalsIgnoreCase(storageString));
            document.set("storageMethod", normalizeLegacyStorageMethod(storageString));
        }
        document.remove("general.storage");
    }

    private static String normalizeLegacyStorageMethod(final String storageMethod) {
        if (LEGACY_YML_STORAGE_METHOD.equalsIgnoreCase(storageMethod)) {
            return SQLITE_STORAGE_METHOD;
        }
        return storageMethod.toLowerCase(Locale.ROOT);
    }

    private static void moveLegacyMultiSetupMode(final StructuredConfigurationDocument document) {
        final Object enabled = document.get(MULTI_SETUP_ENABLED_PATH);
        final Object mode = document.get(MULTI_SETUP_MODE_PATH);
        if (enabled instanceof final Boolean enabledBoolean) {
            if (enabledBoolean && mode instanceof final String modeString) {
                document.set(MULTI_SETUP_MODE_PATH, modeString.toLowerCase(Locale.ROOT));
            } else {
                document.set(MULTI_SETUP_MODE_PATH, STANDALONE_MODE);
            }
        } else if (mode instanceof final String modeString) {
            document.set(MULTI_SETUP_MODE_PATH, modeString.toLowerCase(Locale.ROOT));
        } else {
            document.set(MULTI_SETUP_MODE_PATH, STANDALONE_MODE);
        }
        document.remove(MULTI_SETUP_ENABLED_PATH);
    }
}
