package com.jannik_kuehn.common.storage.statistics;

import com.jannik_kuehn.common.api.storage.TimeRange;
import com.jannik_kuehn.common.api.storage.TimeScope;
import com.jannik_kuehn.common.storage.model.AfkPeriod;
import com.jannik_kuehn.common.storage.model.AfkPeriodEndReason;
import com.jannik_kuehn.common.storage.model.SessionHistoryRow;
import com.jannik_kuehn.common.storage.model.StatisticsRequest;
import com.jannik_kuehn.common.storage.model.TimeEntryReason;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"PMD.UnitTestContainsTooManyAsserts", "PMD.UseExplicitTypes",
        "PMD.UnitTestAssertionsShouldIncludeMessage"})
class StatisticsAggregatorTest {
    private static final UUID PLAYER = UUID.fromString("44174cf6-e76c-4994-899c-3387284ecd62");

    private static final UUID OTHER = UUID.fromString("a44f7fed-d003-4de6-b620-191c0fc22bb7");

    private static final Instant START = Instant.parse("2026-07-10T10:00:00Z");

    @Test
    void mergesSwitchSegmentsAndCalculatesBounceMedianUsageAndPeak() {
        final List<SessionHistoryRow> rows = List.of(
                row(PLAYER, "Lorias_", "survival", "world", 0, 60, TimeEntryReason.WORLD_SWITCH, START),
                row(PLAYER, "Lorias_", "survival", "nether", 60, 120, TimeEntryReason.PLAYER_LEAVE, START),
                row(OTHER, "Other", "creative", "plots", 30, 90, TimeEntryReason.PLAYER_LEAVE, START.plusSeconds(30)));
        final AfkPeriod afk = new AfkPeriod(1, PLAYER, "survival", "nether", START.plusSeconds(70),
                Optional.of(START.plusSeconds(80)), Optional.of(AfkPeriodEndReason.KICKED));

        final var result = StatisticsAggregator.aggregate(new StatisticsRequest(
                        TimeRange.between(START, START.plusSeconds(180)), TimeScope.GLOBAL, Duration.ofSeconds(90)),
                rows, List.of(afk));

        assertEquals(2, result.uniqueUsers());
        assertEquals(2, result.sessions(), "Expected switched segments to form one session");
        assertEquals(Duration.ofSeconds(180), result.totalPlayTime());
        assertEquals(Duration.ofSeconds(90), result.medianSession());
        assertEquals(1, result.bounces());
        assertEquals(2, result.peakConcurrent());
        assertEquals(1, result.afkKicks());
        assertEquals(Duration.ofSeconds(10), result.afkDuration());
        assertEquals(Duration.ofSeconds(120), result.serverUsage().get("survival"));
    }

    @Test
    void clipsIntervalsAndExcludesIncompleteBounceCandidates() {
        final var row = row(PLAYER, "Lorias_", "survival", "world", -60, 60,
                TimeEntryReason.AUTO_FLUSH, START.minusSeconds(60));
        final var result = StatisticsAggregator.aggregate(new StatisticsRequest(
                        TimeRange.between(START, START.plusSeconds(30)), TimeScope.GLOBAL, Duration.ofMinutes(3)),
                List.of(row), List.of());

        assertEquals(Duration.ofSeconds(30), result.totalPlayTime());
        assertEquals(0, result.sessions());
        assertEquals(0, result.bounces());
    }

    @Test
    void resumedAfkPeriodKeepsOneLogicalNetworkSession() {
        final List<SessionHistoryRow> rows = List.of(
                row(PLAYER, "Lorias_", "survival", "world", 0, 60, TimeEntryReason.PLAYER_AFK, START),
                row(PLAYER, "Lorias_", "survival", "world", 180, 240, TimeEntryReason.PLAYER_LEAVE, START));
        final AfkPeriod afk = new AfkPeriod(1, PLAYER, "survival", "world", START.plusSeconds(60),
                Optional.of(START.plusSeconds(180)), Optional.of(AfkPeriodEndReason.RESUMED));

        final var result = StatisticsAggregator.aggregate(new StatisticsRequest(
                        TimeRange.between(START, START.plusSeconds(300)), TimeScope.GLOBAL, Duration.ofSeconds(90)),
                rows, List.of(afk));

        assertEquals(1, result.sessions());
        assertEquals(Duration.ofSeconds(240), result.longestSession(), "Network session includes the AFK interval");
        assertEquals(1, result.peakConcurrent(), "AFK player remains online on the network");
    }

    @Test
    void activeSessionCountsInSessionMetricsButNotBounces() {
        final var active = row(PLAYER, "Lorias_", "survival", "world", 10, 130,
                TimeEntryReason.AUTO_FLUSH, START.plusSeconds(10));

        final var result = StatisticsAggregator.aggregate(new StatisticsRequest(
                TimeRange.between(START, START.plusSeconds(180)), TimeScope.GLOBAL, Duration.ofMinutes(3),
                START.plusSeconds(130)), List.of(active), List.of());

        assertEquals(1, result.uniqueUsers());
        assertEquals(1, result.sessions());
        assertEquals(Duration.ofSeconds(120), result.medianSession());
        assertEquals(Duration.ofSeconds(120), result.longestSession());
        assertEquals(0, result.bounces(), "Active sessions must not be classified as bounces");
        assertEquals(1, result.peakConcurrent());
    }

    @Test
    void calculatesRetentionOnlyForMaturedNewUsersWhoReturnWithinSevenDays() {
        final List<SessionHistoryRow> rows = List.of(
                row(PLAYER, "Lorias_", "survival", "world", 0, 60, TimeEntryReason.PLAYER_LEAVE, START),
                row(PLAYER, "Lorias_", "survival", "world", 6 * 86_400, 6 * 86_400 + 60,
                        TimeEntryReason.PLAYER_LEAVE, START),
                row(OTHER, "Other", "creative", "plots", 0, 60, TimeEntryReason.PLAYER_LEAVE, START));

        final var result = StatisticsAggregator.aggregate(new StatisticsRequest(
                        TimeRange.between(START, START.plus(Duration.ofDays(8))), TimeScope.GLOBAL, Duration.ofMinutes(3)),
                rows, List.of());

        assertEquals(2, result.newUsers());
        assertEquals(0.5D, result.retentionRate());
    }

    private SessionHistoryRow row(final UUID uuid, final String name, final String server, final String world,
                                  final long joined, final long left, final TimeEntryReason reason,
                                  final Instant firstJoin) {
        return new SessionHistoryRow(uuid, name, server, world, START.plusSeconds(joined), START.plusSeconds(left),
                reason, firstJoin);
    }
}
