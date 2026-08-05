package com.jannik_kuehn.loritimepaper.listener;

import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.exception.StorageException;
import com.jannik_kuehn.common.storage.model.AfkPeriodEndReason;
import com.jannik_kuehn.common.storage.model.SessionContextDefaults;
import com.jannik_kuehn.common.storage.model.TimeEntryReason;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * Listener for accumulating online time of players.
 */
public class TimeAccumulatorPaperListener implements Listener {
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
    public TimeAccumulatorPaperListener(final LoriTimePlugin loriTimePlugin) {
        this.loriTimePlugin = loriTimePlugin;
        this.log = loriTimePlugin.getLoggerFactory().create(TimeAccumulatorPaperListener.class);
    }

    /**
     * Starts accumulating online time when a player joins the server.
     *
     * @param event The {@link PlayerJoinEvent} event.
     */
    @EventHandler
    public void playerJoin(final PlayerJoinEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        final String name = event.getPlayer().getName();
        final String server = loriTimePlugin.getConfig().getString("server.name", SessionContextDefaults.SERVER);
        final String world = event.getPlayer().getWorld().getName();
        final long now = System.currentTimeMillis();
        loriTimePlugin.rememberPlayerName(uuid, name);
        loriTimePlugin.rememberScope(server, world);
        loriTimePlugin.getScheduler().runAsyncOnce(() -> {
            try {
                loriTimePlugin.getAccumulator().startAccumulating(uuid, name, server, world, now);
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
                final TimeEntryReason reason = loriTimePlugin.consumeAfkKick(uuid)
                        ? TimeEntryReason.PLAYER_AFK_KICK : TimeEntryReason.PLAYER_LEAVE;
                loriTimePlugin.closeAfkPeriod(uuid, reason == TimeEntryReason.PLAYER_AFK_KICK
                        ? AfkPeriodEndReason.KICKED : AfkPeriodEndReason.DISCONNECTED);
                loriTimePlugin.getAccumulator().stopAccumulatingAndSaveOnlineTime(uuid, now, reason);
                loriTimePlugin.getPlayerConverter().removePlayerFromCache(uuid);
            } catch (final StorageException e) {
                log.warn("error while stopping accumulation of online time for player " + uuid, e);
            }
        });
    }

    /**
     * Switches the accumulating context when a player changes worlds.
     *
     * @param event The {@link PlayerChangedWorldEvent} event.
     */
    @EventHandler
    public void playerChangedWorld(final PlayerChangedWorldEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        final String name = event.getPlayer().getName();
        final String server = loriTimePlugin.getConfig().getString("server.name", SessionContextDefaults.SERVER);
        final String world = event.getPlayer().getWorld().getName();
        final long now = System.currentTimeMillis();
        loriTimePlugin.rememberPlayerName(uuid, name);
        loriTimePlugin.rememberScope(server, world);
        loriTimePlugin.getScheduler().runAsyncOnce(() -> {
            try {
                loriTimePlugin.getAccumulator().switchContext(uuid, name, server, world, now);
            } catch (final StorageException e) {
                log.warn("could not switch accumulating context for player " + uuid, e);
            }
        });
    }
}
