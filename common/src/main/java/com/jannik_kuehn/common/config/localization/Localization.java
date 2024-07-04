package com.jannik_kuehn.common.config.localization;

import com.jannik_kuehn.common.config.Configuration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.Objects;

public class Localization {
    private static final String PLUGIN_PREFIX = "<#808080>[<#08A51D>LoriTime<#808080>]<reset> ";

    private final Configuration langFile;

    private final MiniMessage miniMessage;

    public Localization(final Configuration langFile) {
        this.langFile = langFile;
        this.miniMessage = MiniMessage.builder().build();
    }

    public void reloadTranslation() {
        langFile.reload();
    }

    public TextComponent formatTextComponent(final String message) {
        return (TextComponent) miniMessage.deserialize(PLUGIN_PREFIX + message);
    }

    public TextComponent formatTextComponentWithoutPrefix(final String message) {
        return (TextComponent) miniMessage.deserialize(message);
    }

    public String getRawMessage(final String key) {
        return Objects.requireNonNullElseGet(langFile.getString(key), () -> "Message not found: " + key);
    }

    public ArrayList<?> getLangArray(final String key) {
        return langFile.getArrayList(key);
    }

    public Configuration getLangFile() {
        return langFile;
    }
}
