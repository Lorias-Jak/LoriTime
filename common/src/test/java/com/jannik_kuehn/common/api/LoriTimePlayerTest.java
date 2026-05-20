package com.jannik_kuehn.common.api;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LoriTimePlayerTest {

    private static final UUID PLAYER_ID = UUID.fromString("44174cf6-e76c-4994-899c-3387284ecd62");

    @Test
    void equalityAndHashCodeUseSameIdentity() {
        final LoriTimePlayer first = new LoriTimePlayer(PLAYER_ID, "Lorias_");
        final LoriTimePlayer second = new LoriTimePlayer(PLAYER_ID, "Renamed");

        assertEquals(first, second, "Players with the same UUID should be equal");
        assertEquals(first.hashCode(), second.hashCode(), "Equal players should have the same hash code");
    }

    @Test
    void afkSetterUpdatesState() {
        final LoriTimePlayer player = new LoriTimePlayer(PLAYER_ID, "Lorias_");

        player.setAfk(true);
        assertTrue(player.isAfk(), "Correctly named setter should update AFK state");

        player.setAfk(false);
        assertFalse(player.isAfk(), "Setter should update AFK state");
    }
}
