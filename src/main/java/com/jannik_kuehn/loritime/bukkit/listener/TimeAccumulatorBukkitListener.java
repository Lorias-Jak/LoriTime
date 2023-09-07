package com.jannik_kuehn.loritime.bukkit.listener;

import com.jannik_kuehn.loritime.common.LoriTimePlugin;
import com.jannik_kuehn.loritime.common.exception.StorageException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class TimeAccumulatorBukkitListener implements Listener {

    private final LoriTimePlugin plugin;

    public TimeAccumulatorBukkitListener(LoriTimePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void playerJoin(PlayerJoinEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        final long now = System.currentTimeMillis();
        plugin.getScheduler().runAsyncOnce(() -> {
           try {
               plugin.getTimeStorage().startAccumulating(uuid, now);
           } catch (StorageException e) {
               plugin.getLogger().warning("could not start accumulating online time for player " + uuid, e);
           }
        });
    }

    @EventHandler
    public void playerQuit(PlayerQuitEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        final long now = System.currentTimeMillis();
        plugin.getScheduler().runAsyncOnce(() -> {
           try {
               plugin.getTimeStorage().stopAccumulatingAndSaveOnlineTime(uuid, now);
           } catch (StorageException e) {
               plugin.getLogger().warning("error while stopping accumulation of online time for player " + uuid, e);
           }
        });
    }
}
