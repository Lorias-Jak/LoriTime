package com.jannik_kuehn.common.module.updater;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.common.CommonSender;
import com.jannik_kuehn.common.api.common.CommonServer;
import com.jannik_kuehn.common.api.logger.LoriTimeLogger;
import com.jannik_kuehn.common.api.scheduler.PluginScheduler;
import com.jannik_kuehn.common.api.scheduler.PluginTask;
import com.jannik_kuehn.common.config.Configuration;
import com.jannik_kuehn.common.config.localization.Localization;
import com.jannik_kuehn.common.module.updater.download.Downloader;
import com.jannik_kuehn.common.module.updater.version.Version;
import net.kyori.adventure.text.TextComponent;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.net.URL;
import java.time.Instant;
import java.time.InstantSource;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link Updater}.
 */
class UpdaterTest {

    /**
     * An {@link InstantSource} that can be advanced.
     */
    private static class MutableInstantSource implements InstantSource {
        private Instant now;

        /**
         * Constructor.
         *
         * @param start the start instant
         */
        public MutableInstantSource(final Instant start) {
            this.now = start;
        }

        /**
         * Returns the current instant.
         *
         * @return the current instant
         */
        @Override
        public Instant instant() {
            return now;
        }

        private void advanceSeconds(final long seconds) {
            now = now.plusSeconds(seconds);
        }
    }

    private LoriTimePlugin mockPlugin(final Configuration config, final CommonServer server, final PluginScheduler scheduler, final Localization loc) {
        final LoriTimePlugin plugin = mock(LoriTimePlugin.class);
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getServer()).thenReturn(server);
        when(plugin.getScheduler()).thenReturn(scheduler);
        when(plugin.getLocalization()).thenReturn(loc);
        return plugin;
    }

    private PluginScheduler immediateScheduler() {
        final PluginScheduler scheduler = mock(PluginScheduler.class);
        final PluginTask task = mock(PluginTask.class);

        final Answer<PluginTask> runNow1 = inv -> {
            ((Runnable) inv.getArgument(0)).run(); return task;
        };
        final Answer<PluginTask> runNow2 = inv -> {
            ((Runnable) inv.getArgument(1)).run(); return task;
        };
        final Answer<PluginTask> runNow3 = inv -> {
            ((Runnable) inv.getArgument(2)).run(); return task;
        };

        when(scheduler.runAsyncOnce(any())).thenAnswer(runNow1);
        when(scheduler.runAsyncOnceLater(anyLong(), any())).thenAnswer(runNow2);
        when(scheduler.scheduleSync(any())).thenAnswer(runNow1);

        when(scheduler.scheduleAsync(anyLong(), anyLong(), any())).thenAnswer(runNow3);

        return scheduler;
    }

    private Localization mockLocalization() {
        final Localization loc = mock(Localization.class);
        when(loc.getRawMessage(anyString())).thenReturn("msg");
        when(loc.formatTextComponent(anyString())).thenReturn(mock(TextComponent.class));
        when(loc.formatTextComponentWithoutPrefix(anyString())).thenReturn(mock(TextComponent.class));
        return loc;
    }

    private Configuration mockConfig(final boolean checkForUpdates, final boolean autoUpdate, final boolean inGameNotification, final String strategy) {
        final Configuration cfg = mock(Configuration.class);
        when(cfg.getBoolean(eq("updater.checkForUpdates"), anyBoolean())).thenReturn(checkForUpdates);
        when(cfg.getBoolean(eq("updater.checkForUpdates"))).thenReturn(checkForUpdates);
        when(cfg.getBoolean(eq("updater.autoUpdate"), anyBoolean())).thenReturn(autoUpdate);
        when(cfg.getBoolean(eq("updater.autoUpdate"))).thenReturn(autoUpdate);
        when(cfg.getBoolean(eq("updater.inGameNotification"), anyBoolean())).thenReturn(inGameNotification);
        when(cfg.getBoolean(eq("updater.inGameNotification"))).thenReturn(inGameNotification);
        when(cfg.getString(eq("updater.updateStrategy"), anyString())).thenReturn(strategy);
        when(cfg.getString(eq("updater.updateStrategy"))).thenReturn(strategy);
        return cfg;
    }

    @Test
    void search_withAutoUpdate_triggersDownloader_and_clearsLatestAfterExecute() throws Exception {
        final LoriTimeLogger logger = mock(LoriTimeLogger.class);
        final MutableInstantSource clock = new MutableInstantSource(Instant.parse("2024-01-01T00:00:00Z"));

        final CommonServer server = mock(CommonServer.class, withSettings().extraInterfaces(CommonSender.class));
        when(server.getPluginVersion()).thenReturn("1.0.0");
        when(server.getPluginJarName()).thenReturn("LoriTime.jar");

        final PluginScheduler scheduler = immediateScheduler();
        final Localization loc = mockLocalization();
        final Configuration cfg = mockConfig(true, false, true, "MINOR");
        final LoriTimePlugin plugin = mockPlugin(cfg, server, scheduler, loc);

        final UpdateSourceHandler handler = mock(UpdateSourceHandler.class);
        when(handler.searchUpdate(any(), anyString(), any())).thenReturn(
                Pair.of(new Version("1.0.1"), new File("build.tmp").toURI().toURL().toString()),
                Pair.of(new Version("1.0.1"), null)
        );

        final Downloader downloader = mock(Downloader.class);
        final Updater updater = new Updater(logger, new Version("1.0.0"), handler, plugin, clock, downloader);

        final CommonSender mockedSender = mock(CommonSender.class);
        updater.search();
        updater.update(mockedSender);

        verify(downloader, timeout(1000)).downloadFile(any(URL.class), argThat(f -> "LoriTime.jar".equals(f.getName())));
        assertFalse(updater.isUpdateAvailable(), "After successful executeUpdate, latest link should be cleared");
    }

    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    void sendPlayerUpdateNotification_throttles_per_player_and_respects_console() throws Exception {
        final LoriTimeLogger logger = mock(LoriTimeLogger.class);
        final MutableInstantSource clock = new MutableInstantSource(Instant.parse("2024-01-01T00:00:00Z"));

        final CommonServer server = mock(CommonServer.class, withSettings().extraInterfaces(CommonSender.class));
        when(server.getPluginVersion()).thenReturn("1.0.0");
        when(server.getPluginJarName()).thenReturn("LoriTime.jar");

        final PluginScheduler scheduler = immediateScheduler();
        final Localization loc = mockLocalization();
        final Configuration cfg = mockConfig(true, false, true, "MINOR");
        final LoriTimePlugin plugin = mockPlugin(cfg, server, scheduler, loc);

        final UpdateSourceHandler handler = mock(UpdateSourceHandler.class);
        when(handler.searchUpdate(any(), anyString(), any())).thenReturn(Pair.of(new Version("1.0.1"),
                new File("build.tmp").toURI().toURL().toString()));

        final Downloader downloader = mock(Downloader.class);
        final Updater updater = new Updater(logger, new Version("1.0.0"), handler, plugin, clock, downloader);

        updater.search();
        assertTrue(updater.isUpdateAvailable(), "Update should be available");

        final AtomicInteger msgCount = new AtomicInteger();
        final CommonSender player = mock(CommonSender.class);
        final UUID playerId = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.isConsole()).thenReturn(false);
        when(player.isOnline()).thenReturn(true);
        doAnswer(inv -> {
            msgCount.incrementAndGet(); return null;
        }).when(player).sendMessage(any(TextComponent.class));

        updater.sendPlayerUpdateNotification(player);
        updater.sendPlayerUpdateNotification(player);
        assertEquals(1, msgCount.get(), "Second immediate notification should be throttled");

        clock.advanceSeconds(24 * 3600 + 1);
        updater.sendPlayerUpdateNotification(player);
        assertEquals(2, msgCount.get(), "After throttle window, it should notify again");

        final CommonSender console = mock(CommonSender.class);
        when(console.isConsole()).thenReturn(true);
        updater.sendPlayerUpdateNotification(console);
        assertEquals(2, msgCount.get(), "Expected 2 messages for console");
    }
}
