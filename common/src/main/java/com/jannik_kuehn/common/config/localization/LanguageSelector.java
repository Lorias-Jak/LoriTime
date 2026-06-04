package com.jannik_kuehn.common.config.localization;

import com.jannik_kuehn.common.api.common.CommonSender;

/**
 * Resolves the language tag used for sender-facing messages.
 */
@FunctionalInterface
public interface LanguageSelector {
    /**
     * Resolves a language tag for a sender.
     *
     * @param sender message recipient
     * @return language tag
     */
    String languageFor(CommonSender sender);
}
