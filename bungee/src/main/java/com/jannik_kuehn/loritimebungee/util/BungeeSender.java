package com.jannik_kuehn.loritimebungee.util;

import com.jannik_kuehn.common.api.common.CommonConsoleSender;
import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.CommandSender;

@SuppressWarnings("PMD.CommentRequired")
public class BungeeSender implements CommonConsoleSender {

    private final BungeeAudiences audiences;

    private final CommandSender source;

    public BungeeSender(final BungeeAudiences audiences, final CommandSender source) {
        this.audiences = audiences;
        this.source = source;
    }

    @Override
    public String getName() {
        return "CONSOLE";
    }

    @Override
    public boolean hasPermission(final String permission) {
        return source.hasPermission(permission);
    }

    @Override
    public void sendMessage(final String message) {
        audiences.sender(source).sendMessage(LegacyComponentSerializer.legacy('&').deserialize(message));
    }

    @Override
    public void sendMessage(final TextComponent message) {
        audiences.sender(source).sendMessage(message);
    }

}
