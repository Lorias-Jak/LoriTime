package com.jannik_kuehn.loritimebungee.listener;

import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.storage.SessionContextDefaults;
import com.jannik_kuehn.common.api.storage.TimeEntryReason;
import com.jannik_kuehn.common.exception.StorageException;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.UUID;

/**
 * Listener for accumulating online time of players.
 */
public class TimeAccumulatorBungeeListener implements Listener {
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
    public TimeAccumulatorBungeeListener(final LoriTimePlugin loriTimePlugin) {
        this.loriTimePlugin = loriTimePlugin;
        this.log = loriTimePlugin.getLoggerFactory().create(TimeAccumulatorBungeeListener.class);
    }

    /**
     * Switches the accumulating context when a player changes backend servers.
     *
     * @param event The {@link ServerConnectedEvent} event.
     */
    @EventHandler
    public void onServerConnected(final ServerConnectedEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        final String name = event.getPlayer().getName();
        final String server = event.getServer().getInfo().getName();
        final long now = System.currentTimeMillis();
        loriTimePlugin.rememberPlayerName(uuid, name);
        loriTimePlugin.rememberScope(server, SessionContextDefaults.WORLD);
        loriTimePlugin.getScheduler().runAsyncOnce(() -> {
            try {
                loriTimePlugin.getAccumulator().switchContext(uuid, name, server, SessionContextDefaults.WORLD, now);
            } catch (final StorageException ex) {
                log.warn("could not switch accumulating context for player " + uuid, ex);
            }
        });
    }

    /**
     * Stops accumulating online time when a player leaves the server.
     *
     * @param event The {@link PlayerDisconnectEvent} event.
     */
    @EventHandler
    public void onDisconnect(final PlayerDisconnectEvent event) {
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
