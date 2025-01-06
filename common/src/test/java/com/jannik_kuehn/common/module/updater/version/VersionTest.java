package com.jannik_kuehn.common.module.updater.version;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the {@link Version} class.
 */
class VersionTest {

    @Test
    @SuppressWarnings("PMD.JUnitTestContainsTooManyAsserts")
    void testVersionWithoutQualifierAndBuildNumber() {
        final Version version = new Version("1.2.5");
        assertEquals("1.2.5", version.getVersionString(), "The version string does not match the expected version.");
        assertEquals(1, version.getMajor(), "The major version does not match the expected major version.");
        assertEquals(2, version.getMinor(), "The minor version does not match the expected minor version.");
        assertEquals(5, version.getPatch(), "The patch version does not match the expected patch version.");
    }

    @Test
    void testVersionWithQualifier() {
        final Version version = new Version("1.2.5-DEV");
        assertEquals("DEV", version.getQualifier(), "The qualifier does not match the expected qualifier.");
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestContainsTooManyAsserts")
    void testVersionWithBuildNumber() {
        final Version version = new Version("1.2.5-DEV-12");
        assertEquals(12, version.getBuildNumber(), "The build number does not match the expected build number.");
        assertTrue(version.hasBuildNumber(), "The version should have a build number.");
        assertTrue(version.hasQualifier(), "The version should have a qualifier.");
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestContainsTooManyAsserts")
    void testVersionEqualsVersion() {
        final Version versionOne = new Version("1.2.5-DEV-12");
        final Version versionTwo = new Version("1.2.5-DEV-13");
        final Version versionThree = new Version("1.2.5-DEV-12");
        assertEquals(versionOne, versionOne, "The expected versions should be equal");
        assertNotEquals(versionOne, versionTwo, "The expected versions should not be equal");
        assertEquals(versionOne, versionThree, "The expected versions should be equal");
        assertNotEquals(versionOne, null, "The expected versions should not be equal");
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestContainsTooManyAsserts")
    void testVersionTest() {
        final Version version = new Version("2.0.1-0");
        assertTrue(version.hasBuildNumber(), "The version should have a build number.");
        assertEquals("", version.getQualifier(), "The qualifier does not match the expected qualifier.");
        assertTrue(version.hasBuildNumber(), "The version should have a build number.");
        assertEquals(0, version.getBuildNumber(), "The build number does not match the expected build number.");
    }
}
