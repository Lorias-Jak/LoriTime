package com.jannik_kuehn.common.module.updater.version;

import com.jannik_kuehn.common.exception.UpdateException;

/**
 * The strategy for updating a version.
 * For versions that fulfil semantic versioning.
 */
public enum Strategy {
    /**
     * The first number of a semantic version.
     */
    MAJOR(),

    /**
     * The second number of a semantic version.
     */
    MINOR(),

    /**
     * The third number of a semantic version.
     */
    PATCH();

    /**
     * Returns the strategy from a string.
     *
     * @param strategy The string representation of the strategy.
     * @return The strategy.
     */
    public static Strategy getFromString(final String strategy) {
        try {
            return Strategy.valueOf(strategy);
        } catch (final IllegalArgumentException e) {
            throw new UpdateException("The strategy " + strategy + " is not valid.", e);
        }
    }

    /**
     * Checks if a strategy exists.
     *
     * @param strategy The strategy to check.
     * @return {@code true} if the strategy exists, otherwise {@code false}.
     */
    public static boolean doesStrategyExists(final String strategy) {
        try {
            Strategy.valueOf(strategy);
            return true;
        } catch (final IllegalArgumentException e) {
            return false;
        }
    }
}
