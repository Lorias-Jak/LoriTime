package com.jannik_kuehn.loritimevelocity.util;

import com.jannik_kuehn.common.platform.CommonPlayerSender;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.UUID;

/**
 * Velocity player adapter for shared LoriTime code.
 */
public class VelocityPlayer implements CommonPlayerSender {
    /**
     * The Velocity player.
     */
    private final Player player;

    /**
     * The player's UUID.
     */
    private final UUID uuid;

    /**
     * Creates a player adapter.
     *
     * @param player Velocity player
     */
    public VelocityPlayer(final Player player) {
        this.player = player;
        this.uuid = player.getUniqueId();
    }

    /**
     * Returns the player's UUID.
     *
     * @return player UUID
     */
    @Override
    public UUID getUniqueId() {
        return uuid;
    }

    /**
     * Returns the player's current username.
     *
     * @return player name
     */
    @Override
    public String getName() {
        return player.getUsername();
    }

    /**
     * Checks a Velocity player permission.
     *
     * @param permission permission node
     * @return true when the player has the permission
     */
    @Override
    public boolean hasPermission(final String permission) {
        return player.hasPermission(permission);
    }

    /**
     * Sends a legacy-formatted text message.
     *
     * @param message plain text message
     */
    @Override
    public void sendMessage(final String message) {
        LegacyComponentSerializer.legacy('&').deserialize(message);
    }

    /**
     * Sends an Adventure component message.
     *
     * @param message component message
     */
    @Override
    public void sendMessage(final Component message) {
        player.sendMessage(message);
    }

    /**
     * Returns whether Velocity reports the player in online mode.
     *
     * @return true when the player is in online mode
     */
    @Override
    public boolean isOnline() {
        return player.isOnlineMode();
    }
}
