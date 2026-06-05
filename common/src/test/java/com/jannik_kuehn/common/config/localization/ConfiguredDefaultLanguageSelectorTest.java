package com.jannik_kuehn.common.config.localization;

import com.jannik_kuehn.common.command.core.CommandMessages;
import com.jannik_kuehn.common.platform.CommonPlayerSender;
import com.jannik_kuehn.common.platform.CommonSender;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("PMD.UnitTestAssertionsShouldIncludeMessage")
class ConfiguredDefaultLanguageSelectorTest {
    @Test
    void playerAndConsoleUseConfiguredDefaultLanguage() {
        final LanguageSelector selector = new ConfiguredDefaultLanguageSelector("de-de");
        final CommonSender console = mock(CommonSender.class);
        final CommonPlayerSender player = mock(CommonPlayerSender.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        assertEquals("de-de", selector.languageFor(console));
        assertEquals("de-de", selector.languageFor(player));
    }

    @Test
    void commandMessagesUseSelectedLanguage() {
        final Localization localization = mock(Localization.class);
        final LanguageSelector selector = new ConfiguredDefaultLanguageSelector("de-de");
        final CommonSender sender = mock(CommonSender.class);
        when(localization.getPrefixedMessage("de-de", "message.noPermission")).thenReturn(Component.text("localized"));

        CommandMessages.send(localization, selector, sender, "message.noPermission");

        verify(sender).sendMessage(Component.text("localized"));
    }
}
