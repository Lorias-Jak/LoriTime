package com.jannik_kuehn.common.config.migration;

import java.util.Comparator;
import java.util.List;

/**
 * Schema metadata for a versioned configuration file.
 */
public final class ConfigSchema {
    /**
     * Dot path storing the config schema version.
     */
    public static final String VERSION_PATH = "configSchemaVersion";

    /**
     * Baseline version used for unversioned files.
     */
    private final int legacyBaseline;

    /**
     * Dot path storing this schema's version.
     */
    private final String schemaVersionPath;

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
        this(legacyBaselineVersion, latestVersion, VERSION_PATH, migrations);
    }

    /**
     * Creates schema metadata.
     *
     * @param legacyBaselineVersion baseline for unversioned files
     * @param latestVersion         latest schema version
     * @param versionPath           path storing the schema version
     * @param migrations            ordered migrations
     */
    public ConfigSchema(final int legacyBaselineVersion,
                        final int latestVersion,
                        final String versionPath,
                        final List<ConfigMigration> migrations) {
        this.legacyBaseline = legacyBaselineVersion;
        this.latest = latestVersion;
        this.schemaVersionPath = versionPath;
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
        return new ConfigSchema(1, 3, List.of(
                ConfigSchemaConfigMigrations.legacySectionedConfig(),
                ConfigSchemaConfigMigrations.statisticsDefaults()));
    }

    /**
     * Returns the schema for bundled localization files.
     *
     * @return localization schema
     */
    public static ConfigSchema localization() {
        return new ConfigSchema(0, 3, "schema_version", List.of(
                ConfigSchemaLocalizationMigrations.transferWarning(),
                ConfigSchemaLocalizationMigrations.statisticsMessages()));
    }

    /**
     * Returns the schema for bundled command aliases.
     *
     * @return commands schema
     */
    public static ConfigSchema commands() {
        return new ConfigSchema(0, 1, "commandSchemaVersion", List.of(
                ConfigSchemaCommandMigrations.statisticsCommandAliases()));
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
     * Returns the path storing the schema version.
     *
     * @return schema version path
     */
    public String versionPath() {
        return schemaVersionPath;
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
