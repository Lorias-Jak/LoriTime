package com.jannik_kuehn.loritime.bukkit.listener;

import com.jannik_kuehn.loritime.api.LoriTimePlayer;
import com.jannik_kuehn.loritime.bukkit.util.BukkitPlayer;
import com.jannik_kuehn.loritime.common.LoriTimePlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BukkitPlayerAfkListener implements Listener {
    private final LoriTimePlugin plugin;
    private final ConcurrentHashMap<UUID, LoriTimePlayer> afkPlayers;

    public BukkitPlayerAfkListener(LoriTimePlugin plugin) {
        this.plugin = plugin;
        this.afkPlayers = new ConcurrentHashMap<>();
    }

    @EventHandler (ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        LoriTimePlayer player = new LoriTimePlayer(new BukkitPlayer(event.getPlayer()));
        updateAfkStatus(getOrCreatePlayer(event.getPlayer().getUniqueId(), player));
    }

    @EventHandler (ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        LoriTimePlayer player = new LoriTimePlayer(new BukkitPlayer(event.getPlayer()));
        plugin.getAfkStatusProvider().playerLeft(getOrCreatePlayer(event.getPlayer().getUniqueId(), player));
        // ToDo umbauen, so dass der PlayerHandler das übernimmt!
        afkPlayers.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler (ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        LoriTimePlayer player = new LoriTimePlayer(new BukkitPlayer(event.getPlayer()));
        updateAfkStatus(player);
    }

    @EventHandler (ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        LoriTimePlayer player = new LoriTimePlayer(new BukkitPlayer(event.getPlayer()));
        updateAfkStatus(player);
    }

    @EventHandler (ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        LoriTimePlayer player = new LoriTimePlayer(new BukkitPlayer(event.getPlayer()));
        updateAfkStatus(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (event.getMessage().equalsIgnoreCase("/afk")) {
            return;
        }
        LoriTimePlayer player = new LoriTimePlayer(new BukkitPlayer(event.getPlayer()));
        updateAfkStatus(player);
    }

    private void updateAfkStatus(LoriTimePlayer player) {
        plugin.getScheduler().runAsyncOnce(() -> {
            if (!plugin.getPlayerHandler().contains(player)) {
                plugin.getPlayerHandler().registerPlayer(player);
            }
            plugin.getAfkStatusProvider().resetTimer(player);
        });
    }
}
