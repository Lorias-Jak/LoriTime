package com.jannik_kuehn.common.module.updater;

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
    PATCH(),
}
