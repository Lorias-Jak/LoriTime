package com.jannik_kuehn.common.api.common;

import net.kyori.adventure.text.Component;

@SuppressWarnings("PMD.CommentRequired")
public interface CommonSender {

    String getName();

    boolean hasPermission(String permission);

    void sendMessage(String message);

    void sendMessage(Component message);
}
