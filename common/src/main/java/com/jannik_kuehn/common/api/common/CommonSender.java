package com.jannik_kuehn.common.api.common;

import com.jannik_kuehn.common.api.LoriTimePlayer;
import net.kyori.adventure.text.TextComponent;

@SuppressWarnings("PMD.CommentRequired")
public interface CommonSender extends LoriTimePlayer {

    boolean hasPermission(String permission);

    void sendMessage(String message);

    void sendMessage(TextComponent message);

    boolean isConsole();

    boolean isOnline();
}
