package com.jannik_kuehn.common.config.localization;

import com.jannik_kuehn.common.config.Configuration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.List;
import java.util.Objects;

/**
 * Provides localized MiniMessage text lookup and formatting.
 */
public class Localization {
    /**
     * Prefix added to normal localized messages.
     */
    private static final String PLUGIN_PREFIX = "<#808080>[<#08A51D>LoriTime<#808080>]<reset> ";

    /**
     * Backing language configuration.
     */
    private final Configuration langFile;

    /**
     * MiniMessage parser.
     */
    private final MiniMessage miniMessage;

    /**
     * Creates a localization wrapper.
     *
     * @param langFile language configuration
     */
    public Localization(final Configuration langFile) {
        this.langFile = langFile;
        this.miniMessage = MiniMessage.builder().build();
    }

    /**
     * Reloads translation values from disk.
     */
    public void reloadTranslation() {
        langFile.reload();
    }

    /**
     * Formats a localized message with the plugin prefix.
     *
     * @param message MiniMessage input
     * @return formatted text component
     */
    public TextComponent formatTextComponent(final String message) {
        return (TextComponent) miniMessage.deserialize(PLUGIN_PREFIX + message);
    }

    /**
     * Formats a localized message without the plugin prefix.
     *
     * @param message MiniMessage input
     * @return formatted text component
     */
    public TextComponent formatTextComponentWithoutPrefix(final String message) {
        return (TextComponent) miniMessage.deserialize(message);
    }

    /**
     * Reads a raw localized message.
     *
     * @param key localization key
     * @return configured message, or a fallback marker
     */
    public String getRawMessage(final String key) {
        return Objects.requireNonNullElseGet(langFile.getString(key), () -> "Message not found: " + key);
    }

    /**
     * Reads a localized list.
     *
     * @param key localization key
     * @return configured list
     */
    public List<?> getLangArray(final String key) {
        return langFile.getArrayList(key);
    }

    /**
     * Returns the backing language configuration.
     *
     * @return language configuration
     */
    public Configuration getLangFile() {
        return langFile;
    }
}
