package com.jannik_kuehn.common.api.common;

import net.kyori.adventure.text.TextComponent;

@SuppressWarnings("PMD.CommentRequired")
public interface CommonSender {

    String getName();

    boolean hasPermission(String permission);

    void sendMessage(String message);

    void sendMessage(TextComponent message);
}
