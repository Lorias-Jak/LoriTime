package com.jannik_kuehn.common.module.updater.version;

/**
 * Utility class for comparing versions.
 */
public final class VersionUtil {
    /**
     * The DEV qualifier.
     */
    private static final String DEV = "DEV";

    /**
     * The DEV-UNOFFICIAL qualifier.
     */
    private static final String DEV_UNOFFICIAL = "DEV-UNOFFICIAL";

    /**
     * Private constructor to prevent instantiation.
     */
    private VersionUtil() {
    }

    /**
     * Compares two versions.
     *
     * @param oldVersion the old version
     * @param newVersion the new version
     * @return {@code true} if the new version is newer than the old version, {@code false} otherwise
     */
    @Deprecated
    public static boolean isNewerVersion(final String oldVersion, final String newVersion) {
        return isNewerVersion(new Version(oldVersion), new Version(newVersion));
    }

    /**
     * Compares two versions.
     *
     * @param oldVersion the old version
     * @param newVersion the new version
     * @return {@code true} if the new version is newer than the old version, {@code false} otherwise
     */
    public static boolean isNewerVersion(final Version oldVersion, final Version newVersion) {
        if (compareMajorMinorPatch(oldVersion, newVersion)) {
            return true;
        }

        final int qualifierComparison = compareQualifiers(oldVersion.getQualifier(), newVersion.getQualifier());
        if (qualifierComparison != 0) {
            return qualifierComparison > 0;
        }

        return compareBuildNumbers(oldVersion.getBuildNumber(), newVersion.getBuildNumber());
    }

    private static boolean compareMajorMinorPatch(final Version oldVersion, final Version newVersion) {
        if (newVersion.getMajor() != oldVersion.getMajor()) {
            return newVersion.getMajor() > oldVersion.getMajor();
        }
        if (newVersion.getMinor() != oldVersion.getMinor()) {
            return newVersion.getMinor() > oldVersion.getMinor();
        }
        return newVersion.getPatch() > oldVersion.getPatch();
    }

    private static int compareQualifiers(final String oldQualifier, final String newQualifier) {
        if (oldQualifier == null && newQualifier != null) {
            return -1; // Release version has higher precedence
        }
        if (oldQualifier != null && newQualifier == null) {
            return 1;
        }
        if (oldQualifier == null) {
            return 0; // Both are null, equal
        }
        return Integer.compare(getQualifierRank(newQualifier), getQualifierRank(oldQualifier));
    }

    private static boolean compareBuildNumbers(final Integer oldBuild, final Integer newBuild) {
        return (oldBuild == null && newBuild != null)
                || (oldBuild != null && newBuild != null && newBuild > oldBuild);
    }

    /**
     * Returns the rank of a qualifier for comparison purposes.
     *
     * @param qualifier the qualifier string
     * @return the rank of the qualifier (higher rank means higher precedence)
     */
    private static int getQualifierRank(final String qualifier) {
        return switch (qualifier) {
            case DEV_UNOFFICIAL -> 1;
            case DEV -> 2;
            default -> 0;
        };
    }
}
