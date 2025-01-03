package com.jannik_kuehn.common.module.updater.version;

/**
 * Represents a version.
 */
public class Version {
    /**
     * The length of the qualifier.
     */
    private static final int QUALIFIER_LENGTH_ONE = 1;

    /**
     * The major version number.
     */
    private final int major;

    /**
     * The minor version number.
     */
    private final int minor;

    /**
     * The patch version number.
     */
    private final int patch;

    /**
     * The qualifier of the version.
     */
    private final String qualifier;

    /**
     * The build number of the version.
     */
    private final Integer buildNumber;

    /**
     * Creates a new version.
     *
     * @param versionString the version string in form of `major.minor.patch-qualifier-buildNumber`
     */
    public Version(final String versionString) {
        final ParsedVersion parsedVersion = parseVersionString(versionString);

        this.major = parsedVersion.major;
        this.minor = parsedVersion.minor;
        this.patch = parsedVersion.patch;
        this.qualifier = parsedVersion.qualifier;
        this.buildNumber = parsedVersion.buildNumber;
    }

    private static ParsedVersion parseVersionString(final String versionString) {
        final String[] mainAndQualifier = versionString.split("-", 2);
        final String[] versionParts = mainAndQualifier[0].split("\\.");

        final int parsedMajor;
        final int parsedMinor;
        final int parsedPatch;

        parsedMajor = Integer.parseInt(versionParts[0]);
        parsedMinor = Integer.parseInt(versionParts[1]);
        parsedPatch = Integer.parseInt(versionParts[2]);

        String tempQualifier = null;
        Integer tempBuildNumber = null;
        if (mainAndQualifier.length > QUALIFIER_LENGTH_ONE) {
            final String[] qualifierParts = mainAndQualifier[1].split("-");
            tempQualifier = qualifierParts[0];
            if (qualifierParts.length > QUALIFIER_LENGTH_ONE) {
                tempBuildNumber = Integer.parseInt(qualifierParts[1]);
            }
        }

        return new ParsedVersion(parsedMajor, parsedMinor, parsedPatch, tempQualifier, tempBuildNumber);
    }

    /**
     * Returns the major version number.
     *
     * @return the major version number
     */
    public int getMajor() {
        return major;
    }

    /**
     * Returns the minor version number.
     *
     * @return the minor version number
     */
    public int getMinor() {
        return minor;
    }

    /**
     * Returns the patch version number.
     *
     * @return the patch version number
     */
    public int getPatch() {
        return patch;
    }

    /**
     * Returns the qualifier of the version.
     *
     * @return the qualifier of the version
     */
    public String getQualifier() {
        return qualifier;
    }

    /**
     * Returns the build number of the version.
     *
     * @return the build number of the version
     */
    public Integer getBuildNumber() {
        return buildNumber;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(major).append('.').append(minor).append('.').append(patch);
        if (qualifier != null) {
            builder.append('-').append(qualifier);
        }
        if (buildNumber != null) {
            builder.append('-').append(buildNumber);
        }
        return builder.toString();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Version version = (Version) obj;
        return major == version.major
                && minor == version.minor
                && patch == version.patch
                && ((qualifier == null && version.qualifier == null)
                || (qualifier != null && qualifier.equals(version.qualifier)))
                && ((buildNumber == null && version.buildNumber == null)
                || (buildNumber != null && buildNumber.equals(version.buildNumber)));
    }

    @Override
    public int hashCode() {
        int result = major;
        result = 31 * result + minor;
        result = 31 * result + patch;
        result = 31 * result + (qualifier != null ? qualifier.hashCode() : 0);
        result = 31 * result + (buildNumber != null ? buildNumber.hashCode() : 0);
        return result;
    }

    private record ParsedVersion(int major, int minor, int patch, String qualifier, Integer buildNumber) {
    }
}
