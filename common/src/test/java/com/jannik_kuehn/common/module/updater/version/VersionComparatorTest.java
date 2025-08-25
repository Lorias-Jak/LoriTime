package com.jannik_kuehn.common.module.updater.version;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the {@link VersionComparator} class.
 */
class VersionComparatorTest {

    public static final Version VERSION_ONE = new Version("1.2.4");

    public static final Version VERSION_TWO = new Version("2.2.5-DEV-1");

    public static final Version VERSION_TWO_WITHOUT_QUALIFIER = new Version("2.2.5");

    public static final Version VERSION_THREE = new Version("2.2.5-DEV-12");

    public static final Version VERSION_FOUR = new Version("2.3.6-DEV-UNOFFICIAL");

    public static final Version VERSION_FIVE = new Version("2.3.7-DEV-UNOFFICIAL");

    public static final Version VERSION_SIX = new Version("2.4.5-DEV-UNOFFICIAL");

    private static Stream<Arguments> combinations() {
        return Stream.of(
                Arguments.of(
                        new Input(VERSION_ONE, VERSION_TWO, Strategy.MAJOR),
                        new Expected(false, false, true)
                ),
                Arguments.of(
                        new Input(VERSION_TWO, VERSION_ONE, Strategy.MAJOR),
                        new Expected(false, false, true)
                ),
                Arguments.of(
                        new Input(VERSION_ONE, VERSION_TWO, Strategy.MINOR),
                        new Expected(false, false, true)
                ),
                Arguments.of(
                        new Input(VERSION_TWO, VERSION_ONE, Strategy.MINOR),
                        new Expected(false, false, true)
                ),
                Arguments.of(
                        new Input(VERSION_ONE, VERSION_TWO, Strategy.PATCH),
                        new Expected(false, false, true)
                ),
                Arguments.of(
                        new Input(VERSION_TWO, VERSION_ONE, Strategy.PATCH),
                        new Expected(false, false, true)
                ),
                Arguments.of(
                        new Input(null, null, Strategy.MAJOR),
                        new Expected(false, true, false)
                ),
                Arguments.of(
                        new Input(VERSION_ONE, null, Strategy.MAJOR),
                        new Expected(false, false, true)
                ),
                Arguments.of(
                        new Input(null, VERSION_ONE, Strategy.MINOR),
                        new Expected(true, false, false)
                ),
                Arguments.of(
                        new Input(VERSION_ONE, VERSION_ONE, Strategy.MINOR),
                        new Expected(false, true, false)
                ),
                Arguments.of(
                        new Input(VERSION_ONE, VERSION_TWO, Strategy.PATCH, "DEV-"),
                        new Expected(false, false, true)
                ),
                Arguments.of(
                        new Input(VERSION_TWO, VERSION_TWO_WITHOUT_QUALIFIER, Strategy.PATCH, "DEV-"),
                        new Expected(true, false, false)
                ),
                Arguments.of(
                        new Input(VERSION_TWO_WITHOUT_QUALIFIER, VERSION_TWO, Strategy.PATCH, "DEV-"),
                        new Expected(false, false, true)
                ),
                Arguments.of(
                        new Input(VERSION_TWO, VERSION_THREE, Strategy.PATCH, "DEV-"),
                        new Expected(true, false, false)
                ),
                Arguments.of(
                        new Input(VERSION_THREE, VERSION_TWO, Strategy.PATCH, "DEV-"),
                        new Expected(false, false, true)
                ),
                Arguments.of(
                        new Input(VERSION_SIX, VERSION_FOUR, Strategy.MINOR, "DEV-UNOFFICIAL"), //
                        new Expected(false, false, true)
                ),
                Arguments.of(
                        new Input(VERSION_FOUR, VERSION_FIVE, Strategy.PATCH, "DEV-UNOFFICIAL"), //
                        new Expected(true, false, false)
                ),
                Arguments.of(
                        new Input(VERSION_TWO, VERSION_FIVE, Strategy.PATCH, "TEST_QUALIFIER"), //
                        new Expected(false, true, false)
                )
        );
    }

    @ParameterizedTest
    @MethodSource("combinations")
    void testIsOtherNewerThanCurrent(final Input input, final Expected expected) {
        final VersionComparator versionComparator = new VersionComparator(input.strategy, input.qualifier);
        assertEquals(expected.otherNewerThanCurrent,
                versionComparator.isOtherNewerThanCurrent(input.current, input.other),
                "The expected result is: " + expected.otherNewerThanCurrent);
    }

    @ParameterizedTest
    @MethodSource("combinations")
    void testIsOtherEqualToCurrent(final Input input, final Expected expected) {
        final VersionComparator versionComparator = new VersionComparator(input.strategy, input.qualifier);
        assertEquals(expected.equal,
                versionComparator.isOtherEqualToCurrent(input.current, input.other),
                "The expected result is: " + expected.equal);
    }

    @ParameterizedTest
    @MethodSource("combinations")
    void testIsCurrentNewerThanOther(final Input input, final Expected expected) {
        final VersionComparator versionComparator = new VersionComparator(input.strategy, input.qualifier);
        assertEquals(expected.currentNewerThanOther,
                versionComparator.isCurrentNewerThanOther(input.current, input.other),
                "The expected result ist: " + expected.currentNewerThanOther);
    }

    private record Input(Version current, Version other, Strategy strategy, String... qualifier) {
    }

    private record Expected(boolean otherNewerThanCurrent, boolean equal, boolean currentNewerThanOther) {
    }

}
