package com.jannik_kuehn.common.module.messaging;

import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.scheduler.PluginScheduler;
import com.jannik_kuehn.common.api.scheduler.PluginTask;
import com.jannik_kuehn.common.api.storage.AccumulatingTimeStorage;
import com.jannik_kuehn.common.api.storage.PlayerSessionChunk;
import com.jannik_kuehn.common.api.storage.TimeEntryReason;
import com.jannik_kuehn.common.api.storage.UnifiedStorage;
import com.jannik_kuehn.common.exception.StorageException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"PMD.CloseResource", "PMD.TestClassWithoutTestCases", "PMD.UnitTestAssertionsShouldIncludeMessage",
        "PMD.UnitTestContainsTooManyAsserts"})
class PluginMessagingTest {

    private static final UUID PLAYER = UUID.fromString("44174cf6-e76c-4994-899c-3387284ecd62");

    @Test
    void persistsVersionedRemoteSessionMessage() throws StorageException {
        final LoriTimePlugin plugin = pluginWithInlineScheduler();
        final UnifiedStorage storage = mock(UnifiedStorage.class);
        when(plugin.getStorage()).thenReturn(storage);
        final TestPluginMessaging messaging = new TestPluginMessaging(plugin);

        messaging.processPluginMessage("loritime:storage", messaging.data(PLAYER, "session", 2, "Lorias_", "lobby", "spawn",
                1_000L, 6_000L, TimeEntryReason.PLAYER_LEAVE.name()));

        final ArgumentCaptor<PlayerSessionChunk> captor = ArgumentCaptor.forClass(PlayerSessionChunk.class);
        verify(storage).persistSession(captor.capture());
        final PlayerSessionChunk chunk = captor.getValue();
        assertEquals(PLAYER, chunk.uuid());
        assertEquals(Optional.of("Lorias_"), chunk.name());
        assertEquals("lobby", chunk.server());
        assertEquals("spawn", chunk.world());
        assertEquals(1_000L, chunk.startedAtMs());
        assertEquals(6_000L, chunk.stoppedAtMs());
        assertEquals(TimeEntryReason.PLAYER_LEAVE, chunk.reason());
    }

    @Test
    void ignoresUnsupportedRemoteSessionProtocolVersion() throws StorageException {
        final LoriTimePlugin plugin = pluginWithInlineScheduler();
        final UnifiedStorage storage = mock(UnifiedStorage.class);
        when(plugin.getStorage()).thenReturn(storage);
        final TestPluginMessaging messaging = new TestPluginMessaging(plugin);

        messaging.processPluginMessage("loritime:storage", messaging.data(PLAYER, "session", 999, "Lorias_", "lobby", "spawn",
                1_000L, 6_000L, TimeEntryReason.PLAYER_LEAVE.name()));

        verify(storage, never()).persistSession(any());
    }

    @Test
    void answersSlaveReadRequestWithCurrentTotal() throws StorageException {
        final LoriTimePlugin plugin = pluginWithInlineScheduler();
        final AccumulatingTimeStorage storage = mock(AccumulatingTimeStorage.class);
        when(plugin.getAccumulatingStorage()).thenReturn(storage);
        when(storage.getTime(PLAYER)).thenReturn(OptionalLong.of(44L));
        final TestPluginMessaging messaging = new TestPluginMessaging(plugin);

        messaging.processPluginMessage("loritime:storage", messaging.data(PLAYER, "get"));

        assertEquals(1, messaging.sentMessages.size());
        final SentMessage sent = messaging.sentMessages.getFirst();
        assertEquals("loritime:storage", sent.channel());
        assertEquals(3, sent.payload().length);
        assertEquals(PLAYER, sent.payload()[0]);
        assertEquals("send", sent.payload()[1]);
        assertEquals(44L, sent.payload()[2]);
    }

    @Test
    void serializesUuidAsSixteenBytes() {
        final LoriTimePlugin plugin = pluginWithInlineScheduler();
        final TestPluginMessaging messaging = new TestPluginMessaging(plugin);

        final byte[] bytes = messaging.data(PLAYER);

        assertArrayEquals(new byte[] {
                0x44, 0x17, 0x4c, (byte) 0xf6, (byte) 0xe7, 0x6c, 0x49, (byte) 0x94,
                (byte) 0x89, (byte) 0x9c, 0x33, (byte) 0x87, 0x28, 0x4e, (byte) 0xcd, 0x62
        }, bytes);
    }

    private LoriTimePlugin pluginWithInlineScheduler() {
        final LoriTimePlugin plugin = mock(LoriTimePlugin.class);
        final PluginScheduler scheduler = mock(PluginScheduler.class);
        when(plugin.getLoggerFactory()).thenReturn(new LoggerFactory(Logger.getLogger("test")));
        when(plugin.getScheduler()).thenReturn(scheduler);
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return (PluginTask) () -> {
            };
        }).when(scheduler).runAsyncOnce(any());
        return plugin;
    }

    private static final class TestPluginMessaging extends PluginMessaging {

        private final List<SentMessage> sentMessages = new ArrayList<>();

        private TestPluginMessaging(final LoriTimePlugin loriTimePlugin) {
            super(loriTimePlugin);
        }

        @Override
        public void sendPluginMessage(final String channelIdentifier, final Object... message) {
            sentMessages.add(new SentMessage(channelIdentifier, message));
        }

        private byte[] data(final Object... message) {
            return getDataAsByte(message);
        }
    }

    private record SentMessage(String channel, Object[] payload) {
    }
}
