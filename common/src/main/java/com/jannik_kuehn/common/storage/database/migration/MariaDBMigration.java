package com.jannik_kuehn.common.storage.database.migration;

import com.github.roleplaycauldron.spellbook.database.updater.DatabaseVersion;

import java.util.List;

/**
 * This class provides a utility for generating MariaDB database schema migrations
 */
public final class MariaDBMigration {

    private MariaDBMigration() {
    }

    /**
     * Builds a list of database version migration scripts tailored to the specified table prefix.
     * This method utilizes the MySQLMigration class to generate the necessary database schema
     * for setting up a graph-based data model in a MariaDB database.
     *
     * @param tablePrefix the prefix to be used for all table names in the generated schema
     * @return a list of {@code DatabaseVersion} objects representing the migration scripts
     */
    public static List<DatabaseVersion> build(final String tablePrefix) {
        return MySQLMigration.build(tablePrefix);
    }
}
