package com.jannik_kuehn.common.storage.database.table;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

/**
 * Reads timestamp values across JDBC drivers and SQLite text/numeric storage.
 */
final class DatabaseInstantReader {

    private DatabaseInstantReader() {
    }

    /* default */ static Instant readInstant(final ResultSet result, final String column) throws SQLException {
        final Object value = result.getObject(column);
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant();
        }
        if (value instanceof Number number) {
            return Instant.ofEpochMilli(number.longValue());
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.toInstant(ZoneOffset.UTC);
        }
        if (value instanceof String text) {
            return readInstant(text);
        }
        throw new SQLException("Unsupported timestamp value for column " + column + ": " + value);
    }

    private static Instant readInstant(final String text) throws SQLException {
        try {
            return Instant.ofEpochMilli(Long.parseLong(text));
        } catch (NumberFormatException ignored) {
            // Continue with timestamp formats below.
        }
        try {
            return Timestamp.valueOf(text).toInstant();
        } catch (IllegalArgumentException ignored) {
            // Continue with ISO-8601 parsing below.
        }
        try {
            return Instant.parse(text);
        } catch (DateTimeParseException exception) {
            throw new SQLException("Unsupported timestamp value: " + text, exception);
        }
    }
}
