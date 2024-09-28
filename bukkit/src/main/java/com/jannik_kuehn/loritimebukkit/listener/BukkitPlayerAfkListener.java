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

public class BukkitPlayerAfkListener implements Listener {
    private final LoriTimePlugin loriTimePlugin;

    public BukkitPlayerAfkListener(final LoriTimePlugin loriTimePlugin) {
        this.loriTimePlugin = loriTimePlugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final LoriTimePlayer player = loriTimePlugin.getPlayerConverter().getOnlinePlayer(event.getPlayer().getUniqueId());
        updateAfkStatus(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(final PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        final LoriTimePlayer player = loriTimePlugin.getPlayerConverter().getOnlinePlayer(event.getPlayer().getUniqueId());
        updateAfkStatus(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerChat(final AsyncChatEvent event) {
        final LoriTimePlayer player = loriTimePlugin.getPlayerConverter().getOnlinePlayer(event.getPlayer().getUniqueId());
        updateAfkStatus(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(final PlayerInteractEvent event) {
        final LoriTimePlayer player = loriTimePlugin.getPlayerConverter().getOnlinePlayer(event.getPlayer().getUniqueId());
        updateAfkStatus(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerCommand(final PlayerCommandPreprocessEvent event) {
        if (event.getMessage().equalsIgnoreCase("/afk")) {
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
