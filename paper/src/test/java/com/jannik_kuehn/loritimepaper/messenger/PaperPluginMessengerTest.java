package com.jannik_kuehn.loritimepaper.messenger;

import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.loritimepaper.LoriTimePaper;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.logging.Logger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaperPluginMessengerTest {

    @Test
    void skipsSendWhenPluginIsDisabled() {
        final LoriTimePaper paperPlugin = mock(LoriTimePaper.class);
        final LoriTimePlugin plugin = mock(LoriTimePlugin.class);
        when(plugin.getLoggerFactory()).thenReturn(new LoggerFactory(Logger.getLogger("test")));
        when(paperPlugin.getPlugin()).thenReturn(plugin);
        when(paperPlugin.isEnabled()).thenReturn(false);

        new PaperPluginMessenger(paperPlugin).sendPluginMessage("loritime:storage", UUID.randomUUID());

        verify(paperPlugin, never()).getServer();
    }
}
