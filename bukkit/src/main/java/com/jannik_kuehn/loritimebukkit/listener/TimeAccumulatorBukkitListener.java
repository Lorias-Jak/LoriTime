package com.jannik_kuehn.loritimebukkit.listener;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.exception.StorageException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class TimeAccumulatorBukkitListener implements Listener {

    private final LoriTimePlugin plugin;

    public TimeAccumulatorBukkitListener(final LoriTimePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void playerJoin(final PlayerJoinEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        final long now = System.currentTimeMillis();
        plugin.getScheduler().runAsyncOnce(() -> {
            try {
                plugin.getTimeStorage().startAccumulating(uuid, now);
            } catch (final StorageException e) {
                plugin.getLogger().warning("could not start accumulating online time for player " + uuid, e);
            }
        });
    }

    @EventHandler
    public void playerQuit(final PlayerQuitEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        final long now = System.currentTimeMillis();
        plugin.getScheduler().runAsyncOnce(() -> {
            try {
                plugin.getTimeStorage().stopAccumulatingAndSaveOnlineTime(uuid, now);
            } catch (final StorageException e) {
                plugin.getLogger().warning("error while stopping accumulation of online time for player " + uuid, e);
            }
        });
    }
}
