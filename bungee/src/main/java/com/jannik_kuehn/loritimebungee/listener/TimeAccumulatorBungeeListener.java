package com.jannik_kuehn.loritimebungee.listener;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.exception.StorageException;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.UUID;

public class TimeAccumulatorBungeeListener implements Listener {

    private final LoriTimePlugin plugin;

    public TimeAccumulatorBungeeListener(LoriTimePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        final long now = System.currentTimeMillis();
        plugin.getScheduler().runAsyncOnce(() -> {
            try {
                plugin.getTimeStorage().startAccumulating(uuid, now);
            } catch (StorageException ex) {
                plugin.getLogger().warning("could not start accumulating online time for player " + uuid, ex);
            }
        });
    }

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        final long now = System.currentTimeMillis();
        plugin.getScheduler().runAsyncOnce(() -> {
            try {
                plugin.getTimeStorage().stopAccumulatingAndSaveOnlineTime(uuid, now);
            } catch (StorageException ex) {
                plugin.getLogger().warning("error while stopping accumulation of online time for player " + uuid, ex);
            }
        });
    }
}
