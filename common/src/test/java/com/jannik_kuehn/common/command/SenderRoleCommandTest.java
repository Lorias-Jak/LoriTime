package com.jannik_kuehn.common.command;

import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.LoriTimePlayerConverter;
import com.jannik_kuehn.common.api.common.CommonConsoleSender;
import com.jannik_kuehn.common.api.common.CommonPlayerSender;
import com.jannik_kuehn.common.api.scheduler.PluginScheduler;
import com.jannik_kuehn.common.api.scheduler.PluginTask;
import com.jannik_kuehn.common.api.storage.ManualTimeAdjustment;
import com.jannik_kuehn.common.api.storage.TimeEntryReason;
import com.jannik_kuehn.common.api.storage.UnifiedStorage;
import com.jannik_kuehn.common.config.localization.Localization;
import com.jannik_kuehn.common.exception.StorageException;
import com.jannik_kuehn.common.player.TrackedLoriTimePlayer;
import com.jannik_kuehn.common.utils.TimeParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.junit.jupiter.api.Test;

import java.util.OptionalLong;
import java.util.UUID;
import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SenderRoleCommandTest {

    private static final UUID PLAYER_ID = UUID.fromString("44174cf6-e76c-4994-899c-3387284ecd62");

    @Test
    void playerSenderTargetsOwnLoriTimeQuery() throws StorageException {
        final CommandContext context = new CommandContext();
        final CommonPlayerSender sender = mock(CommonPlayerSender.class);
        when(sender.hasPermission("loritime.see")).thenReturn(true);
        when(sender.getUniqueId()).thenReturn(PLAYER_ID);
        when(context.storage().getTime(PLAYER_ID)).thenReturn(OptionalLong.of(45L));
        when(context.playerConverter().getOnlinePlayer(PLAYER_ID))
                .thenReturn(new TrackedLoriTimePlayer(PLAYER_ID, "Lorias_"));

        new LoriTimeCommand(context.plugin(), context.localization()).execute(sender);

        verify(context.playerConverter()).getOnlinePlayer(PLAYER_ID);
        verify(sender).sendMessage(any(TextComponent.class));
    }

    @Test
    void consoleSenderCannotTargetOwnLoriTimeQuery() {
        final CommandContext context = new CommandContext();
        final CommonConsoleSender sender = mock(CommonConsoleSender.class);
        when(sender.hasPermission("loritime.see")).thenReturn(true);

        new LoriTimeCommand(context.plugin(), context.localization()).execute(sender);

        verify(context.playerConverter(), never()).getOnlinePlayer(any(UUID.class));
        verify(sender).sendMessage(any(TextComponent.class));
    }

    @Test
    void adminAdjustmentUsesPlayerActorIdentity() throws StorageException {
        final CommandContext context = new CommandContext();
        final CommonPlayerSender sender = mock(CommonPlayerSender.class);
        when(sender.getUniqueId()).thenReturn(PLAYER_ID);
        when(sender.getName()).thenReturn("Lorias_");

        adminCommand(context).modifyOnlineTime(sender, PLAYER_ID, 12L);

        verify(context.storage()).addTime(new ManualTimeAdjustment(PLAYER_ID, 12L,
                TimeEntryReason.MANUAL_ADJUSTMENT, PLAYER_ID, "Lorias_"));
    }

    @Test
    void adminAdjustmentUsesConsoleActorMetadata() throws StorageException {
        final CommandContext context = new CommandContext();
        final CommonConsoleSender sender = mock(CommonConsoleSender.class);

        adminCommand(context).modifyOnlineTime(sender, PLAYER_ID, 12L);

        verify(context.storage()).addTime(new ManualTimeAdjustment(PLAYER_ID, 12L,
                TimeEntryReason.MANUAL_ADJUSTMENT, (UUID) null, "CONSOLE"));
    }

    private LoriTimeAdminCommand adminCommand(final CommandContext context) {
        return new LoriTimeAdminCommand(context.plugin(), context.localization(), mock(TimeParser.class));
    }

    private record CommandContext(LoriTimePlugin plugin, UnifiedStorage storage, Localization localization,
                                  LoriTimePlayerConverter playerConverter) {

        private CommandContext() {
            this(mock(LoriTimePlugin.class), mock(UnifiedStorage.class), mock(Localization.class),
                    mock(LoriTimePlayerConverter.class));
            final PluginScheduler scheduler = mock(PluginScheduler.class);
            when(plugin().getLoggerFactory()).thenReturn(new LoggerFactory(Logger.getLogger("test")));
            when(plugin().getStorage()).thenReturn(storage());
            when(plugin().getLocalization()).thenReturn(localization());
            when(plugin().getPlayerConverter()).thenReturn(playerConverter());
            when(plugin().getScheduler()).thenReturn(scheduler);
            when(localization().getRawMessage(anyString())).thenReturn("[time]");
            when(localization().formatTextComponent(anyString())).thenReturn(Component.text("message"));
            doAnswer(invocation -> {
                invocation.<Runnable>getArgument(0).run();
                return mock(PluginTask.class);
            }).when(scheduler).runAsyncOnce(any());
        }
    }
}
