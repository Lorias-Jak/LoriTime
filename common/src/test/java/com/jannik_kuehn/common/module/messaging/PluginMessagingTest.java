package com.jannik_kuehn.common.module.messaging;

import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.LoriTimePlayerConverter;
import com.jannik_kuehn.common.api.common.CommonPlayerSender;
import com.jannik_kuehn.common.api.common.CommonServer;
import com.jannik_kuehn.common.api.scheduler.PluginScheduler;
import com.jannik_kuehn.common.api.scheduler.PluginTask;
import com.jannik_kuehn.common.api.storage.TimeAccumulator;
import com.jannik_kuehn.common.api.storage.TimeEntryReason;
import com.jannik_kuehn.common.api.storage.UnifiedStorage;
import com.jannik_kuehn.common.exception.StorageException;
import com.jannik_kuehn.common.module.afk.AfkStatusProvider;
import com.jannik_kuehn.common.module.afk.AfkTransitionType;
import com.jannik_kuehn.common.player.TrackedLoriTimePlayer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SuppressWarnings({"PMD.CloseResource", "PMD.UnitTestContainsTooManyAsserts"})
class PluginMessagingTest {

    private static final UUID PLAYER = UUID.fromString("44174cf6-e76c-4994-899c-3387284ecd62");

    @Test
    void ignoresStaleRemoteSessionMessage() throws StorageException {
        final LoriTimePlugin plugin = pluginWithInlineScheduler();
        final UnifiedStorage storage = mock(UnifiedStorage.class);
        when(plugin.getStorage()).thenReturn(storage);
        final CapturingPluginMessaging messaging = new CapturingPluginMessaging(plugin);

        messaging.processPluginMessage("loritime:storage", messaging.data(PLAYER, StorageMessageType.SESSION.wireValue(), 3,
                "Lorias_", "lobby", "spawn",
                1_000L, 6_000L, TimeEntryReason.PLAYER_LEAVE.name()));

        verify(storage, never()).persistSession(any());
    }

    @Test
    void ignoresUnsupportedRemoteSessionProtocolVersion() throws StorageException {
        final LoriTimePlugin plugin = pluginWithInlineScheduler();
        final UnifiedStorage storage = mock(UnifiedStorage.class);
        when(plugin.getStorage()).thenReturn(storage);
        final CapturingPluginMessaging messaging = new CapturingPluginMessaging(plugin);

        messaging.processPluginMessage("loritime:storage", messaging.data(PLAYER, StorageMessageType.SESSION.wireValue(),
                999, "Lorias_", "lobby", "spawn", 1_000L, 6_000L, TimeEntryReason.PLAYER_LEAVE.name()));

        verify(storage, never()).persistSession(any());
    }

    @Test
    void appliesRemoteAddOperationToStorage() throws StorageException {
        final LoriTimePlugin plugin = pluginWithInlineScheduler();
        final UnifiedStorage storage = mock(UnifiedStorage.class);
        when(plugin.getStorage()).thenReturn(storage);
        final CapturingPluginMessaging messaging = new CapturingPluginMessaging(plugin);

        messaging.processPluginMessage("loritime:storage", messaging.data(PLAYER, StorageMessageType.ADD.wireValue(), 12L));

        verify(storage).addTime(PLAYER, 12L);
    }

    @Test
    void ignoresUnknownStorageOperation() throws StorageException {
        final LoriTimePlugin plugin = pluginWithInlineScheduler();
        final UnifiedStorage storage = mock(UnifiedStorage.class);
        final TimeAccumulator accumulator = mock(TimeAccumulator.class);
        when(plugin.getStorage()).thenReturn(storage);
        when(plugin.getAccumulator()).thenReturn(accumulator);
        final CapturingPluginMessaging messaging = new CapturingPluginMessaging(plugin);

        messaging.processPluginMessage("loritime:storage", messaging.data(PLAYER, "unknown_operation", 2));

        verifyNoInteractions(storage, accumulator);
    }

    @Test
    void ignoresMasterOnlySendOperation() throws StorageException {
        final LoriTimePlugin plugin = pluginWithInlineScheduler();
        final UnifiedStorage storage = mock(UnifiedStorage.class);
        final TimeAccumulator accumulator = mock(TimeAccumulator.class);
        when(plugin.getStorage()).thenReturn(storage);
        when(plugin.getAccumulator()).thenReturn(accumulator);
        final CapturingPluginMessaging messaging = new CapturingPluginMessaging(plugin);

        messaging.processPluginMessage("loritime:storage", messaging.data(PLAYER, StorageMessageType.SEND.wireValue(), 44L));

        verifyNoInteractions(storage, accumulator);
    }

    @Test
    void appliesRemoteWorldContextToAccumulator() throws StorageException {
        final LoriTimePlugin plugin = pluginWithInlineScheduler();
        final TimeAccumulator accumulator = mock(TimeAccumulator.class);
        when(plugin.getAccumulator()).thenReturn(accumulator);
        final CapturingPluginMessaging messaging = new CapturingPluginMessaging(plugin);

        messaging.processPluginMessage("loritime:storage", messaging.data(PLAYER, StorageMessageType.WORLD.wireValue(),
                StorageMessageProtocol.VERSION, "world_nether", 7_000L));

        verify(accumulator).updateWorldContext(PLAYER, "world_nether", 7_000L);
    }

    @Test
    void appliesRemoteWorldSwitchToAccumulator() throws StorageException {
        final LoriTimePlugin plugin = pluginWithInlineScheduler();
        final TimeAccumulator accumulator = mock(TimeAccumulator.class);
        when(plugin.getAccumulator()).thenReturn(accumulator);
        final CapturingPluginMessaging messaging = new CapturingPluginMessaging(plugin);

        messaging.processPluginMessage("loritime:storage", messaging.data(PLAYER, StorageMessageType.WORLD_SWITCH.wireValue(),
                StorageMessageProtocol.VERSION, "world_nether", 7_000L));

        verify(accumulator).switchWorldContext(PLAYER, "world_nether", 7_000L);
    }

    @Test
    void ignoresUnsupportedWorldContextProtocolVersion() throws StorageException {
        final LoriTimePlugin plugin = pluginWithInlineScheduler();
        final TimeAccumulator accumulator = mock(TimeAccumulator.class);
        when(plugin.getAccumulator()).thenReturn(accumulator);
        final CapturingPluginMessaging messaging = new CapturingPluginMessaging(plugin);

        messaging.processPluginMessage("loritime:storage", messaging.data(PLAYER, StorageMessageType.WORLD.wireValue(),
                999, "world_nether", 7_000L));

        verify(accumulator, never()).updateWorldContext(any(), anyString(), anyLong());
    }

    @Test
    void ignoresUnsupportedWorldSwitchProtocolVersion() throws StorageException {
        final LoriTimePlugin plugin = pluginWithInlineScheduler();
        final TimeAccumulator accumulator = mock(TimeAccumulator.class);
        when(plugin.getAccumulator()).thenReturn(accumulator);
        final CapturingPluginMessaging messaging = new CapturingPluginMessaging(plugin);

        messaging.processPluginMessage("loritime:storage", messaging.data(PLAYER, StorageMessageType.WORLD_SWITCH.wireValue(),
                999, "world_nether", 7_000L));

        verify(accumulator, never()).switchWorldContext(any(), anyString(), anyLong());
    }

    @Test
    void ignoresMalformedStoragePayload() throws StorageException {
        final LoriTimePlugin plugin = pluginWithInlineScheduler();
        final TimeAccumulator accumulator = mock(TimeAccumulator.class);
        when(plugin.getAccumulator()).thenReturn(accumulator);
        final CapturingPluginMessaging messaging = new CapturingPluginMessaging(plugin);

        messaging.processPluginMessage("loritime:storage", messaging.data(PLAYER, StorageMessageType.WORLD.wireValue(),
                StorageMessageProtocol.VERSION));

        verifyNoInteractions(accumulator);
    }

    @Test
    void answersSlaveReadRequestWithCurrentTotal() throws StorageException {
        final LoriTimePlugin plugin = pluginWithInlineScheduler();
        final UnifiedStorage storage = mock(UnifiedStorage.class);
        when(plugin.getStorage()).thenReturn(storage);
        when(storage.getTime(PLAYER)).thenReturn(OptionalLong.of(44L));
        final CapturingPluginMessaging messaging = new CapturingPluginMessaging(plugin);

        messaging.processPluginMessage("loritime:storage", messaging.data(PLAYER, StorageMessageType.GET.wireValue()));

        assertEquals(1, messaging.sentMessages.size(), "Expected one message to be sent");
        final SentMessage sent = messaging.sentMessages.getFirst();
        assertEquals("loritime:storage", sent.channel(), "Expected the correct channel");
        assertEquals(3, sent.payload().length, "Expected three payload elements");
        assertEquals(PLAYER, sent.payload()[0], "Expected the same UUID as the one passed to the plugin messaging");
        assertEquals(StorageMessageType.SEND.wireValue(), sent.payload()[1], "Expected the correct payload element");
        assertEquals(44L, sent.payload()[2], "Expected the correct payload element");
    }

    @Test
    void appliesSupportedAfkStartMessage() {
        final AfkMessagingContext context = new AfkMessagingContext();
        final CapturingPluginMessaging messaging = new CapturingPluginMessaging(context.plugin());

        messaging.processPluginMessage("loritime:afk", messaging.data(PLAYER, AfkMessageProtocol.VERSION,
                AfkTransitionType.START.name(), 15L));

        verify(context.afkStatusProvider()).setPlayerAFK(context.player(), 15L);
    }

    @Test
    void appliesSupportedAfkResumeMessage() {
        final AfkMessagingContext context = new AfkMessagingContext();
        final CapturingPluginMessaging messaging = new CapturingPluginMessaging(context.plugin());

        messaging.processPluginMessage("loritime:afk", messaging.data(PLAYER, AfkMessageProtocol.VERSION,
                AfkTransitionType.RESUME.name()));

        verify(context.afkStatusProvider()).resumePlayerAFK(context.player());
    }

    @Test
    void ignoresUnsupportedAfkProtocolVersion() {
        final AfkMessagingContext context = new AfkMessagingContext();
        final CapturingPluginMessaging messaging = new CapturingPluginMessaging(context.plugin());

        messaging.processPluginMessage("loritime:afk", messaging.data(PLAYER, 999, AfkTransitionType.START.name(), 15L));

        verifyNoInteractions(context.afkStatusProvider());
    }

    @Test
    void ignoresInvalidAfkTransition() {
        final AfkMessagingContext context = new AfkMessagingContext();
        final CapturingPluginMessaging messaging = new CapturingPluginMessaging(context.plugin());

        messaging.processPluginMessage("loritime:afk", messaging.data(PLAYER, AfkMessageProtocol.VERSION, "INVALID", 15L));

        verifyNoInteractions(context.afkStatusProvider());
    }

    @Test
    void ignoresSlaveAfkKickTransition() {
        final AfkMessagingContext context = new AfkMessagingContext();
        final CapturingPluginMessaging messaging = new CapturingPluginMessaging(context.plugin());

        messaging.processPluginMessage("loritime:afk", messaging.data(PLAYER, AfkMessageProtocol.VERSION,
                AfkTransitionType.KICK.name()));

        verifyNoInteractions(context.afkStatusProvider());
    }

    @Test
    void ignoresLegacyUnversionedAfkPayload() {
        final AfkMessagingContext context = new AfkMessagingContext();
        final CapturingPluginMessaging messaging = new CapturingPluginMessaging(context.plugin());

        messaging.processPluginMessage("loritime:afk", messaging.data(PLAYER, "true", 15L));

        verifyNoInteractions(context.afkStatusProvider());
    }

    @Test
    void ignoresMalformedAfkPayload() {
        final AfkMessagingContext context = new AfkMessagingContext();
        final CapturingPluginMessaging messaging = new CapturingPluginMessaging(context.plugin());

        messaging.processPluginMessage("loritime:afk", messaging.data(PLAYER, AfkMessageProtocol.VERSION,
                AfkTransitionType.START.name()));

        verifyNoInteractions(context.afkStatusProvider());
    }

    @Test
    void serializesUuidAsSixteenBytes() {
        final LoriTimePlugin plugin = pluginWithInlineScheduler();
        final CapturingPluginMessaging messaging = new CapturingPluginMessaging(plugin);

        final byte[] bytes = messaging.data(PLAYER);

        assertArrayEquals(new byte[]{
                0x44, 0x17, 0x4c, (byte) 0xf6, (byte) 0xe7, 0x6c, 0x49, (byte) 0x94,
                (byte) 0x89, (byte) 0x9c, 0x33, (byte) 0x87, 0x28, 0x4e, (byte) 0xcd, 0x62
        }, bytes, "Expected the correct byte array");
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

    private static final class CapturingPluginMessaging extends PluginMessaging {

        private final List<SentMessage> sentMessages = new ArrayList<>();

        private CapturingPluginMessaging(final LoriTimePlugin loriTimePlugin) {
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

    private record AfkMessagingContext(LoriTimePlugin plugin, AfkStatusProvider afkStatusProvider,
                                       TrackedLoriTimePlayer player) {

        private AfkMessagingContext() {
            this(pluginWithAfkMessaging(), mock(AfkStatusProvider.class), new TrackedLoriTimePlayer(PLAYER, "Lorias_"));
            final CommonServer server = mock(CommonServer.class);
            final CommonPlayerSender sender = mock(CommonPlayerSender.class);
            final LoriTimePlayerConverter converter = mock(LoriTimePlayerConverter.class);
            when(plugin().getServer()).thenReturn(server);
            when(plugin().getPlayerConverter()).thenReturn(converter);
            when(plugin().getAfkStatusProvider()).thenReturn(afkStatusProvider());
            when(server.getPlayer(PLAYER)).thenReturn(Optional.of(sender));
            when(converter.getOnlinePlayer(PLAYER)).thenReturn(player());
        }

        private static LoriTimePlugin pluginWithAfkMessaging() {
            return new PluginMessagingTest().pluginWithInlineScheduler();
        }
    }
}
