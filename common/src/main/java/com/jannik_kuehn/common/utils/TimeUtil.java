package com.jannik_kuehn.common.utils;

import com.jannik_kuehn.common.config.localization.Localization;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.util.Objects;

public final class TimeUtil {

    private TimeUtil() {
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
                .appendSuffix(getLocalizedUnitString(duration.toStandardHours().getHours(), localization, "unit.hour"))
                .appendMinutes()
                .appendSuffix(getLocalizedUnitString(duration.toStandardMinutes().getMinutes(), localization, "unit.minute"))
                .appendSeconds()
                .appendSuffix(getLocalizedUnitString(duration.toStandardSeconds().getSeconds(), localization, "unit.second"))
                .toFormatter();

        return formatter.print(period).replaceAll("\\s+", " ").trim();
    }

    private static String getLocalizedUnitString(final long value, final Localization localization, final String unitKey) {
        final String unitMessageKey = unitKey + (value == 1 ? ".singular" : ".plural");
        final String unitString = localization.getRawMessage(unitMessageKey);
        return (value == 0 ? "" : " ") + unitString + (value == 0 ? "" : " ");
    }
}
