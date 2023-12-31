package com.jannik_kuehn.loritime.velocity.listener;

import com.jannik_kuehn.loritime.common.LoriTimePlugin;
import com.jannik_kuehn.loritime.common.exception.StorageException;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;

import java.util.UUID;

public class TimeAccumulatorVelocityListener {
    private final LoriTimePlugin plugin;

    public TimeAccumulatorVelocityListener(LoriTimePlugin plugin) {
        this.plugin = plugin;
    }

    @Subscribe
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

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
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
