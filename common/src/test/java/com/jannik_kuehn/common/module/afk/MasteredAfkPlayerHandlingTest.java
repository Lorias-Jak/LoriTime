package com.jannik_kuehn.common.module.afk;

import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.LoriTimePlayer;
import com.jannik_kuehn.common.api.common.CommonSender;
import com.jannik_kuehn.common.api.common.CommonServer;
import com.jannik_kuehn.common.api.storage.SessionContextDefaults;
import com.jannik_kuehn.common.api.storage.TimeAccumulator;
import com.jannik_kuehn.common.api.storage.TimeEntryReason;
import com.jannik_kuehn.common.config.Configuration;
import com.jannik_kuehn.common.config.localization.Localization;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class MasteredAfkPlayerHandlingTest {

    private static final UUID PLAYER_ID = UUID.fromString("44174cf6-e76c-4994-899c-3387284ecd62");

    @Test
    void afkStopsSessionWithAfkReasonByDefault() throws Exception {
        final TestContext context = new TestContext(false, false);
        final MasteredAfkPlayerHandling handling = new MasteredAfkPlayerHandling(context.plugin());

        handling.executePlayerAfk(new LoriTimePlayer(PLAYER_ID, "Lorias_"), 15L);

        verify(context.accumulator()).stopAccumulatingAndSaveOnlineTime(eq(PLAYER_ID), anyLong(),
                eq(TimeEntryReason.PLAYER_AFK));
        verify(context.sender()).sendMessage(any(TextComponent.class));
    }

    @Test
    void stopCountBypassKeepsSessionCounting() throws Exception {
        final TestContext context = new TestContext(false, false);
        when(context.sender().hasPermission("loritime.afk.bypass.stopCount")).thenReturn(true);
        final MasteredAfkPlayerHandling handling = new MasteredAfkPlayerHandling(context.plugin());

        handling.executePlayerAfk(new LoriTimePlayer(PLAYER_ID, "Lorias_"), 15L);

        verify(context.accumulator(), never()).stopAccumulatingAndSaveOnlineTime(eq(PLAYER_ID), anyLong(),
                eq(TimeEntryReason.PLAYER_AFK));
        verify(context.sender()).sendMessage(any(TextComponent.class));
    }

    @Test
    void afkAnnounceIsSentToPermittedViewers() {
        final TestContext context = new TestContext(false, false);
        when(context.sender().hasPermission("loritime.afk.announce.afkAnnounce")).thenReturn(true);
        final MasteredAfkPlayerHandling handling = new MasteredAfkPlayerHandling(context.plugin());

        handling.executePlayerAfk(new LoriTimePlayer(PLAYER_ID, "Lorias_"), 15L);

        verify(context.sender(), times(2)).sendMessage(any(TextComponent.class));
    }

    @Test
    void resumeRestartsOnlyWhenAfkStoppedCounting() throws Exception {
        final TestContext context = new TestContext(false, false);
        final LoriTimePlayer player = new LoriTimePlayer(PLAYER_ID, "Lorias_");
        final MasteredAfkPlayerHandling handling = new MasteredAfkPlayerHandling(context.plugin());

        handling.executePlayerAfk(player, 15L);
        clearInvocations(context.sender());
        handling.executePlayerResume(player);

        verify(context.accumulator()).startAccumulating(eq(PLAYER_ID), eq("Lorias_"), eq(SessionContextDefaults.SERVER),
                eq(SessionContextDefaults.WORLD), anyLong());
        verify(context.sender()).sendMessage(any(TextComponent.class));
    }

    @Test
    void resumeDoesNotRestartWhenStopCountWasBypassed() throws Exception {
        final TestContext context = new TestContext(false, false);
        when(context.sender().hasPermission("loritime.afk.bypass.stopCount")).thenReturn(true);
        final LoriTimePlayer player = new LoriTimePlayer(PLAYER_ID, "Lorias_");
        final MasteredAfkPlayerHandling handling = new MasteredAfkPlayerHandling(context.plugin());

        handling.executePlayerAfk(player, 15L);
        clearInvocations(context.sender());
        handling.executePlayerResume(player);

        verify(context.accumulator(), never()).startAccumulating(eq(PLAYER_ID), eq("Lorias_"),
                eq(SessionContextDefaults.SERVER), eq(SessionContextDefaults.WORLD), anyLong());
        verify(context.sender()).sendMessage(any(TextComponent.class));
    }

    @Test
    void autoKickMarksNextDisconnectAsAfk() throws Exception {
        final TestContext context = new TestContext(false, true);
        final LoriTimePlayer player = new LoriTimePlayer(PLAYER_ID, "Lorias_");
        final MasteredAfkPlayerHandling handling = new MasteredAfkPlayerHandling(context.plugin());

        handling.executePlayerAfk(player, 15L);

        verify(context.plugin()).markAfkKick(PLAYER_ID);
        verify(context.server()).kickPlayer(eq(player), any(TextComponent.class));
        verify(context.sender(), never()).sendMessage(any(TextComponent.class));
    }

    private record TestContext(LoriTimePlugin plugin, CommonServer server, CommonSender sender,
                               TimeAccumulator accumulator) {

        private TestContext(final boolean removeTime, final boolean autoKick) {
            this(mock(LoriTimePlugin.class), mock(CommonServer.class), mock(CommonSender.class),
                    mock(TimeAccumulator.class));
            final Configuration config = mock(Configuration.class);
            final Localization localization = mock(Localization.class);
            when(plugin().getLoggerFactory()).thenReturn(new LoggerFactory(Logger.getLogger("test")));
            when(plugin().getConfig()).thenReturn(config);
            when(plugin().getServer()).thenReturn(server());
            when(plugin().getAccumulator()).thenReturn(accumulator());
            when(plugin().getLocalization()).thenReturn(localization);
            when(server().getPlayer(PLAYER_ID)).thenReturn(Optional.of(sender()));
            when(server().getOnlinePlayers()).thenReturn(new CommonSender[]{sender()});
            when(sender().getName()).thenReturn("Lorias_");
            when(config.getBoolean("afk.enabled", false)).thenReturn(true);
            when(config.getBoolean("afk.removeTime", true)).thenReturn(removeTime);
            when(config.getBoolean("afk.autoKick", true)).thenReturn(autoKick);
            when(localization.getRawMessage(any())).thenReturn("[player] [time]");
            when(localization.formatTextComponent(any())).thenReturn(Component.text("message"));
            when(localization.formatTextComponentWithoutPrefix(any())).thenReturn(Component.text("message"));
        }
    }
}
