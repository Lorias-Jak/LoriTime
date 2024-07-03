package com.jannik_kuehn.loritimevelocity.util;

import com.jannik_kuehn.common.api.common.CommonSender;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.UUID;

public class VelocityPlayer implements CommonSender {
    private final Player player;

    private final UUID uuid;

    public VelocityPlayer(Player player) {
        this.player = player;
        this.uuid = player.getUniqueId();
    }

    @Override
    public UUID getUniqueId() {
        return uuid;
    }

    @Override
    public String getName() {
        return player.getUsername();
    }

    @Override
    public boolean hasPermission(String permission) {
        return player.hasPermission(permission);
    }

    @Override
    public void sendMessage(String message) {
        LegacyComponentSerializer.legacy('&').deserialize(message);
    }

    @Override
    public void sendMessage(TextComponent message) {
        player.sendMessage(message);
    }

    @Override
    public boolean isConsole() {
        return false;
    }

    @Override
    public boolean isOnline() {
        return player.isOnlineMode();
    }
}
