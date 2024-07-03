package com.jannik_kuehn.loritimebungee.util;

import com.jannik_kuehn.common.api.common.CommonSender;
import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.UUID;

public class BungeeSender implements CommonSender {

    private final BungeeAudiences audiences;

    private final CommandSender source;

    public BungeeSender(BungeeAudiences audiences, CommandSender source) {
        this.audiences = audiences;
        this.source = source;
    }

    @Override
    public UUID getUniqueId() {
        return isConsole() ? null : ((ProxiedPlayer) source).getUniqueId();
    }

    @Override
    public String getName() {
        return isConsole() ? "CONSOLE" : ((ProxiedPlayer) source).getName();
    }

    @Override
    public boolean hasPermission(String permission) {
        return source.hasPermission(permission);
    }

    @Override
    public void sendMessage(String message) {
        audiences.sender(source).sendMessage(LegacyComponentSerializer.legacy('&').deserialize(message));
    }

    @Override
    public void sendMessage(TextComponent message) {
        audiences.sender(source).sendMessage(message);
    }

    @Override
    public boolean isConsole() {
        return !(source instanceof ProxiedPlayer);
    }

    @Override
    public boolean isOnline() {
        return isConsole() || ((ProxiedPlayer) source).getServer().isConnected();
    }
}
