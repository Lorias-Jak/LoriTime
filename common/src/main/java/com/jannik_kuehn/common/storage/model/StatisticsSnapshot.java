package com.jannik_kuehn.common.storage.model;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Immutable result of one bounded statistics query. */
public record StatisticsSnapshot(long uniqueUsers, long newUsers, long sessions, Duration totalPlayTime,
                                 Duration medianSession, Duration longestSession, double sessionsPerUser,
                                 long bounces, double bounceRate, long peakConcurrent, long afkPlayers,
                                 long afkKicks, Duration afkDuration, long afkPeriods, double retentionRate,
                                 Map<String, Duration> serverUsage, Map<String, Duration> worldUsage,
                                 List<PlayerTotal> topPlayers) {
    /** Ranked player duration. */
    public record PlayerTotal(UUID playerId, String name, Duration duration) {
    }
}
