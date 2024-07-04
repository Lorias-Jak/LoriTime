package com.jannik_kuehn.loritimebukkit.listener;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.LoriTimePlayer;
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

    public BukkitPlayerAfkListener(final LoriTimePlugin plugin) {
        this.plugin = plugin;
        this.afkPlayers = new ConcurrentHashMap<>();
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final LoriTimePlayer player = new LoriTimePlayer(event.getPlayer().getUniqueId(), event.getPlayer().getName());
        updateAfkStatus(getOrCreatePlayer(event.getPlayer().getUniqueId(), player));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerQuit(final PlayerQuitEvent event) {
        final LoriTimePlayer player = new LoriTimePlayer(event.getPlayer().getUniqueId(), event.getPlayer().getName());
        plugin.getAfkStatusProvider().playerLeft(getOrCreatePlayer(event.getPlayer().getUniqueId(), player));
        afkPlayers.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(final PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        final LoriTimePlayer player = new LoriTimePlayer(event.getPlayer().getUniqueId(), event.getPlayer().getName());
        updateAfkStatus(getOrCreatePlayer(event.getPlayer().getUniqueId(), player));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerChat(final AsyncPlayerChatEvent event) {
        final LoriTimePlayer player = new LoriTimePlayer(event.getPlayer().getUniqueId(), event.getPlayer().getName());
        updateAfkStatus(getOrCreatePlayer(event.getPlayer().getUniqueId(), player));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(final PlayerInteractEvent event) {
        final LoriTimePlayer player = new LoriTimePlayer(event.getPlayer().getUniqueId(), event.getPlayer().getName());
        updateAfkStatus(getOrCreatePlayer(event.getPlayer().getUniqueId(), player));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerCommand(final PlayerCommandPreprocessEvent event) {
        if (event.getMessage().equalsIgnoreCase("/afk")) {
            return;
        }
        final LoriTimePlayer player = new LoriTimePlayer(event.getPlayer().getUniqueId(), event.getPlayer().getName());
        updateAfkStatus(getOrCreatePlayer(event.getPlayer().getUniqueId(), player));
    }

    private void updateAfkStatus(final LoriTimePlayer player) {
        plugin.getScheduler().runAsyncOnce(() -> {
            plugin.getAfkStatusProvider().resetTimer(player);
        });
    }

    private LoriTimePlayer getOrCreatePlayer(final UUID uuid, final LoriTimePlayer player) {
        if (afkPlayers.containsKey(uuid)) {
            return afkPlayers.get(uuid);
        }
        afkPlayers.put(uuid, player);
        return player;
    }
}
