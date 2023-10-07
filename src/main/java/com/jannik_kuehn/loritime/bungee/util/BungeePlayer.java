package com.jannik_kuehn.loritime.bungee.util;

import com.jannik_kuehn.loritime.api.CommonSender;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.UUID;

public class BungeePlayer implements CommonSender {

    private final ProxiedPlayer player;
    private final UUID uuid;

    public BungeePlayer(ProxiedPlayer player) {
        this.player = player;
        this.uuid = player.getUniqueId();
    }


    @Override
    public UUID getUniqueId() {
        return uuid;
    }

    @Override
    public String getName() {
        return player.getName();
    }

    @Override
    public boolean hasPermission(String permission) {
        return player.hasPermission(permission);
    }

    @Override
    public void sendMessage(String message) {
        player.sendMessage(message);
    }

    @Override
    public void sendMessage(TextComponent message) {
        player.sendMessage(BungeeComponentSerializer.get().serialize(message));
    }

    @Override
    public boolean isConsole() {
        return false;
    }

    @Override
    public boolean isOnline() {
        return player.isConnected();
    }
}
