package com.jannik_kuehn.common.config.migration;

import com.jannik_kuehn.common.config.StructuredConfigurationDocument;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Schema metadata for a versioned configuration file.
 */
public final class ConfigSchema {
    /**
     * Dot path storing the config schema version.
     */
    public static final String VERSION_PATH = "configSchemaVersion";

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

    /**
     * Baseline version used for unversioned files.
     */
    private final int legacyBaseline;

    /**
     * Latest supported schema version.
     */
    private final int latest;

    /**
     * Migrations ordered by source version.
     */
    private final List<ConfigMigration> orderedMigrations;

    /**
     * Creates schema metadata.
     *
     * @param legacyBaselineVersion baseline for unversioned files
     * @param latestVersion         latest schema version
     * @param migrations            ordered migrations
     */
    public ConfigSchema(final int legacyBaselineVersion, final int latestVersion, final List<ConfigMigration> migrations) {
        this.legacyBaseline = legacyBaselineVersion;
        this.latest = latestVersion;
        this.orderedMigrations = migrations.stream()
                .sorted(Comparator.comparingInt(ConfigMigration::getFromVersion))
                .toList();
    }

    /**
     * Returns the schema for LoriTime config.yml.
     *
     * @return LoriTime config schema
     */
    public static ConfigSchema loriTimeConfig() {
        return new ConfigSchema(1, 2, List.of(legacySectionedConfigMigration()));
    }

    /**
     * Returns the schema for bundled localization files.
     *
     * @return localization schema
     */
    public static ConfigSchema localization() {
        return new ConfigSchema(0, 1, List.of());
    }

    private static ConfigMigration legacySectionedConfigMigration() {
        return ConfigMigration.from(1)
                .operation(ConfigSchema::moveLegacyStorageMethod)
                .move("general.checkForUpdates", "updater.checkForUpdates")
                .move("mysql.host", "data.host")
                .move("mysql.port", "data.port")
                .move("mysql.database", "data.database")
                .move("mysql.user", "data.user")
                .move("mysql.password", "data.password")
                .operation(ConfigSchema::standardizeTablePrefix)
                .operation(ConfigSchema::moveLegacyMultiSetupMode)
                .delete("mysql")
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

    /**
     * Returns the baseline version used for unversioned files.
     *
     * @return legacy baseline version
     */
    public int legacyBaselineVersion() {
        return legacyBaseline;
    }

    /**
     * Returns the latest schema version.
     *
     * @return latest version
     */
    public int latestVersion() {
        return latest;
    }

    /**
     * Returns ordered migrations.
     *
     * @return migrations
     */
    public List<ConfigMigration> migrations() {
        return orderedMigrations;
    }
}
