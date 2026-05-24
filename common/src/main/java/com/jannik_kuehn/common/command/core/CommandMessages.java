package com.jannik_kuehn.common.command.core;

import com.jannik_kuehn.common.api.common.CommonSender;
import com.jannik_kuehn.common.config.localization.Localization;

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
     * @param sender command sender
     * @param messageKey localization key
     */
    public static void send(final Localization localization, final CommonSender sender, final String messageKey) {
        sender.sendMessage(localization.formatTextComponent(localization.getRawMessage(messageKey)));
    }
}
