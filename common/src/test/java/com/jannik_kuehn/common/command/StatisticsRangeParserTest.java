package com.jannik_kuehn.common.command;

import com.jannik_kuehn.common.api.storage.TimeRange;
import com.jannik_kuehn.common.utils.TimeParser;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"PMD.UnitTestAssertionsShouldIncludeMessage", "PMD.UnitTestContainsTooManyAsserts",
        "PMD.UseExplicitTypes"})
class StatisticsRangeParserTest {
    private static final Instant OBSERVED = Instant.parse("2024-04-17T15:30:00Z");

    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

    private static TimeParser parser() {
        return new TimeParser.Builder()
                .addUnit(60L, "m")
                .addUnit(86_400L, "d")
                .addUnit(604_800L, "w")
                .addUnit(2_592_000L, "mo")
                .build();
    }

    private static StatisticsRangeParser.Selection parse(final String... arguments) {
        return StatisticsRangeParser.parse(parser(), Clock.fixed(OBSERVED, ZoneOffset.UTC), BERLIN,
                "calendar:today", arguments);
    }

    @Test
    void rollingRangesRemainFixedDurations() {
        final var day = parse("time:1d");
        final var week = parse("time:7d");

        assertEquals(Duration.ofHours(24), Duration.between(day.range().startInclusive(), day.range().endExclusive()));
        assertEquals(Duration.ofHours(168), Duration.between(week.range().startInclusive(), week.range().endExclusive()));
        assertEquals(OBSERVED, day.observedAt());
    }

    @Test
    void calendarDayWeekAndMonthAlignInConfiguredZone() {
        assertEquals(Instant.parse("2024-04-16T22:00:00Z"), parse("calendar:today").range().startInclusive());
        assertEquals(Instant.parse("2024-04-14T22:00:00Z"),
                parse("calendar:this-week").range().startInclusive());
        assertEquals(Instant.parse("2024-03-31T22:00:00Z"),
                parse("calendar:this-month").range().startInclusive());
        assertEquals(OBSERVED, parse("calendar:today").range().endExclusive());
    }

    @Test
    void countedCalendarRangesIncludeCurrentAndPreviousUnits() {
        assertEquals(Instant.parse("2024-04-15T22:00:00Z"), parse("calendar:2d").range().startInclusive());
        assertEquals(Instant.parse("2024-04-07T22:00:00Z"), parse("calendar:2w").range().startInclusive());
        assertEquals(Instant.parse("2024-02-29T23:00:00Z"), parse("calendar:2mo").range().startInclusive());
    }

    @Test
    void oneUnitAliasesMatchKeywords() {
        assertEquals(parse("calendar:today").range(), parse("calendar:1d").range());
        assertEquals(parse("calendar:this-week").range(), parse("calendar:1w").range());
        assertEquals(parse("calendar:this-month").range(), parse("calendar:1mo").range());
    }

    @Test
    void calendarRangeUsesLocalBoundariesAcrossDaylightSaving() {
        final Instant observed = Instant.parse("2026-03-30T12:00:00Z");
        final var selection = StatisticsRangeParser.parse(parser(), Clock.fixed(observed, ZoneOffset.UTC), BERLIN,
                "calendar:today", "calendar:2d");

        assertEquals(Instant.parse("2026-03-28T23:00:00Z"), selection.range().startInclusive());
        assertEquals(Duration.ofHours(37),
                Duration.between(selection.range().startInclusive(), selection.range().endExclusive()));
    }

    @Test
    void rejectsConflictingDuplicateAndMalformedSelectors() {
        assertNull(parse("time:1d", "calendar:today"));
        assertNull(parse("calendar:today", "calendar:this-week"));
        assertNull(parse("time:1d", "time:7d"));
        assertNull(parse("calendar:0d"));
        assertNull(parse("calendar:-1w"));
        assertNull(parse("calendar:1m"));
        assertNull(parse("calendar:nope"));
        assertNull(parse("calendar:3d-2d"));
        assertNull(parse("calendar:2d-3w"));
        assertNull(parse("calendar:2d-3d-4d"));
    }

    @Test
    void completedCalendarSlicesIncludeBothOrdinalEndpoints() {
        assertEquals(TimeRange.between(Instant.parse("2024-04-14T22:00:00Z"),
                        Instant.parse("2024-04-16T22:00:00Z")),
                parse("calendar:2d-3d").range());
        assertEquals(TimeRange.between(Instant.parse("2024-03-31T22:00:00Z"),
                        Instant.parse("2024-04-14T22:00:00Z")),
                parse("calendar:2w-3w").range());
        assertEquals(TimeRange.between(Instant.parse("2024-01-31T23:00:00Z"),
                        Instant.parse("2024-03-31T22:00:00Z")),
                parse("calendar:2mo-3mo").range());
    }

    @Test
    void completedDaySlicePreservesDstAwareMidnights() {
        final Instant observed = Instant.parse("2026-03-30T12:00:00Z");
        final StatisticsRangeParser.Selection selection = StatisticsRangeParser.parse(parser(),
                Clock.fixed(observed, ZoneOffset.UTC), BERLIN, "calendar:today", "calendar:2d-3d");

        assertEquals(TimeRange.between(Instant.parse("2026-03-27T23:00:00Z"),
                Instant.parse("2026-03-29T22:00:00Z")), selection.range());
        assertEquals(Duration.ofHours(47), Duration.between(selection.range().startInclusive(),
                selection.range().endExclusive()));
    }

    @Test
    void configuredDefaultSupportsCalendarAndRollingSelectors() {
        final StatisticsRangeParser.Selection calendar = StatisticsRangeParser.parse(parser(),
                Clock.fixed(OBSERVED, ZoneOffset.UTC), BERLIN, "calendar:today");
        final StatisticsRangeParser.Selection rolling = StatisticsRangeParser.parse(parser(),
                Clock.fixed(OBSERVED, ZoneOffset.UTC), BERLIN, "time:7d");

        assertEquals(parse("calendar:today").range(), calendar.range());
        assertEquals(Duration.ofDays(7), Duration.between(rolling.range().startInclusive(),
                rolling.range().endExclusive()));
    }

    @Test
    void calendarSelectorCombinesWithScopeFlags() {
        final var selection = parse("world:world", "calendar:this-week", "server:survival");

        assertEquals("survival", selection.lookup().serverName());
        assertEquals("world", selection.lookup().worldName());
        assertEquals("calendar:this-week", selection.label());
    }

    @Test
    void resolvesSystemAndIanaZonesAndWarnsOnInvalidValue() {
        final List<String> warnings = new ArrayList<>();

        assertEquals(ZoneOffset.UTC, StatisticsRangeParser.resolveZone("system", ZoneOffset.UTC, warnings::add));
        assertEquals(BERLIN, StatisticsRangeParser.resolveZone("Europe/Berlin", ZoneOffset.UTC, warnings::add));
        assertEquals(ZoneOffset.UTC, StatisticsRangeParser.resolveZone("Not/AZone", ZoneOffset.UTC, warnings::add));
        assertEquals(1, warnings.size());
    }

    @Test
    void historicalRollingRangeKeepsCurrentObservationInstant() {
        final var selection = parse("time:1d-7d");

        assertEquals(TimeRange.between(OBSERVED.minus(Duration.ofDays(7)), OBSERVED.minus(Duration.ofDays(1))),
                selection.range());
        assertEquals(OBSERVED, selection.observedAt());
    }
}
