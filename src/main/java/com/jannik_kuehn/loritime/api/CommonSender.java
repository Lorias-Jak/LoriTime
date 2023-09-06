package com.jannik_kuehn.loritime.api;

import net.kyori.adventure.text.TextComponent;

import java.util.UUID;

public interface CommonSender {

    UUID getUniqueId();

    String getName();

    boolean hasPermission(String permission);

    void sendMessage(String message);

    void sendMessage(TextComponent message);

    boolean isConsole();

    boolean isOnline();
}
