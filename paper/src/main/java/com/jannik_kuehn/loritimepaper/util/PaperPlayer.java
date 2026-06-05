package com.jannik_kuehn.loritimepaper.util;

import com.jannik_kuehn.common.platform.CommonPlayerSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Paper player adapter for shared LoriTime code.
 */
public class PaperPlayer implements CommonPlayerSender {

    /**
     * Bukkit player.
     */
    private final Player player;

    /**
     * Player UUID.
     */
    private final UUID uuid;

    /**
     * Creates a player adapter.
     *
     * @param player Bukkit player
     */
    public PaperPlayer(final Player player) {
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
     * Returns the player's current name.
     *
     * @return player name
     */
    @Override
    public String getName() {
        return player.getName();
    }

    /**
     * Checks a Bukkit player permission.
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
     * Returns whether the player is online.
     *
     * @return true when the player is online
     */
    @Override
    public boolean isOnline() {
        return player.isOnline();
    }
}
