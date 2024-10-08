package com.jannik_kuehn.common.api.common;

import net.kyori.adventure.text.TextComponent;

import java.util.UUID;

@SuppressWarnings("PMD.CommentRequired")
public interface CommonSender {

    UUID getUniqueId();

    String getName();

    boolean hasPermission(String permission);

    void sendMessage(String message);

    void sendMessage(TextComponent message);

    boolean isConsole();

    boolean isOnline();
}
