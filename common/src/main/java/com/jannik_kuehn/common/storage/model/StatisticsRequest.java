package com.jannik_kuehn.common.storage.model;

import com.jannik_kuehn.common.api.storage.TimeRange;
import com.jannik_kuehn.common.api.storage.TimeScope;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** Bounded request for canonical network statistics. */
public record StatisticsRequest(TimeRange range, TimeScope scope, Duration bounceThreshold, Instant observedAt) {
    /** Creates a request whose observation boundary is the selected range end. */
    public StatisticsRequest(final TimeRange range, final TimeScope scope, final Duration bounceThreshold) {
        this(range, scope, bounceThreshold, range.endExclusive());
    }

    public StatisticsRequest {
        Objects.requireNonNull(range, "range");
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(bounceThreshold, "bounceThreshold");
        Objects.requireNonNull(observedAt, "observedAt");
        if (bounceThreshold.isZero() || bounceThreshold.isNegative()) {
            throw new IllegalArgumentException("bounceThreshold must be positive");
        }
    }
}
