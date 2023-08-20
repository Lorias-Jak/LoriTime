package com.jannik_kuehn.loritime.velocity.util;

import com.jannik_kuehn.loritime.common.LoriTimePlugin;
import com.jannik_kuehn.loritime.common.utils.CommonSender;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.UUID;

public class VelocitySender implements CommonSender {

    private LoriTimePlugin plugin;
    private CommandSource source;

    public VelocitySender(LoriTimePlugin plugin, CommandSource source) {
        this.plugin = plugin;
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
    public boolean hasPermission(String permission) {
        return source.hasPermission(permission);
    }

    @Override
    public void sendMessage(String message) {
        source.sendMessage(LegacyComponentSerializer.legacy('&').deserialize(message));
    }

    @Override
    public void sendMessage(TextComponent message) {
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
