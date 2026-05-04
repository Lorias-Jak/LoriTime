package com.jannik_kuehn.common.api.storage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings({"PMD.UnitTestAssertionsShouldIncludeMessage", "PMD.UnitTestContainsTooManyAsserts"})
class StorageModeTest {

    @Test
    void parsesConfiguredModesCaseInsensitively() {
        assertEquals(StorageMode.STANDALONE, StorageMode.parse("standalone"));
        assertEquals(StorageMode.MASTER, StorageMode.parse(" MASTER "));
        assertEquals(StorageMode.SLAVE, StorageMode.parse("slave"));
    }

    @Test
    void defaultsBlankModesToStandalone() {
        assertEquals(StorageMode.STANDALONE, StorageMode.parse(null));
        assertEquals(StorageMode.STANDALONE, StorageMode.parse(""));
        assertEquals(StorageMode.STANDALONE, StorageMode.parse("   "));
    }

    @Test
    void rejectsUnknownModes() {
        assertThrows(IllegalArgumentException.class, () -> StorageMode.parse("proxy-master"));
    }
}
