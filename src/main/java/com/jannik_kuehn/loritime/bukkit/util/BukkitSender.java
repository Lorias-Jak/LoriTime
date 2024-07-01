package com.jannik_kuehn.loritime.bukkit.util;

import com.jannik_kuehn.loritime.api.common.CommonSender;
import com.jannik_kuehn.loritime.common.LoriTimePlugin;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class BukkitSender implements CommonSender {

    private LoriTimePlugin plugin;
    private CommandSender source;

    public BukkitSender(LoriTimePlugin plugin, CommandSender source) {
        this.plugin = plugin;
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
