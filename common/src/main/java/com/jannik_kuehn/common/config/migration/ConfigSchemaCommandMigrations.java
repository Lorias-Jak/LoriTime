package com.jannik_kuehn.common.config.migration;

import java.util.List;

/**
 * commands.yml schema migrations.
 */
final class ConfigSchemaCommandMigrations {
    /**
     * Default statistics command name.
     */
    private static final String STATS_COMMAND_NAME = "ltstats";

    /**
     * Default statistics command aliases.
     */
    private static final List<String> STATS_COMMAND_ALIASES = List.of("stats", "loritimestats");

    private ConfigSchemaCommandMigrations() {
    }

    /* default */
    static ConfigMigration statisticsCommandAliases() {
        return ConfigMigration.from(0)
                .add("profiles.proxy.stats.name", STATS_COMMAND_NAME)
                .add("profiles.proxy.stats.aliases", STATS_COMMAND_ALIASES)
                .add("profiles.backend.canonical.stats.name", STATS_COMMAND_NAME)
                .add("profiles.backend.canonical.stats.aliases", STATS_COMMAND_ALIASES)
                .build();
    }
}
