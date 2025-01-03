package com.jannik_kuehn.common.module.updater;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a version.
 */
public class Version {
    /**
     * The pattern for parsing the build number and the qualifier.
     */
    public static final Pattern BUILD_PATTERN = Pattern.compile("^(?<qualifier>.*?)(?<buildnumber>0|[1-9]\\d*)?$");

    /**
     * The artifact version.
     */
    private final DefaultArtifactVersion artifactVersion;

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
        this.artifactVersion = new DefaultArtifactVersion(versionString);
        String tempQualifier = null;
        Integer tempBuildNumber = null;

        if (artifactVersion.getQualifier() != null) {
            final Matcher matcher = BUILD_PATTERN.matcher(artifactVersion.getQualifier());
            if (matcher.matches()) {
                tempQualifier = matcher.group(1);
                final String buildNumberString = matcher.group(2);
                if (buildNumberString != null) {
                    tempBuildNumber = Integer.valueOf(buildNumberString);
                }
            }
        } else if (artifactVersion.getBuildNumber() != 0 || versionString.endsWith("-0")) {
            tempBuildNumber = artifactVersion.getBuildNumber();
            tempQualifier = "";
        }
        this.qualifier = tempQualifier;
        this.buildNumber = tempBuildNumber;
    }

    /**
     * Returns the major version number.
     *
     * @return the major version number
     */
    public int getMajor() {
        return artifactVersion.getMajorVersion();
    }

    /**
     * Returns the minor version number.
     *
     * @return the minor version number
     */
    public int getMinor() {
        return artifactVersion.getMinorVersion();
    }

    /**
     * Returns the patch version number.
     *
     * @return the patch version number
     */
    public int getPatch() {
        return artifactVersion.getIncrementalVersion();
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

    /**
     * Returns the version string.
     *
     * @return the version string
     */
    public String getVersionString() {
        return artifactVersion.toString();
    }

    /**
     * Returns whether the version has a qualifier.
     *
     * @return {@code true} if the version has a qualifier.
     */
    public boolean hasQualifier() {
        return qualifier != null;
    }

    /**
     * Returns whether the version has a build number.
     *
     * @return {@code true} if the version has a build number.
     */
    public boolean hasBuildNumber() {
        return buildNumber != null;
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
        return artifactVersion.equals(version.artifactVersion) && Objects.equals(qualifier, version.qualifier)
                && Objects.equals(buildNumber, version.buildNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(artifactVersion, qualifier, buildNumber);
    }
}
