package com.jannik_kuehn.common;

import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import com.jannik_kuehn.common.platform.CommonServer;
import com.jannik_kuehn.common.scheduler.PluginScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoriTimePluginTest {

    private static final UUID PLAYER_ID = UUID.fromString("44174cf6-e76c-4994-899c-3387284ecd62");

    @TempDir
    private File dataFolder;

    @Test
    void afkKickMarkerIsConsumedOnce() {
        final LoriTimePlugin plugin = new LoriTimePlugin(new LoggerFactory(Logger.getLogger("test")), dataFolder,
                mock(PluginScheduler.class), mock(CommonServer.class), null);

        plugin.markAfkKick(PLAYER_ID);

        assertTrue(plugin.consumeAfkKick(PLAYER_ID), "Expected marked AFK kick to be consumed");
        assertFalse(plugin.consumeAfkKick(PLAYER_ID), "Expected AFK kick marker to be consumed once");
    }

}
