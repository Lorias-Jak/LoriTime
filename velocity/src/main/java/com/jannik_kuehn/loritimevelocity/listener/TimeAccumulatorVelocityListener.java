package com.jannik_kuehn.loritimevelocity.listener;

import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.storage.TimeEntryReason;
import com.jannik_kuehn.common.exception.StorageException;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;

import java.util.UUID;

/**
 * Listener for accumulating online time of players.
 */
public class TimeAccumulatorVelocityListener {
    /**
     * The {@link LoriTimePlugin} instance.
     */
    private final LoriTimePlugin loriTimePlugin;

    /**
     * The {@link WrappedLogger} instance.
     */
    private final WrappedLogger log;

    /**
     * The default constructor.
     *
     * @param loriTimePlugin The {@link LoriTimePlugin} instance.
     */
    public TimeAccumulatorVelocityListener(final LoriTimePlugin loriTimePlugin) {
        this.loriTimePlugin = loriTimePlugin;
        this.log = loriTimePlugin.getLoggerFactory().create(TimeAccumulatorVelocityListener.class);
    }

    /**
     * Switches the accumulating context when a player changes backend servers.
     *
     * @param event The {@link ServerConnectedEvent} event.
     */
    @Subscribe
    public void onServerConnected(final ServerConnectedEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        final String name = event.getPlayer().getUsername();
        final String server = event.getServer().getServerInfo().getName();
        final long now = System.currentTimeMillis();
        loriTimePlugin.getScheduler().runAsyncOnce(() -> {
            try {
                loriTimePlugin.getAccumulator().switchContext(uuid, name, server, "global", now);
            } catch (final StorageException ex) {
                log.warn("could not switch accumulating context for player " + uuid, ex);
            }
        });
    }

    /**
     * Stops accumulating online time when a player leaves the server.
     *
     * @param event The {@link DisconnectEvent} event.
     */
    @Subscribe
    public void onDisconnect(final DisconnectEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        final long now = System.currentTimeMillis();
        loriTimePlugin.getScheduler().runAsyncOnce(() -> {
            try {
                final TimeEntryReason reason = loriTimePlugin.consumeAfkKick(uuid)
                        ? TimeEntryReason.PLAYER_AFK_KICK : TimeEntryReason.PLAYER_LEAVE;
                loriTimePlugin.getAccumulator().stopAccumulatingAndSaveOnlineTime(uuid, now, reason);
                loriTimePlugin.getPlayerConverter().removePlayerFromCache(uuid);
            } catch (final StorageException ex) {
                log.warn("error while stopping accumulation of online time for player " + uuid, ex);
            }
        });
    }
}
