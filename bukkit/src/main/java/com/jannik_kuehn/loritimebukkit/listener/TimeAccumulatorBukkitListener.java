package com.jannik_kuehn.loritimebukkit.listener;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.logger.LoriTimeLogger;
import com.jannik_kuehn.common.exception.StorageException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * Listener for accumulating online time of players.
 */
public class TimeAccumulatorBukkitListener implements Listener {
    /**
     * The {@link LoriTimePlugin} instance.
     */
    private final LoriTimePlugin loriTimePlugin;

    /**
     * The {@link LoriTimeLogger} instance.
     */
    private final LoriTimeLogger log;

    /**
     * The default constructor.
     *
     * @param loriTimePlugin The {@link LoriTimePlugin} instance.
     */
    public TimeAccumulatorBukkitListener(final LoriTimePlugin loriTimePlugin) {
        this.loriTimePlugin = loriTimePlugin;
        this.log = loriTimePlugin.getLoggerFactory().create(TimeAccumulatorBukkitListener.class);
    }

    /**
     * Starts accumulating online time when a player joins the server.
     *
     * @param event The {@link PlayerJoinEvent} event.
     */
    @EventHandler
    public void playerJoin(final PlayerJoinEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        final long now = System.currentTimeMillis();
        loriTimePlugin.getScheduler().runAsyncOnce(() -> {
            try {
                loriTimePlugin.getTimeStorage().startAccumulating(uuid, now);
            } catch (final StorageException e) {
                log.warn("could not start accumulating online time for player " + uuid, e);
            }
        });
    }

    /**
     * Stops accumulating online time when a player leaves the server.
     *
     * @param event The {@link PlayerQuitEvent} event.
     */
    @EventHandler
    public void playerQuit(final PlayerQuitEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        final long now = System.currentTimeMillis();
        loriTimePlugin.getScheduler().runAsyncOnce(() -> {
            try {
                loriTimePlugin.getTimeStorage().stopAccumulatingAndSaveOnlineTime(uuid, now);
                loriTimePlugin.getPlayerConverter().removePlayerFromCache(uuid);
            } catch (final StorageException e) {
                log.warn("error while stopping accumulation of online time for player " + uuid, e);
            }
        });
    }
}
