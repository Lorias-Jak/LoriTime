package com.jannik_kuehn.loritimevelocity.util;

import com.jannik_kuehn.common.api.common.CommonPlayerSender;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.UUID;

@SuppressWarnings("PMD.CommentRequired")
public class VelocityPlayer implements CommonPlayerSender {
    private final Player player;

    private final UUID uuid;

    public VelocityPlayer(final Player player) {
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
    public boolean hasPermission(final String permission) {
        return player.hasPermission(permission);
    }

    @Override
    public void sendMessage(final String message) {
        LegacyComponentSerializer.legacy('&').deserialize(message);
    }

    @Override
    public void sendMessage(final Component message) {
        player.sendMessage(message);
    }

    @Override
    public boolean isOnline() {
        return player.isOnlineMode();
    }
}
