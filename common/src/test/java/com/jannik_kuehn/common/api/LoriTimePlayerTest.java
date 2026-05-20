package com.jannik_kuehn.common.api;

import com.jannik_kuehn.common.api.common.CommonSender;
import com.jannik_kuehn.common.player.TrackedLoriTimePlayer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LoriTimePlayerTest {

    private static final UUID PLAYER_ID = UUID.fromString("44174cf6-e76c-4994-899c-3387284ecd62");

    @Test
    void publicReferenceEqualityAndHashCodeUseAllRecordComponents() {
        final LoriTimePlayerRef first = new LoriTimePlayerRef(PLAYER_ID, "Lorias_");
        final LoriTimePlayerRef second = new LoriTimePlayerRef(PLAYER_ID, "Lorias_");

        assertEquals(first, second, "Identical public references should be equal");
        assertEquals(first.hashCode(), second.hashCode(), "Equal references should have the same hash code");
    }

    @Test
    void publicPlayerContractExposesIdentityMethods() {
        final Method[] methods = LoriTimePlayer.class.getDeclaredMethods();

        assertTrue(Arrays.stream(methods).anyMatch(method -> "getUniqueId".equals(method.getName())),
                "Public player contract should expose UUID identity");
        assertTrue(Arrays.stream(methods).anyMatch(method -> "getName".equals(method.getName())),
                "Public player contract should expose name identity");
    }

    @Test
    void publicPlayerContractDoesNotExposeRuntimeTrackingMethods() {
        final Method[] methods = LoriTimePlayer.class.getDeclaredMethods();

        assertFalse(Arrays.stream(methods).anyMatch(method -> method.getName().contains("Afk")),
                "Public player contract should not expose AFK state");
        assertFalse(Arrays.stream(methods).anyMatch(method -> method.getName().contains("Resume")),
                "Public player contract should not expose resume tracking state");
    }

    @Test
    void publicReferenceRejectsInvalidIdentity() {
        assertAll(
                () -> assertThrows(NullPointerException.class, () -> new LoriTimePlayerRef(null, "Lorias_")),
                () -> assertThrows(NullPointerException.class, () -> new LoriTimePlayerRef(PLAYER_ID, null)),
                () -> assertThrows(IllegalArgumentException.class, () -> new LoriTimePlayerRef(PLAYER_ID, " "))
        );
    }

    @Test
    void commonSenderSatisfiesPublicPlayerContract() {
        assertTrue(LoriTimePlayer.class.isAssignableFrom(CommonSender.class),
                "CommonSender should be usable wherever LoriTimePlayer is accepted");
    }

    @Test
    void trackedPlayerKeepsInternalAfkState() {
        final TrackedLoriTimePlayer player = new TrackedLoriTimePlayer(PLAYER_ID, "Lorias_");

        player.setAfk(true);
        assertTrue(player.isAfk(), "Internal tracked player should update AFK state");
        player.setAfk(false);
        assertFalse(player.isAfk(), "Internal tracked player should update AFK state");
    }
}
