package com.jannik_kuehn.loritimebungee.listener;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.logger.LoriTimeLogger;
import com.jannik_kuehn.common.exception.StorageException;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.UUID;

public class TimeAccumulatorBungeeListener implements Listener {

    private final LoriTimePlugin plugin;

    private final LoriTimeLogger log;

    public TimeAccumulatorBungeeListener(final LoriTimePlugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLoggerFactory().create(TimeAccumulatorBungeeListener.class);
    }

    @EventHandler
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

    @EventHandler
    public void onDisconnect(final PlayerDisconnectEvent event) {
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
