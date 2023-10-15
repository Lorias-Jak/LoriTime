package com.jannik_kuehn.loritime.bukkit.listener;

import com.jannik_kuehn.loritime.api.LoriTimePlayer;
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
        LoriTimePlayer player = new LoriTimePlayer(event.getPlayer().getUniqueId(), event.getPlayer().getName());
        updateAfkStatus(getOrCreatePlayer(event.getPlayer().getUniqueId(), player));
    }

    @EventHandler (ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        LoriTimePlayer player = new LoriTimePlayer(event.getPlayer().getUniqueId(), event.getPlayer().getName());
        plugin.getAfkStatusProvider().playerLeft(getOrCreatePlayer(event.getPlayer().getUniqueId(), player));
        afkPlayers.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler (ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        LoriTimePlayer player = new LoriTimePlayer(event.getPlayer().getUniqueId(), event.getPlayer().getName());
        updateAfkStatus(getOrCreatePlayer(event.getPlayer().getUniqueId(), player));
    }

    @EventHandler (ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        LoriTimePlayer player = new LoriTimePlayer(event.getPlayer().getUniqueId(), event.getPlayer().getName());
        updateAfkStatus(getOrCreatePlayer(event.getPlayer().getUniqueId(), player));
    }

    @EventHandler (ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        LoriTimePlayer player = new LoriTimePlayer(event.getPlayer().getUniqueId(), event.getPlayer().getName());
        updateAfkStatus(getOrCreatePlayer(event.getPlayer().getUniqueId(), player));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerCommandEvent(PlayerCommandPreprocessEvent event) {
        LoriTimePlayer player = new LoriTimePlayer(event.getPlayer().getUniqueId(), event.getPlayer().getName());
        updateAfkStatus(getOrCreatePlayer(event.getPlayer().getUniqueId(), player));
    }

    private void updateAfkStatus(LoriTimePlayer player) {
        plugin.getScheduler().runAsyncOnce(() -> {
            plugin.getAfkStatusProvider().resetTimer(player);
        });
    }

    private LoriTimePlayer getOrCreatePlayer(UUID uuid, LoriTimePlayer player) {
        if (afkPlayers.containsKey(uuid)) {
            return afkPlayers.get(uuid);
        }
        afkPlayers.put(uuid, player);
        return player;
    }
}
