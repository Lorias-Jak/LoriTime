package com.jannik_kuehn.loritime.common.config.localization;

import com.jannik_kuehn.loritime.common.config.Configuration;
import com.jannik_kuehn.loritime.common.config.YamlConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Localization {
    private static final String PLUGINPREFIX = "<#808080>[<#08A51D>LoriTime<#808080>]<reset> ";
    private final Configuration langFile;
    private final MiniMessage miniMessage;

    public Localization(Configuration langFile) {
        this.langFile = langFile;
        this.miniMessage = MiniMessage.builder().build();
    }

    public void reloadTranslation() {
        langFile.reload();
    }

    public TextComponent formatMiniMessage(String message) {
        return (TextComponent) miniMessage.deserialize(PLUGINPREFIX + message);
    }

    public TextComponent formatMiniMessageWithoutPrefix(String message) {
        return (TextComponent) miniMessage.deserialize(message);
    }

    public String getRawMessage(String key) {
        return Objects.requireNonNullElseGet(langFile.getString(key), () -> "Message not found: " + key);
    }

    public ArrayList<?> getLangArray(String key) {
        return langFile.getArrayList(key);
    }

    public Configuration getLangFile() {
        return langFile;
    }
}