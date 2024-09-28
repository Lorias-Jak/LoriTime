package com.jannik_kuehn.loritimevelocity.util;

import com.jannik_kuehn.common.api.common.CommonSender;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.UUID;

@SuppressWarnings("PMD.CommentRequired")
public class VelocitySender implements CommonSender {
    private final CommandSource source;

    public VelocitySender(final CommandSource source) {
        this.source = source;
    }

    @Override
    public UUID getUniqueId() {
        return isConsole() ? null : ((Player) source).getUniqueId();
    }

    @Override
    public String getName() {
        return isConsole() ? "CONSOLE" : ((Player) source).getUsername();
    }

    @Override
    public boolean hasPermission(final String permission) {
        return source.hasPermission(permission);
    }

    @Override
    public void sendMessage(final String message) {
        source.sendMessage(LegacyComponentSerializer.legacy('&').deserialize(message));
    }

    @Override
    public void sendMessage(final TextComponent message) {
        source.sendMessage(message);
    }

    @Override
    public boolean isConsole() {
        return !(source instanceof Player);
    }

    @Override
    public boolean isOnline() {
        return isConsole() || ((Player) source).getCurrentServer().isPresent();
    }
}
