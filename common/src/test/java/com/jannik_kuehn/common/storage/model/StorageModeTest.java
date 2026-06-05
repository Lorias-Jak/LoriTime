package com.jannik_kuehn.common.storage.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"PMD.UnitTestContainsTooManyAsserts"})
class StorageModeTest {

    @Test
    void parsesConfiguredModesCaseInsensitively() {
        assertEquals(StorageMode.STANDALONE, StorageMode.parse("standalone"), "Standalone should be parsed correctly");
        assertEquals(StorageMode.MASTER, StorageMode.parse(" MASTER "), "Master should be parsed correctly");
        assertEquals(StorageMode.SLAVE, StorageMode.parse("slave"), "Slave should be parsed correctly");
    }

    @Test
    void defaultsBlankModesToStandalone() {
        assertEquals(StorageMode.STANDALONE, StorageMode.parse(null), "Null should default to standalone");
        assertEquals(StorageMode.STANDALONE, StorageMode.parse(""), "Empty string should default to standalone");
        assertEquals(StorageMode.STANDALONE, StorageMode.parse("   "), "Whitespace string should default to standalone");
    }

    @Test
    void rejectsUnknownModes() {
        assertThrows(IllegalArgumentException.class, () -> StorageMode.parse("proxy-master"), "Unknown mode should throw exception");
    }
}
