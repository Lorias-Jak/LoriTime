package com.jannik_kuehn.common.config.localization;

import java.util.Locale;

/**
 * Normalization helpers for localization tags.
 */
final class LocalizationTags {
    /**
     * Bundled fallback language tag.
     */
    /* default */ static final String HARD_FALLBACK_TAG = "en-us";

    private LocalizationTags() {
    }

    /* default */
    static String normalize(final String rawTag) {
        if (rawTag == null || rawTag.isBlank()) {
            return HARD_FALLBACK_TAG;
        }
        return rawTag.trim().replace('_', '-').toLowerCase(Locale.ROOT);
    }

    /* default */
    static String languageOnly(final String tag) {
        if (tag == null || tag.isBlank()) {
            return null;
        }
        final int separator = tag.indexOf('-');
        if (separator <= 0) {
            return tag;
        }
        return tag.substring(0, separator);
    }
}
