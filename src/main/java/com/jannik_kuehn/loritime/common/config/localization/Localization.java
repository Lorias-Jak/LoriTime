/*
 * MIT License
 *
 * Copyright (c) 2019 Niklas Seyfarth
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.jannik_kuehn.loritime.common.config.localization;

import com.jannik_kuehn.loritime.common.config.Configuration;
import com.jannik_kuehn.loritime.common.config.YamlConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Localization {
    private final Configuration langFile;
    private final Map<String, String> cachedTranslations;
    private final MiniMessage miniMessage;

    public Localization(Configuration langFile) {
        // ToDo gegen richtiges Schema austauschen mit "en"

        this.langFile = Objects.requireNonNullElseGet(langFile, () -> new YamlConfiguration("en"));
        cachedTranslations = new HashMap<>();
        miniMessage = MiniMessage.builder().build();

        loadTranslations();
    }

    private void loadTranslations() {
        final Map<String, String> readTranslations = new HashMap<>();
        for (String key : langFile.getKeys()) {
            readTranslations.put(key, (String) langFile.getObject(key));
        }
        cachedTranslations.clear();
        cachedTranslations.putAll(readTranslations);
    }

    public TextComponent formatMiniMessage(String message) {
        return (TextComponent) miniMessage.deserialize(message);
    }

    public String getRawMessage(String key) {
        return Objects.requireNonNullElseGet(cachedTranslations.get(key), () -> "Message not found: " + key);
    }

    public Configuration getLangFile() {
        return langFile;
    }
}