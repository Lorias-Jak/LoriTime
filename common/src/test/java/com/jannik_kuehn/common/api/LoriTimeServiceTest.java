package com.jannik_kuehn.common.api;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.storage.ManualTimeAdjustment;
import com.jannik_kuehn.common.api.storage.TimeEntryReason;
import com.jannik_kuehn.common.api.storage.UnifiedStorage;
import com.jannik_kuehn.common.exception.StorageException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ResourceLock("LoriTimeAPI")
@SuppressWarnings({"PMD.TooManyMethods", "PMD.AvoidAccessibilityAlteration"})
class LoriTimeServiceTest {

    private static final UUID PLAYER_ID = UUID.fromString("44174cf6-e76c-4994-899c-3387284ecd62");

    private static final UUID ACTOR_ID = UUID.fromString("22fc9749-1470-4998-b74f-22add1f4dbb3");

    private LoriTimePlugin plugin;

    private UnifiedStorage storage;

    private LoriTimeService service;

    @BeforeEach
    void setUp() throws ReflectiveOperationException {
        resetApi();
        plugin = mock(LoriTimePlugin.class);
        storage = mock(UnifiedStorage.class);
        when(plugin.getStorage()).thenReturn(storage);
        service = new LoriTimeService(plugin);
    }

    @AfterEach
    void tearDown() throws ReflectiveOperationException {
        resetApi();
    }

    @Test
    void serviceAccessorIsEmptyBeforeInitialization() {
        assertTrue(LoriTimeAPI.service().isEmpty(), "Service should be unavailable before initialization");
    }

    @Test
    void serviceAccessorReturnsFacadeAfterInitialization() {
        LoriTimeAPI.setPlugin(plugin);

        assertTrue(LoriTimeAPI.service().isPresent(), "Service should be available after initialization");
    }

    @Test
    void findsUuidByPlayerName() throws StorageException {
        when(storage.getUuid("Lorias_")).thenReturn(Optional.of(PLAYER_ID));

        assertEquals(Optional.of(PLAYER_ID), service.findUuid("Lorias_"), "Expected facade to return stored UUID");
    }

    @Test
    void findsLatestNameByUuid() throws StorageException {
        when(storage.getName(PLAYER_ID)).thenReturn(Optional.of("Lorias_"));

        assertEquals(Optional.of("Lorias_"), service.findName(PLAYER_ID), "Expected facade to return stored name");
    }

    @Test
    void returnsOnlineTimeAsDuration() throws StorageException {
        when(storage.getTime(PLAYER_ID)).thenReturn(OptionalLong.of(3661));

        assertEquals(Optional.of(Duration.ofSeconds(3661)), service.getOnlineTime(PLAYER_ID),
                "Expected facade to expose time as Duration");
    }

    @Test
    void returnsEmptyOnlineTimeForUnknownPlayer() throws StorageException {
        when(storage.getTime(PLAYER_ID)).thenReturn(OptionalLong.empty());

        assertEquals(Optional.empty(), service.getOnlineTime(PLAYER_ID), "Unknown time should be empty");
    }

    @Test
    void returnsOnlineTimeByPlayerIdentity() throws StorageException {
        final LoriTimePlayer player = new LoriTimePlayerRef(PLAYER_ID, "Renamed");
        when(storage.getTime(PLAYER_ID)).thenReturn(OptionalLong.of(120));

        assertEquals(Optional.of(Duration.ofSeconds(120)), service.getOnlineTime(player),
                "Player overload should query by UUID");
    }

    @Test
    void addsSystemManualAdjustment() throws StorageException {
        service.addTime(PLAYER_ID, Duration.ofSeconds(30));

        verify(storage).addTime(new ManualTimeAdjustment(PLAYER_ID, 30L,
                TimeEntryReason.MANUAL_ADJUSTMENT, (UUID) null, LoriTimeService.API_ACTOR));
    }

    @Test
    void addsSystemManualAdjustmentByPlayerIdentity() throws StorageException {
        final LoriTimePlayer player = new LoriTimePlayerRef(PLAYER_ID, "Renamed");

        service.addTime(player, Duration.ofSeconds(30));

        verify(storage).addTime(new ManualTimeAdjustment(PLAYER_ID, 30L,
                TimeEntryReason.MANUAL_ADJUSTMENT, (UUID) null, LoriTimeService.API_ACTOR));
    }

    @Test
    void addsActorAwareManualAdjustment() throws StorageException {
        service.addTime(PLAYER_ID, Duration.ofSeconds(-45), ACTOR_ID, "Admin");

        verify(storage).addTime(new ManualTimeAdjustment(PLAYER_ID, -45L,
                TimeEntryReason.MANUAL_ADJUSTMENT, ACTOR_ID, "Admin"));
    }

    @Test
    void addsActorAwareManualAdjustmentByPlayerIdentities() throws StorageException {
        final LoriTimePlayer player = new LoriTimePlayerRef(PLAYER_ID, "Renamed");
        final LoriTimePlayer actor = new LoriTimePlayerRef(ACTOR_ID, "Admin");

        service.addTime(player, Duration.ofSeconds(-45), actor);

        verify(storage).addTime(new ManualTimeAdjustment(PLAYER_ID, -45L,
                TimeEntryReason.MANUAL_ADJUSTMENT, ACTOR_ID, "Admin"));
    }

    @Test
    void rejectsSubSecondAdjustment() throws StorageException {
        assertThrows(IllegalArgumentException.class, () -> service.addTime(PLAYER_ID, Duration.ofMillis(500)),
                "Sub-second adjustments should be rejected");

        verify(storage, never()).addTime(any(ManualTimeAdjustment.class));
    }

    @Test
    void rejectsNullInputsBeforeWriting() throws StorageException {
        assertAll(
                () -> assertThrows(NullPointerException.class, () -> service.findUuid(null)),
                () -> assertThrows(NullPointerException.class, () -> service.findName(null)),
                () -> assertThrows(NullPointerException.class, () -> service.getOnlineTime((UUID) null)),
                () -> assertThrows(NullPointerException.class, () -> service.getOnlineTime((LoriTimePlayer) null)),
                () -> assertThrows(NullPointerException.class, () -> service.addTime((UUID) null, Duration.ofSeconds(1))),
                () -> assertThrows(NullPointerException.class,
                        () -> service.addTime((LoriTimePlayer) null, Duration.ofSeconds(1))),
                () -> assertThrows(NullPointerException.class, () -> service.addTime(PLAYER_ID, null)),
                () -> assertThrows(NullPointerException.class,
                        () -> service.addTime(PLAYER_ID, Duration.ofSeconds(1), ACTOR_ID, null))
        );
        verify(storage, never()).addTime(any(ManualTimeAdjustment.class));
    }

    @Test
    void rejectsInvalidPlayerIdentityBeforeWriting() throws StorageException {
        final LoriTimePlayer missingName = mock(LoriTimePlayer.class);
        when(missingName.getUniqueId()).thenReturn(PLAYER_ID);
        when(missingName.getName()).thenReturn(" ");

        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> service.getOnlineTime(missingName)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> service.addTime(missingName, Duration.ofSeconds(1))),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> service.addTime(new LoriTimePlayerRef(PLAYER_ID, "Lorias_"),
                                Duration.ofSeconds(1), missingName))
        );
        verify(storage, never()).addTime(any(ManualTimeAdjustment.class));
    }

    @Test
    void wrapsStorageFailures() throws StorageException {
        when(storage.getName(PLAYER_ID)).thenThrow(new StorageException("failure"));

        assertThrows(LoriTimeApiException.class, () -> service.findName(PLAYER_ID),
                "Storage failures should be wrapped in the public API exception");
    }

    private void resetApi() throws ReflectiveOperationException {
        final Field field = LoriTimeAPI.class.getDeclaredField("loriTimePlugin");
        field.setAccessible(true);
        field.set(null, null);
    }
}
