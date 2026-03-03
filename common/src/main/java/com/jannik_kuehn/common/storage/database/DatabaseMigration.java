package com.jannik_kuehn.common.storage.database;

import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import com.github.roleplaycauldron.spellbook.database.ConnectionProvider;
import com.github.roleplaycauldron.spellbook.database.updater.DatabaseUpdater;
import com.github.roleplaycauldron.spellbook.database.updater.VersionRepository;

public class DatabaseMigration {

    private final WrappedLogger log;

    private final DatabaseUpdater updater;

    public DatabaseMigration(final LoggerFactory loggerFactory, final ConnectionProvider provider,
                             final String tablePrefix, final DatabaseDialect dialect, final boolean firstStartup) {
        this.log = loggerFactory.create(DatabaseMigration.class);
        final VersionRepository versionRepository = new DatabaseVersionRepository(tablePrefix, dialect);
        this.updater = new DatabaseUpdater(loggerFactory.create(DatabaseUpdater.class), provider, versionRepository, tablePrefix + "_version");
    }
}
