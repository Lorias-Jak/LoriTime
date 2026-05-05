package com.jannik_kuehn.common.storage.database;

import java.util.Locale;

/**
 * Built-in SQL dialects supported by the storage layer.
 */
public enum DatabaseDialect implements SqlDialect {
    /**
     * An enumeration constant representing the MySQL SQL dialect implementation.
     * Provides methods for generating MySQL-specific SQL statements, including table creation
     * and query expressions for database operations.
     */
    MYSQL {
        @Override
        public String durationSecondsExpression(final String join, final String leave) {
            return "TIMESTAMPDIFF(SECOND, " + join + ", " + leave + ")";
        }
    },

    /**
     * An enumeration constant representing the MariaDB SQL dialect implementation.
     * Similar to the MySQL dialect, the MariaDB dialect is tailored for generating
     * MariaDB-specific SQL statements. It includes functionalities such as table creation
     * and custom query expressions necessary for interacting with MariaDB databases.
     */
    MARIADB {
        @Override
        public String durationSecondsExpression(final String join, final String leave) {
            return "TIMESTAMPDIFF(SECOND, " + join + ", " + leave + ")";
        }
    },

    /**
     * Represents the SQLite dialect for interacting with SQLite databases.
     * Provides SQL statement generation for creating and managing tables and
     * for performing specific database operations.
     */
    SQLITE {
        @Override
        public String durationSecondsExpression(final String joinColumn, final String leaveColumn) {
            return "CAST((" + leaveColumn + " - " + joinColumn + ") / 1000 AS INTEGER)";
        }
    };

    /**
     * Retrieves an {@code Dialect} instance by its name.
     *
     * @param dialect the name of the database dialect in string format (e.g., "mysql", "mariadb", "sqlite").
     *                The comparison is case-insensitive.
     * @return the corresponding {@code dialect} instance for the provided name.
     * @throws IllegalArgumentException if the provided dialect name does not match any supported database dialect.
     */
    public static DatabaseDialect getEngineByName(final String dialect) {
        return switch (dialect.toLowerCase(Locale.ROOT)) {
            case "mysql" -> MYSQL;
            case "mariadb" -> MARIADB;
            case "sqlite" -> SQLITE;
            default -> throw new IllegalArgumentException("Unknown database dialect: " + dialect);
        };
    }
}
