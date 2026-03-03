package com.jannik_kuehn.common.storage.database;

import com.github.roleplaycauldron.spellbook.database.updater.DatabaseVersion;
import com.github.roleplaycauldron.spellbook.database.updater.VersionRepository;

import java.util.ArrayList;
import java.util.List;

public class DatabaseVersionRepository extends VersionRepository {

    private final List<DatabaseVersion> versions;

    public DatabaseVersionRepository(final String tablePrefix, final DatabaseDialect dialect) {
        super();
        this.versions = new ArrayList<>();

        versions.add(new MigrationOne(tablePrefix, dialect));
    }

    @Override
    public List<DatabaseVersion> getAllVersions() {
        return versions;
    }

    private record MigrationOne(String tablePrefix, DatabaseDialect dialect) implements DatabaseVersion {

        @Override
        public int getVersionNumber() {
            return 1;
        }

        @Override
        public List<String> getUpgradeQueries() {
            final List<String> queries = new ArrayList<>();

            final String playerTable = tablePrefix + "_player";
            final String serverTable = tablePrefix + "_server";
            final String worldTable = tablePrefix + "_world";
            final String timeTable = tablePrefix + "_time";
            final String statisticTable = tablePrefix + "_statistic";
            queries.add(dialect.createPlayerTable(playerTable));
            queries.add(dialect.createServerTable(serverTable));
            queries.add(dialect.createWorldTable(worldTable, serverTable));
            queries.add(dialect.createTimeTable(timeTable, playerTable, worldTable));
            queries.add(dialect.createStatisticTable(statisticTable));

            queries.add("INSERT IGNORE INTO `" + serverTable + "` (`server`) VALUES ('default')");
            queries.add(
                    "INSERT IGNORE INTO `" + worldTable + "` (`server_id`, `world`) "
                            + "SELECT s.`id`, 'world' FROM `" + serverTable + "` s WHERE s.`server`='default'"
            );
            queries.add(
                    "INSERT IGNORE INTO `" + playerTable + "` (`uuid`, `name`, `last_seen`) "
                            + "SELECT o.`uuid`, o.`name`, NOW() FROM `old_table` o"
            );
            queries.add(
                    "INSERT INTO `" + timeTable + "` (`player_id`, `world_id`, `join_time`, `leave_time`, `reason`) "
                            + "SELECT p.`id`, w.`id`, DATE_SUB(NOW(3), INTERVAL o.`time` SECOND), NOW(3), 'MIGRATED' "
                            + "FROM `old_table` o "
                            + "JOIN `" + playerTable + "` p ON p.`uuid` = o.`uuid` "
                            + "JOIN `" + serverTable + "` s ON s.`server`='default' "
                            + "JOIN `" + worldTable + "` w ON w.`server_id`=s.`id` AND w.`world`='world' "
                            + "WHERE o.`time` > 0"
            );
            return queries;
        }
    }
}
