package com.jannik_kuehn.common.command.core;

import com.jannik_kuehn.common.config.localization.LanguageSelector;
import com.jannik_kuehn.common.config.localization.Localization;
import com.jannik_kuehn.common.platform.CommonSender;

/**
 * Shared localized command response helpers.
 */
public final class CommandMessages {

    private CommandMessages() {
    }

    /**
     * Sends a localized message by key.
     *
     * @param localization localization source
     * @param sender       command sender
     * @param messageKey   localization key
     */
    public static void send(final Localization localization, final CommonSender sender, final String messageKey) {
        sender.sendMessage(localization.formatTextComponent(localization.getRawMessage(messageKey)));
    }

    /**
     * Sends a localized message by key using sender language selection.
     *
     * @param localization     localization source
     * @param languageSelector language selector
     * @param sender           command sender
     * @param messageKey       localization key
     */
    public static void send(final Localization localization, final LanguageSelector languageSelector,
                            final CommonSender sender, final String messageKey) {
        if (languageSelector == null) {
            send(localization, sender, messageKey);
            return;
        }
        sender.sendMessage(localization.getPrefixedMessage(languageSelector.languageFor(sender), messageKey));
    }
}
