package com.jannik_kuehn.common.api.storage;

import java.time.Instant;
import java.util.Objects;

/**
 * Inclusive-start, exclusive-end time window for bounded time lookups.
 *
 * @param startInclusive start boundary
 * @param endExclusive   end boundary
 */
public record TimeRange(Instant startInclusive, Instant endExclusive) {

    /**
     * Creates a time range.
     *
     * @param startInclusive start boundary
     * @param endExclusive   end boundary
     */
    public TimeRange {
        Objects.requireNonNull(startInclusive, "startInclusive");
        Objects.requireNonNull(endExclusive, "endExclusive");
        if (!startInclusive.isBefore(endExclusive)) {
            throw new IllegalArgumentException("startInclusive must be before endExclusive");
        }
    }

    /**
     * Creates a time range.
     *
     * @param startInclusive start boundary
     * @param endExclusive   end boundary
     * @return time range
     */
    public static TimeRange between(final Instant startInclusive, final Instant endExclusive) {
        return new TimeRange(startInclusive, endExclusive);
    }

    /**
     * Checks if an instant falls into this range.
     *
     * @param instant instant to check
     * @return true if instant is in range
     */
    public boolean contains(final Instant instant) {
        Objects.requireNonNull(instant, "instant");
        return !instant.isBefore(startInclusive) && instant.isBefore(endExclusive);
    }

    /**
     * Calculates overlapping duration in seconds.
     *
     * @param start start timestamp in milliseconds
     * @param end   end timestamp in milliseconds
     * @return overlap in seconds
     */
    public long overlapSeconds(final long start, final long end) {
        final long overlapStart = Math.max(start, startInclusive.toEpochMilli());
        final long overlapEnd = Math.min(end, endExclusive.toEpochMilli());
        return Math.max(0L, (overlapEnd - overlapStart + 500L) / 1000L);
    }
}
