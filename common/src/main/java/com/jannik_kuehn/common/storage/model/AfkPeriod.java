package com.jannik_kuehn.common.storage.model;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** One durable AFK interval in a canonical server/world scope. */
@SuppressWarnings("PMD.ShortVariable")
public record AfkPeriod(long id, UUID playerId, String server, String world, Instant startedAt,
                        Optional<Instant> endedAt, Optional<AfkPeriodEndReason> endReason) {
}
