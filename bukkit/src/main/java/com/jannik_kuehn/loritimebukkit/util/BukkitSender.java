package com.jannik_kuehn.loritimebukkit.util;

import com.jannik_kuehn.common.api.common.CommonSender;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class BukkitSender implements CommonSender {
    private CommandSender source;

    public BukkitSender(CommandSender source) {
        this.source = source;
    }

    @Override
    public UUID getUniqueId() {
        return isConsole() ? null : ((Player) source).getUniqueId();
    }

    @Override
    public String getName() {
        return isConsole() ? "CONSOLE" : source.getName();
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
        return isConsole() || ((Player) source).isOnline();
    }
}
