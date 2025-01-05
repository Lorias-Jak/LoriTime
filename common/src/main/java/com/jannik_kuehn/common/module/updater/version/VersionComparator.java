package com.jannik_kuehn.common.module.updater.version;

import java.util.Arrays;
import java.util.List;

/**
 * Compares versions based on the chosen update strategy.
 */
public class VersionComparator {
    /**
     * The chosen update strategy.
     */
    private final Strategy updateStrategy;

    /**
     * List of qualifiers in prioritized order.
     */
    private final List<String> qualifiers;

    /**
     * Creates a new {@link VersionComparator}.
     *
     * @param updateStrategy The chosen update strategy.
     * @param qualifiers     The optional list of valid qualifiers in prioritized order (first entry has the highest priority).
     */
    public VersionComparator(final Strategy updateStrategy, final String... qualifiers) {
        this.updateStrategy = updateStrategy;
        this.qualifiers = Arrays.stream(qualifiers).toList();
    }

    /**
     * Checks if the other version is newer than the current one.
     *
     * @param currentVersion the current {@link Version}.
     * @param otherVersion   the other {@link Version}.
     * @return {@code true} if the other {@link Version} is newer than the current {@link Version}, {@code false} otherwise.
     */
    public boolean isOtherNewerThanCurrent(final Version currentVersion, final Version otherVersion) {
        return compareVersions(currentVersion, otherVersion) < 0;
    }

    /**
     * Checks if the other version is equal than the current one.
     *
     * @param currentVersion The current {@link Version}.
     * @param otherVersion   The other {@link Version}.
     * @return {@code true} if the other {@link Version} is equal to the current {@link Version}, {@code false} otherwise.
     */
    public boolean isOtherEqualToCurrent(final Version currentVersion, final Version otherVersion) {
        return compareVersions(currentVersion, otherVersion) == 0;
    }

    /**
     * Checks if the other version is newer or equal than the current one.
     *
     * @param currentVersion The current {@link Version}.
     * @param otherVersion   The other {@link Version}.
     * @return {@code true} if the current {@link Version} is newer than the other {@link Version}, {@code false} otherwise.
     */
    public boolean isCurrentNewerThanOther(final Version currentVersion, final Version otherVersion) {
        return compareVersions(currentVersion, otherVersion) > 0;
    }

    /**
     * Compares two versions.
     *
     * @param currentVersion the current {@link Version}.
     * @param otherVersion   the other {@link Version}.
     * @return 0 if equal, less than 0 if other is newer, more than 0 if current is newer.
     */
    @SuppressWarnings({"PMD.NPathComplexity", "PMD.CyclomaticComplexity"})
    public int compareVersions(final Version currentVersion, final Version otherVersion) {
        if (currentVersion == null && otherVersion == null) {
            return 0;
        }
        if (currentVersion == null) {
            return -1;
        }
        if (otherVersion == null) {
            return 1;
        }
        if (currentVersion.getVersionString().equals(otherVersion.getVersionString())) {
            return 0;
        }
        if (currentVersion.hasQualifier() && !qualifiers.contains(currentVersion.getQualifier())
                && otherVersion.hasQualifier() && !qualifiers.contains(otherVersion.getQualifier())) {
            return 0;
        }
        if (otherVersion.hasQualifier() && !qualifiers.contains(otherVersion.getQualifier())) {
            return 1;
        }

        return compare(currentVersion, otherVersion);
    }

    private int compare(final Version currentVersion, final Version otherVersion) {
        final int majorVersion = Integer.compare(currentVersion.getMajor(), otherVersion.getMajor());
        final int minorVersion = Integer.compare(currentVersion.getMinor(), otherVersion.getMinor());
        final int patchVersion = Integer.compare(currentVersion.getPatch(), otherVersion.getPatch());

        final int currentBuildNumber = currentVersion.hasBuildNumber() ? currentVersion.getBuildNumber() : Integer.MAX_VALUE;
        final int otherBuildNumber = otherVersion.hasBuildNumber() ? otherVersion.getBuildNumber() : Integer.MAX_VALUE;
        final int buildNumber = Integer.compare(currentBuildNumber, otherBuildNumber);
        final int currentQualifier = currentVersion.hasQualifier() ? qualifiers.contains(currentVersion.getQualifier())
                ? qualifiers.indexOf(currentVersion.getQualifier()) : Integer.MIN_VALUE : Integer.MAX_VALUE;
        final int otherQualifier = otherVersion.hasQualifier() ? qualifiers.contains(otherVersion.getQualifier())
                ? qualifiers.indexOf(otherVersion.getQualifier()) : Integer.MIN_VALUE : Integer.MAX_VALUE;
        final int qualifierNumber = Integer.compare(currentQualifier, otherQualifier);

        return compareByUpdateStrategy(majorVersion, minorVersion, patchVersion, buildNumber, qualifierNumber);
    }

    @SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.ImplicitSwitchFallThrough"})
    private int compareByUpdateStrategy(final int majorVersion, final int minorVersion, final int patchVersion,
                                        final int buildNumber, final int qualifierNumber) {
        switch (updateStrategy) {
            case MAJOR:
                if (majorVersion != 0) {
                    return majorVersion;
                }
            case MINOR:
                if (majorVersion == 0 && minorVersion != 0) {
                    return minorVersion;
                }
            case PATCH:
                if (majorVersion == 0 && minorVersion == 0) {
                    if (patchVersion != 0) {
                        return patchVersion;
                    }
                    return compareQualifier(qualifierNumber, buildNumber);
                }
            default:
                return 1;
        }
    }

    private int compareQualifier(final int qualifierNumber, final int buildNumber) {
        if (qualifierNumber != 0) {
            return qualifierNumber;
        }
        return buildNumber;
    }
}
