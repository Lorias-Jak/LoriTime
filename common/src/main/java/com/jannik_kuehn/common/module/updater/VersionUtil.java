package com.jannik_kuehn.common.module.updater;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for comparing versions.
 */
public final class VersionUtil {

    /**
     * Private constructor to prevent instantiation.
     */
    private VersionUtil() {
        // Empty
    }

    /**
     * Compares two versions.
     *
     * @param oldVersion the old version
     * @param newVersion the new version
     * @return {@code true} if the new version is newer than the old version, {@code false} otherwise
     */
    @SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.SimplifyBooleanReturns"})
    public static boolean isNewerVersion(final String oldVersion, final String newVersion) {
        final String oldVersionNumber = extractVersionNumber(oldVersion);
        final String newVersionNumber = extractVersionNumber(newVersion);

        if (oldVersionNumber == null || newVersionNumber == null) {
            throw new IllegalArgumentException("Versions must be in the format x.x.x");
        }

        final String[] oldVersionParts = oldVersionNumber.split("\\.");
        final String[] newVersionParts = newVersionNumber.split("\\.");

        if (oldVersionParts.length != 3 || newVersionParts.length != 3) {
            throw new IllegalArgumentException("Versions must be in the format x.x.x");
        }

        final int oldMajor = Integer.parseInt(oldVersionParts[0]);
        final int oldMinor = Integer.parseInt(oldVersionParts[1]);
        final int oldPatch = Integer.parseInt(oldVersionParts[2]);

        final int newMajor = Integer.parseInt(newVersionParts[0]);
        final int newMinor = Integer.parseInt(newVersionParts[1]);
        final int newPatch = Integer.parseInt(newVersionParts[2]);

        if (newMajor > oldMajor) {
            return true;
        } else if (newMajor < oldMajor) {
            return false;
        } else {
            if (newMinor > oldMinor) {
                return true;
            } else if (newMinor < oldMinor) {
                return false;
            } else {
                return newPatch > oldPatch;
            }
        }
    }

    private static String extractVersionNumber(final String version) {
        final Pattern pattern = Pattern.compile("(\\d+\\.\\d+\\.\\d+)");
        final Matcher matcher = pattern.matcher(version);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
