package com.jannik_kuehn.loritimevelocity.listener;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.logger.LoriTimeLogger;
import com.jannik_kuehn.common.exception.StorageException;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;

import java.util.UUID;

public class TimeAccumulatorVelocityListener {
    private final LoriTimePlugin plugin;

    private final LoriTimeLogger log;

    public TimeAccumulatorVelocityListener(final LoriTimePlugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLoggerFactory().create(TimeAccumulatorVelocityListener.class);
    }

    @Subscribe
    public void onPostLogin(final PostLoginEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        final long now = System.currentTimeMillis();
        plugin.getScheduler().runAsyncOnce(() -> {
            try {
                plugin.getTimeStorage().startAccumulating(uuid, now);
            } catch (final StorageException ex) {
                log.warn("could not start accumulating online time for player " + uuid, ex);
            }
        });
    }

    @Subscribe
    public void onDisconnect(final DisconnectEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        final long now = System.currentTimeMillis();
        plugin.getScheduler().runAsyncOnce(() -> {
            try {
                plugin.getTimeStorage().stopAccumulatingAndSaveOnlineTime(uuid, now);
            } catch (final StorageException ex) {
                log.warn("error while stopping accumulation of online time for player " + uuid, ex);
            }
        });
    }
}
