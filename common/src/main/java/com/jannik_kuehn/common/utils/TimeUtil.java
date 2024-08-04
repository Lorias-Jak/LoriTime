package com.jannik_kuehn.common.utils;

import com.jannik_kuehn.common.config.localization.Localization;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.util.Objects;

public final class TimeUtil {

    private TimeUtil() {
        // Empty
    }

    public static String formatTime(final long seconds, final Localization localization) {
        Objects.requireNonNull(localization);

        final Duration duration = Duration.standardSeconds(seconds);
        final Period period = duration.toPeriodFrom(new org.joda.time.Instant(0)).normalizedStandard();

        final PeriodFormatter formatter = new PeriodFormatterBuilder()
                .appendYears()
                .appendSuffix(getLocalizedUnitString(period.getYears(), localization, "unit.year"))
                .appendMonths()
                .appendSuffix(getLocalizedUnitString(period.getMonths(), localization, "unit.month"))
                .appendWeeks()
                .appendSuffix(getLocalizedUnitString(period.getWeeks(), localization, "unit.week"))
                .appendDays()
                .appendSuffix(getLocalizedUnitString(period.getDays(), localization, "unit.day"))
                .appendHours()
                .appendSuffix(getLocalizedUnitString(period.getHours(), localization, "unit.hour"))
                .appendMinutes()
                .appendSuffix(getLocalizedUnitString(period.getMinutes(), localization, "unit.minute"))
                .appendSeconds()
                .appendSuffix(getLocalizedUnitString(period.getSeconds(), localization, "unit.second"))
                .toFormatter();

        return formatter.print(period).replaceAll("\\s+", " ").trim();
    }

    private static String getLocalizedUnitString(final long value, final Localization localization, final String unitKey) {
        final String unitMessageKey = unitKey + (value == 1 ? ".singular" : ".plural");
        final String unitString = localization.getRawMessage(unitMessageKey);
        return (value == 0 ? "" : " ") + unitString + (value == 0 ? "" : " ");
    }

    public static String getSeconds(final long seconds) {
        final Duration duration = Duration.standardSeconds(seconds);
        final Period period = duration.toPeriodFrom(new org.joda.time.Instant(0)).normalizedStandard();
        return String.valueOf(period.getSeconds());
    }

    public static String getMinutes(final long seconds) {
        final Duration duration = Duration.standardSeconds(seconds);
        final Period period = duration.toPeriodFrom(new org.joda.time.Instant(0)).normalizedStandard();
        return String.valueOf(period.getMinutes());
    }

    public static String getHours(final long seconds) {
        final Duration duration = Duration.standardSeconds(seconds);
        final Period period = duration.toPeriodFrom(new org.joda.time.Instant(0)).normalizedStandard();
        return String.valueOf(period.getHours());
    }

    public static String getDays(final long seconds) {
        final Duration duration = Duration.standardSeconds(seconds);
        final Period period = duration.toPeriodFrom(new org.joda.time.Instant(0)).normalizedStandard();
        return String.valueOf(period.getDays());
    }

    public static String getWeeks(final long seconds) {
        final Duration duration = Duration.standardSeconds(seconds);
        final Period period = duration.toPeriodFrom(new org.joda.time.Instant(0)).normalizedStandard();
        return String.valueOf(period.getWeeks());
    }

    public static String getMonths(final long seconds) {
        final Duration duration = Duration.standardSeconds(seconds);
        final Period period = duration.toPeriodFrom(new org.joda.time.Instant(0)).normalizedStandard();
        return String.valueOf(period.getMonths());
    }

    public static String getYears(final long seconds) {
        final Duration duration = Duration.standardSeconds(seconds);
        final Period period = duration.toPeriodFrom(new org.joda.time.Instant(0)).normalizedStandard();
        return String.valueOf(period.getYears());
    }
}
