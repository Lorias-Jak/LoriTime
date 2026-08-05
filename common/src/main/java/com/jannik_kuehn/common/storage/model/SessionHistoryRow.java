package com.jannik_kuehn.common.storage.model;

import java.time.Instant;
import java.util.UUID;

/** Bounded session projection used by statistics aggregation. */
public record SessionHistoryRow(UUID playerId, String playerName, String server, String world,
                                Instant joinedAt, Instant leftAt, TimeEntryReason reason, Instant firstJoin) {
}
