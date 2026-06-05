package com.jannik_kuehn.common.config.localization;

import com.jannik_kuehn.common.platform.CommonSender;

import java.util.Objects;

/**
 * Language selector that uses the configured default for every sender.
 */
public final class ConfiguredDefaultLanguageSelector implements LanguageSelector {
    /**
     * Configured default language tag.
     */
    private final String configuredDefaultLanguage;

    /**
     * Creates a selector.
     *
     * @param configuredDefaultLanguage configured default language
     */
    public ConfiguredDefaultLanguageSelector(final String configuredDefaultLanguage) {
        this.configuredDefaultLanguage = LocalizationTags.normalize(
                Objects.requireNonNullElse(configuredDefaultLanguage, LocalizationTags.HARD_FALLBACK_TAG));
    }

    @Override
    public String languageFor(final CommonSender sender) {
        return configuredDefaultLanguage;
    }
}
