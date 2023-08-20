package com.jannik_kuehn.loritime.common.config.localization;

import com.jannik_kuehn.loritime.common.config.Configuration;
import com.jannik_kuehn.loritime.common.config.YamlConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Localization {
    private static final String PLUGINPREFIX = "<#808080>[<#08A51D>LoriTime<#808080>]<reset> ";
    private final Configuration langFile;
    private final Map<String, String> cachedTranslations;
    private final MiniMessage miniMessage;

    public Localization(Configuration langFile) {
        this.langFile = Objects.requireNonNullElseGet(langFile, () -> new YamlConfiguration("en"));
        cachedTranslations = new HashMap<>();
        miniMessage = MiniMessage.builder().build();

        loadTranslations();
    }

    private void loadTranslations() {
        Map<String, String> dataMap = new HashMap<>();
        for (String key : langFile.getKeys()) {
            dataMap.put(key, (String) langFile.getObject(key));
        }
        cachedTranslations.clear();
        cachedTranslations.putAll(dataMap);
    }

    public void reloadTranslation() {
        langFile.reload();
        loadTranslations();
    }

    public TextComponent formatMiniMessage(String message) {
        return (TextComponent) miniMessage.deserialize(PLUGINPREFIX + message);
    }

    public String getRawMessage(String key) {
        return Objects.requireNonNullElseGet(cachedTranslations.get(key), () -> "Message not found: " + key);
    }

    public Configuration getLangFile() {
        return langFile;
    }
}