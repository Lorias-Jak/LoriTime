package com.jannik_kuehn.common.storage.statistics;

import com.jannik_kuehn.common.storage.model.AfkPeriod;
import com.jannik_kuehn.common.storage.model.AfkPeriodEndReason;
import com.jannik_kuehn.common.storage.model.SessionHistoryRow;
import com.jannik_kuehn.common.storage.model.StatisticsRequest;
import com.jannik_kuehn.common.storage.model.StatisticsSnapshot;
import com.jannik_kuehn.common.storage.model.TimeEntryReason;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Dialect-neutral aggregation of bounded canonical history. */
@SuppressWarnings({"PMD.CouplingBetweenObjects", "PMD.TooManyMethods", "PMD.CommentRequired",
        "PMD.NPathComplexity", "PMD.ConfusingTernary"})
public final class StatisticsAggregator {
    /** Maximum event-ordering gap merged across context switches. */
    public static final Duration CONTEXT_SWITCH_TOLERANCE = Duration.ofSeconds(30);

    private static final Duration RETENTION_WINDOW = Duration.ofDays(7);

    private StatisticsAggregator() {
    }

    /** Aggregates session and AFK projections for one request. */
    public static StatisticsSnapshot aggregate(final StatisticsRequest request,
                                               final List<SessionHistoryRow> input,
                                               final List<AfkPeriod> afkPeriods) {
        final List<SessionHistoryRow> rows = input.stream()
                .sorted(Comparator.comparing(SessionHistoryRow::playerId).thenComparing(SessionHistoryRow::joinedAt))
                .toList();
        final Set<UUID> users = new HashSet<>();
        final Set<UUID> newUsers = new HashSet<>();
        final Map<UUID, Long> playerSeconds = new HashMap<>();
        final Map<String, Long> serverSeconds = new HashMap<>();
        final Map<String, Long> worldSeconds = new HashMap<>();
        final List<Event> events = new ArrayList<>();
        for (final SessionHistoryRow row : rows) {
            final Instant start = later(row.joinedAt(), request.range().startInclusive());
            final Instant end = earlier(row.leftAt(), request.range().endExclusive());
            if (end.isAfter(start)) {
                final long seconds = Duration.between(start, end).getSeconds();
                users.add(row.playerId());
                playerSeconds.merge(row.playerId(), seconds, Long::sum);
                serverSeconds.merge(row.server(), seconds, Long::sum);
                worldSeconds.merge(row.server() + "/" + row.world(), seconds, Long::sum);
                events.add(new Event(start, row.playerId(), 1));
                events.add(new Event(end, row.playerId(), -1));
            }
            if (!row.firstJoin().isBefore(request.range().startInclusive())
                    && row.firstJoin().isBefore(request.range().endExclusive())) {
                newUsers.add(row.playerId());
            }
        }

        final List<LogicalSession> sessions = logicalSessions(rows, afkPeriods);
        final List<LogicalSession> selectedSessions = sessions.stream()
                .filter(session -> request.range().contains(session.start))
                .toList();
        final List<Long> durations = selectedSessions.stream()
                .map(session -> Duration.between(session.start, session.end).getSeconds())
                .sorted().toList();
        final long bounces = selectedSessions.stream().filter(session -> session.completed)
                .map(session -> Duration.between(session.start, session.end).getSeconds())
                .filter(value -> value < request.bounceThreshold().getSeconds()).count();
        final long totalSeconds = playerSeconds.values().stream().mapToLong(Long::longValue).sum();
        final long longest = durations.isEmpty() ? 0L : durations.get(durations.size() - 1);
        final long median = median(durations);

        final Set<UUID> afkUsers = new HashSet<>();
        long afkSeconds = 0L;
        long afkKicks = 0L;
        for (final AfkPeriod period : afkPeriods) {
            afkUsers.add(period.playerId());
            final Instant end = period.endedAt().orElse(request.range().endExclusive());
            afkSeconds += Math.max(0L, Duration.between(later(period.startedAt(), request.range().startInclusive()),
                    earlier(end, request.range().endExclusive())).getSeconds());
            final Instant eventStart = later(period.startedAt(), request.range().startInclusive());
            final Instant eventEnd = earlier(end, request.range().endExclusive());
            if (eventEnd.isAfter(eventStart)) {
                events.add(new Event(eventStart, period.playerId(), 1));
                events.add(new Event(eventEnd, period.playerId(), -1));
            }
            if (period.endReason().orElse(null) == AfkPeriodEndReason.KICKED) {
                afkKicks++;
            }
        }

        final List<StatisticsSnapshot.PlayerTotal> top = playerSeconds.entrySet().stream()
                .sorted(Map.Entry.<UUID, Long>comparingByValue().reversed())
                .map(entry -> new StatisticsSnapshot.PlayerTotal(entry.getKey(), name(rows, entry.getKey()),
                        Duration.ofSeconds(entry.getValue())))
                .toList();
        final long retained = newUsers.stream().filter(uuid -> retained(uuid, sessions, request.range().endExclusive())).count();
        final long matured = newUsers.stream().filter(uuid -> firstJoin(rows, uuid).plus(RETENTION_WINDOW)
                .isBefore(request.range().endExclusive())).count();
        return new StatisticsSnapshot(users.size(), newUsers.size(), durations.size(), Duration.ofSeconds(totalSeconds),
                Duration.ofSeconds(median), Duration.ofSeconds(longest), ratio(durations.size(), users.size()), bounces,
                ratio(bounces, durations.size()), peak(events), afkUsers.size(), afkKicks, Duration.ofSeconds(afkSeconds),
                afkPeriods.size(), ratio(retained, matured), durations(serverSeconds), durations(worldSeconds), top);
    }

    private static List<LogicalSession> logicalSessions(final List<SessionHistoryRow> rows,
                                                        final List<AfkPeriod> afkPeriods) {
        final List<LogicalSession> result = new ArrayList<>();
        LogicalSession current = null;
        for (final SessionHistoryRow row : rows) {
            final boolean contextContinues = current != null && current.switchEnded
                    && !row.joinedAt().isAfter(current.end.plus(CONTEXT_SWITCH_TOLERANCE));
            final boolean afkContinues = current != null && current.afkEnded
                    && resumedNear(current.player, row.joinedAt(), afkPeriods);
            final boolean continues = current != null && current.player.equals(row.playerId())
                    && (contextContinues || afkContinues);
            if (!continues) {
                if (current != null) {
                    result.add(current);
                }
                current = new LogicalSession(row.playerId(), row.joinedAt(), row.leftAt(), endsNetwork(row.reason()),
                        isSwitch(row.reason()), row.reason() == TimeEntryReason.PLAYER_AFK);
            } else {
                current.end = later(current.end, row.leftAt());
                current.completed = endsNetwork(row.reason());
                current.switchEnded = isSwitch(row.reason());
                current.afkEnded = row.reason() == TimeEntryReason.PLAYER_AFK;
            }
        }
        if (current != null) {
            result.add(current);
        }
        return result;
    }

    private static boolean resumedNear(final UUID player, final Instant nextJoin, final List<AfkPeriod> periods) {
        return periods.stream().filter(period -> period.playerId().equals(player))
                .filter(period -> period.endReason().orElse(null) == AfkPeriodEndReason.RESUMED)
                .flatMap(period -> period.endedAt().stream())
                .anyMatch(end -> !nextJoin.isBefore(end.minus(CONTEXT_SWITCH_TOLERANCE))
                        && !nextJoin.isAfter(end.plus(CONTEXT_SWITCH_TOLERANCE)));
    }

    private static boolean isSwitch(final TimeEntryReason reason) {
        return reason == TimeEntryReason.SERVER_SWITCH || reason == TimeEntryReason.WORLD_SWITCH;
    }

    private static boolean endsNetwork(final TimeEntryReason reason) {
        return reason == TimeEntryReason.PLAYER_LEAVE || reason == TimeEntryReason.PLAYER_AFK_KICK
                || reason == TimeEntryReason.SHUTDOWN_FLUSH;
    }

    private static long peak(final List<Event> events) {
        events.sort(Comparator.comparing(Event::time).thenComparingInt(Event::delta));
        final Map<UUID, Integer> online = new HashMap<>();
        long peak = 0L;
        for (final Event event : events) {
            if (event.delta > 0) {
                online.merge(event.player, 1, Integer::sum);
            } else {
                online.computeIfPresent(event.player, (ignored, count) -> count <= 1 ? null : count - 1);
            }
            peak = Math.max(peak, online.size());
        }
        return peak;
    }

    private static long median(final List<Long> values) {
        if (values.isEmpty()) {
            return 0L;
        }
        final int middle = values.size() / 2;
        return values.size() % 2 == 1 ? values.get(middle) : (values.get(middle - 1) + values.get(middle)) / 2L;
    }

    private static boolean retained(final UUID uuid, final List<LogicalSession> sessions, final Instant rangeEnd) {
        final List<LogicalSession> player = sessions.stream().filter(s -> s.player.equals(uuid)).toList();
        return player.size() > 1 && !player.get(1).start.isAfter(player.get(0).start.plus(RETENTION_WINDOW))
                && player.get(0).start.plus(RETENTION_WINDOW).isBefore(rangeEnd);
    }

    private static Instant firstJoin(final List<SessionHistoryRow> rows, final UUID uuid) {
        return rows.stream().filter(row -> row.playerId().equals(uuid)).findFirst().orElseThrow().firstJoin();
    }

    private static String name(final List<SessionHistoryRow> rows, final UUID uuid) {
        return rows.stream().filter(row -> row.playerId().equals(uuid)).map(SessionHistoryRow::playerName)
                .filter(value -> value != null && !value.isBlank()).findFirst().orElse(uuid.toString());
    }

    private static Map<String, Duration> durations(final Map<String, Long> values) {
        final Map<String, Duration> result = new LinkedHashMap<>();
        values.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> result.put(entry.getKey(), Duration.ofSeconds(entry.getValue())));
        return Map.copyOf(result);
    }

    private static double ratio(final long numerator, final long denominator) {
        return denominator == 0L ? 0.0D : (double) numerator / denominator;
    }

    private static Instant later(final Instant left, final Instant right) {
        return left.isAfter(right) ? left : right;
    }

    private static Instant earlier(final Instant left, final Instant right) {
        return left.isBefore(right) ? left : right;
    }

    private record Event(Instant time, UUID player, int delta) {
    }

    private static final class LogicalSession {
        private final UUID player;

        private final Instant start;

        private Instant end;

        private boolean completed;

        private boolean switchEnded;

        private boolean afkEnded;

        private LogicalSession(final UUID player, final Instant start, final Instant end,
                               final boolean completed, final boolean switchEnded, final boolean afkEnded) {
            this.player = player;
            this.start = start;
            this.end = end;
            this.completed = completed;
            this.switchEnded = switchEnded;
            this.afkEnded = afkEnded;
        }
    }
}
