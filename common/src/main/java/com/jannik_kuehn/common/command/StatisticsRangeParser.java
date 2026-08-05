package com.jannik_kuehn.common.command;

import com.jannik_kuehn.common.api.storage.TimeRange;
import com.jannik_kuehn.common.command.core.CommandScopes;
import com.jannik_kuehn.common.utils.TimeParser;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Statistics-specific rolling and calendar range parsing. */
@SuppressWarnings({"PMD.CommentRequired", "PMD.CommentDefaultAccessModifier", "PMD.CognitiveComplexity",
        "PMD.CyclomaticComplexity", "PMD.AvoidLiteralsInIfCondition"})
final class StatisticsRangeParser {
    private static final String CALENDAR_PREFIX = "calendar:";

    private static final Pattern COUNTED_CALENDAR = Pattern.compile("([1-9][0-9]*)(d|w|mo)");

    private static final Pattern CALENDAR_SLICE = Pattern.compile("([1-9][0-9]*)(d|w|mo)-([1-9][0-9]*)(d|w|mo)");

    private StatisticsRangeParser() {
    }

    static Selection parse(final TimeParser parser, final Clock clock, final ZoneId zone,
                           final String defaultRangeInput, final String... arguments) {
        final Instant observedAt = clock.instant();
        final List<String> lookupArguments = new ArrayList<>();
        String calendarInput = null;
        for (final String argument : arguments) {
            final String lower = argument.toLowerCase(Locale.ROOT);
            if (lower.startsWith(CALENDAR_PREFIX)) {
                if (calendarInput != null || argument.length() == CALENDAR_PREFIX.length()) {
                    return null;
                }
                calendarInput = argument.substring(CALENDAR_PREFIX.length());
            } else {
                lookupArguments.add(argument);
            }
        }
        final CommandScopes.LookupRequest lookup = CommandScopes.parseLookup(parser,
                Clock.fixed(observedAt, ZoneOffset.UTC), lookupArguments.toArray(String[]::new));
        if (lookup == null || lookup.playerName() != null || calendarInput != null && lookup.hasTimeRange()) {
            return null;
        }
        if (calendarInput != null) {
            final TimeRange calendarRange = calendarRange(calendarInput, observedAt, zone);
            return calendarRange == null ? null
                    : new Selection(lookup, calendarRange, "calendar:" + calendarInput, observedAt);
        }
        final TimeRange range = lookup.hasTimeRange() ? lookup.timeRange()
                : configuredDefaultRange(parser, defaultRangeInput, observedAt, zone);
        if (range == null) {
            return null;
        }
        final String label = lookup.timeRangeInput() == null ? defaultRangeInput : lookup.timeRangeInput();
        return new Selection(lookup, range, label, observedAt);
    }

    static ZoneId resolveZone(final String configured, final ZoneId systemZone, final Consumer<String> warning) {
        if (configured == null || configured.isBlank() || "system".equalsIgnoreCase(configured)) {
            return systemZone;
        }
        try {
            return ZoneId.of(configured);
        } catch (final DateTimeException exception) {
            warning.accept("Invalid stats.calendar-time-zone '" + configured
                    + "', falling back to system timezone " + systemZone);
            return systemZone;
        }
    }

    private static TimeRange calendarRange(final String input, final Instant observedAt, final ZoneId zone) {
        try {
            final String normalized = input.toLowerCase(Locale.ROOT);
            final ZonedDateTime observed = observedAt.atZone(zone);
            if ("today".equals(normalized)) {
                return TimeRange.between(unitStart(observed, 1L, "d").toInstant(), observedAt);
            } else if ("this-week".equals(normalized)) {
                return TimeRange.between(unitStart(observed, 1L, "w").toInstant(), observedAt);
            } else if ("this-month".equals(normalized)) {
                return TimeRange.between(unitStart(observed, 1L, "mo").toInstant(), observedAt);
            }
            final Matcher counted = COUNTED_CALENDAR.matcher(normalized);
            if (counted.matches()) {
                final long ordinal = Long.parseLong(counted.group(1));
                return TimeRange.between(unitStart(observed, ordinal, counted.group(2)).toInstant(), observedAt);
            }
            final Matcher slice = CALENDAR_SLICE.matcher(normalized);
            if (!slice.matches() || !slice.group(2).equals(slice.group(4))) {
                return null;
            }
            final long near = Long.parseLong(slice.group(1));
            final long far = Long.parseLong(slice.group(3));
            if (near > far) {
                return null;
            }
            final String unit = slice.group(2);
            final Instant start = unitStart(observed, far, unit).toInstant();
            final Instant end = near == 1L ? observedAt : unitStart(observed, near - 1L, unit).toInstant();
            return TimeRange.between(start, end);
        } catch (final DateTimeException | ArithmeticException | IllegalArgumentException exception) {
            return null;
        }
    }

    private static TimeRange configuredDefaultRange(final TimeParser parser, final String input,
                                                    final Instant observedAt, final ZoneId zone) {
        final String normalized = input.toLowerCase(Locale.ROOT);
        if (normalized.startsWith(CALENDAR_PREFIX)) {
            return calendarRange(input.substring(CALENDAR_PREFIX.length()), observedAt, zone);
        }
        final String duration = normalized.startsWith(CommandScopes.TIME_PREFIX)
                ? input.substring(CommandScopes.TIME_PREFIX.length()) : input;
        return parser.parseToSeconds(duration).stream()
                .filter(seconds -> seconds > 0L)
                .mapToObj(seconds -> TimeRange.between(observedAt.minusSeconds(seconds), observedAt))
                .findFirst().orElse(null);
    }

    private static ZonedDateTime unitStart(final ZonedDateTime observed, final long ordinal, final String unit) {
        return switch (unit) {
            case "d" -> observed.toLocalDate().minusDays(ordinal - 1L).atStartOfDay(observed.getZone());
            case "w" -> startOfWeek(observed).minusWeeks(ordinal - 1L);
            case "mo" -> startOfMonth(observed).minusMonths(ordinal - 1L);
            default -> throw new IllegalStateException("Unsupported calendar unit");
        };
    }

    private static ZonedDateTime startOfWeek(final ZonedDateTime observed) {
        return observed.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .atStartOfDay(observed.getZone());
    }

    private static ZonedDateTime startOfMonth(final ZonedDateTime observed) {
        return observed.toLocalDate().withDayOfMonth(1).atStartOfDay(observed.getZone());
    }

    /** Parsed statistics range and shared scope lookup. */
    record Selection(CommandScopes.LookupRequest lookup, TimeRange range, String label, Instant observedAt) {
    }
}
