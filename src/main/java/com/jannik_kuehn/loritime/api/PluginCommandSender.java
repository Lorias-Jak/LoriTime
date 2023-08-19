package com.jannik_kuehn.loritime.api;


import net.kyori.adventure.text.TextComponent;

import java.util.Optional;

public interface PluginCommandSender {

    boolean hasPermission(String permissionNode);

    void sendMessage(TextComponent message);

    /**
     * Check whether this sender is a player or not. Equal to {@code asPlayer.isPresent()}, but possibly faster.
     *
     * @return true if this sender is a player
     */
    boolean isPlayer();

    /**
     * Interpret the sender as player. If this sender is not a player, the result is empty.
     *
     * @return the represented player if any
     */
    Optional<PlayerData> asPlayer();
}
