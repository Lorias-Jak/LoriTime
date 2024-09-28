package com.jannik_kuehn.common.utils;

import com.jannik_kuehn.common.config.localization.Localization;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility class for formatting time.
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class TimeUtil {

    /**
     * Private constructor to prevent instantiation.
     */
    private TimeUtil() {
        // Empty
    }

    /**
     * Formats the given time in seconds to a human-readable string.
     *
     * @param seconds      the time in seconds
     * @param localization the localization
     * @return the formatted time
     */
    public static String formatTime(final long seconds, @NotNull final Localization localization) {
        final Map<String, Long> timeUnits = calculateTimeUnits(seconds);
        final StringBuilder formattedTime = new StringBuilder();

        timeUnits.forEach((unit, value) -> {
            if (value > 0) {
                formattedTime.append(value).append(getLocalizedUnitString(value, localization, unit));
            }
        });

        return formattedTime.toString().replaceAll("\\s+", " ").trim();
    }

    private static String getLocalizedUnitString(final long value, final Localization localization, final String unitKey) {
        final String unitMessageKey = unitKey + (value == 1 ? ".singular" : ".plural");
        final String unitString = localization.getRawMessage(unitMessageKey);
        return (value == 0 ? "" : " ") + unitString + (value == 0 ? "" : " ");
    }

    private static Map<String, Long> calculateTimeUnits(final long seconds) {
        final LocalDateTime startDateTime = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC);
        final LocalDateTime endDateTime = LocalDateTime.ofEpochSecond(seconds, 0, ZoneOffset.UTC);

        final Map<String, Long> timeUnits = new LinkedHashMap<>();
        timeUnits.put("unit.year", ChronoUnit.YEARS.between(startDateTime, endDateTime));
        timeUnits.put("unit.month", ChronoUnit.MONTHS.between(startDateTime.plusYears(timeUnits.get("unit.year")), endDateTime));
        timeUnits.put("unit.week", ChronoUnit.WEEKS.between(startDateTime.plusYears(timeUnits.get("unit.year")).plusMonths(timeUnits.get("unit.month")), endDateTime));
        timeUnits.put("unit.day", ChronoUnit.DAYS.between(startDateTime.plusYears(timeUnits.get("unit.year")).plusMonths(timeUnits.get("unit.month")).plusWeeks(timeUnits.get("unit.week")), endDateTime));
        timeUnits.put("unit.hour", ChronoUnit.HOURS.between(startDateTime.plusYears(timeUnits.get("unit.year")).plusMonths(timeUnits.get("unit.month")).plusWeeks(timeUnits.get("unit.week")).plusDays(timeUnits.get("unit.day")), endDateTime));
        timeUnits.put("unit.minute", ChronoUnit.MINUTES.between(startDateTime.plusYears(timeUnits.get("unit.year")).plusMonths(timeUnits.get("unit.month")).plusWeeks(timeUnits.get("unit.week")).plusDays(timeUnits.get("unit.day")).plusHours(timeUnits.get("unit.hour")), endDateTime));
        timeUnits.put("unit.second", ChronoUnit.SECONDS.between(startDateTime.plusYears(timeUnits.get("unit.year")).plusMonths(timeUnits.get("unit.month")).plusWeeks(timeUnits.get("unit.week")).plusDays(timeUnits.get("unit.day")).plusHours(timeUnits.get("unit.hour")).plusMinutes(timeUnits.get("unit.minute")), endDateTime));

        return timeUnits;
    }

    /**
     * Returns the seconds of the given time in seconds.
     *
     * @param seconds the time in seconds
     * @return the seconds
     */
    public static String getSeconds(final long seconds) {
        return String.valueOf(calculateTimeUnits(seconds).get("unit.second"));
    }

    /**
     * Returns the minutes of the given time in seconds.
     *
     * @param seconds the time in seconds
     * @return the minutes
     */
    public static String getMinutes(final long seconds) {
        return String.valueOf(calculateTimeUnits(seconds).get("unit.minute"));
    }

    /**
     * Returns the hours of the given time in seconds.
     *
     * @param seconds the time in seconds
     * @return the hours
     */
    public static String getHours(final long seconds) {
        return String.valueOf(calculateTimeUnits(seconds).get("unit.hour"));
    }

    /**
     * Returns the days of the given time in seconds.
     *
     * @param seconds the time in seconds
     * @return the days
     */
    public static String getDays(final long seconds) {
        return String.valueOf(calculateTimeUnits(seconds).get("unit.day"));
    }

    /**
     * Returns the weeks of the given time in seconds.
     *
     * @param seconds the time in seconds
     * @return the weeks
     */
    public static String getWeeks(final long seconds) {
        return String.valueOf(calculateTimeUnits(seconds).get("unit.week"));
    }

    /**
     * Returns the months of the given time in seconds.
     *
     * @param seconds the time in seconds
     * @return the months
     */
    public static String getMonths(final long seconds) {
        return String.valueOf(calculateTimeUnits(seconds).get("unit.month"));
    }

    /**
     * Returns the years of the given time in seconds.
     *
     * @param seconds the time in seconds
     * @return the years
     */
    public static String getYears(final long seconds) {
        return String.valueOf(calculateTimeUnits(seconds).get("unit.year"));
    }

    /**
     * Returns the total seconds of the given time in seconds.
     *
     * @param seconds the time in seconds
     * @return the total seconds
     */
    public static String getTotalSeconds(final long seconds) {
        return formatDecimal(seconds, 0);
    }

    /**
     * Returns the total minutes of the given time in seconds.
     *
     * @param seconds the time in seconds
     * @return the total minutes
     */
    public static String getTotalMinutes(final long seconds) {
        final double totalMinutes = seconds / 60.0;
        return formatDecimal(totalMinutes, 2);
    }

    /**
     * Returns the total hours of the given time in seconds.
     *
     * @param seconds the time in seconds
     * @return the total hours
     */
    public static String getTotalHours(final long seconds) {
        final double totalHours = seconds / 3_600.0;
        return formatDecimal(totalHours, 2);
    }

    /**
     * Returns the total days of the given time in seconds.
     *
     * @param seconds the time in seconds
     * @return the total days
     */
    public static String getTotalDays(final long seconds) {
        final double totalDays = seconds / 86_400.0;
        return formatDecimal(totalDays, 2);
    }

    /**
     * Returns the total weeks of the given time in seconds.
     *
     * @param seconds the time in seconds
     * @return the total weeks
     */
    public static String getTotalWeeks(final long seconds) {
        final double totalWeeks = seconds / (86_400.0 * 7);
        return formatDecimal(totalWeeks, 2);
    }

    /**
     * Returns the total months of the given time in seconds.
     *
     * @param seconds the time in seconds
     * @return the total months
     */
    public static String getTotalMonths(final long seconds) {
        final double totalMonths = seconds / (86_400.0 * 30.4375);
        return formatDecimal(totalMonths, 2);
    }

    /**
     * Returns the total years of the given time in seconds.
     *
     * @param seconds the time in seconds
     * @return the total years
     */
    public static String getTotalYears(final long seconds) {
        final double totalYears = seconds / (86_400.0 * 365.25);
        return formatDecimal(totalYears, 2);
    }

    private static String formatDecimal(final double value, final int decimalPlaces) {
        BigDecimal bigDecimal = new BigDecimal(Double.toString(value));
        bigDecimal = bigDecimal.setScale(decimalPlaces, RoundingMode.HALF_UP);
        return bigDecimal.toPlainString();
    }
}
