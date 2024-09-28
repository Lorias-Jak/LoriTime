package com.jannik_kuehn.loritimebukkit.listener;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.LoriTimePlayer;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Listener for player actions to update the AFK status of the player.
 */
public class BukkitPlayerAfkListener implements Listener {
    /**
     * The {@link LoriTimePlugin} instance.
     */
    private final LoriTimePlugin loriTimePlugin;

    /**
     * Creates a new instance of the {@link BukkitPlayerAfkListener}.
     *
     * @param loriTimePlugin The {@link LoriTimePlugin} instance.
     */
    public BukkitPlayerAfkListener(final LoriTimePlugin loriTimePlugin) {
        this.loriTimePlugin = loriTimePlugin;
    }

    /**
     * Handles the {@link PlayerJoinEvent} to update the AFK status of the player.
     *
     * @param event The {@link PlayerJoinEvent}.
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final LoriTimePlayer player = loriTimePlugin.getPlayerConverter().getOnlinePlayer(event.getPlayer().getUniqueId());
        updateAfkStatus(player);
    }

    /**
     * Handles the {@link PlayerMoveEvent} to update the AFK status of the player.
     *
     * @param event The {@link PlayerMoveEvent}.
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(final PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        final LoriTimePlayer player = loriTimePlugin.getPlayerConverter().getOnlinePlayer(event.getPlayer().getUniqueId());
        updateAfkStatus(player);
    }

    /**
     * Handles the {@link AsyncChatEvent} to update the AFK status of the player.
     *
     * @param event The {@link AsyncChatEvent}.
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerChat(final AsyncChatEvent event) {
        final LoriTimePlayer player = loriTimePlugin.getPlayerConverter().getOnlinePlayer(event.getPlayer().getUniqueId());
        updateAfkStatus(player);
    }

    /**
     * Handles the {@link PlayerInteractEvent} to update the AFK status of the player.
     *
     * @param event The {@link PlayerInteractEvent}.
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(final PlayerInteractEvent event) {
        final LoriTimePlayer player = loriTimePlugin.getPlayerConverter().getOnlinePlayer(event.getPlayer().getUniqueId());
        updateAfkStatus(player);
    }

    /**
     * Handles the {@link PlayerCommandPreprocessEvent} to update the AFK status of the player.
     *
     * @param event The {@link PlayerCommandPreprocessEvent}.
     */
    @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
    @EventHandler(ignoreCancelled = true)
    public void onPlayerCommand(final PlayerCommandPreprocessEvent event) {
        if ("/afk".equalsIgnoreCase(event.getMessage())) {
            return;
        }
        final LoriTimePlayer player = loriTimePlugin.getPlayerConverter().getOnlinePlayer(event.getPlayer().getUniqueId());
        updateAfkStatus(player);
    }

    private void updateAfkStatus(final LoriTimePlayer player) {
        loriTimePlugin.getScheduler().runAsyncOnce(() -> {
            loriTimePlugin.getAfkStatusProvider().resetTimer(player);
        });
    }
}
