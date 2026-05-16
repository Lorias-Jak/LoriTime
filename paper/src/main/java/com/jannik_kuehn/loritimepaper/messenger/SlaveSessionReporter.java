package com.jannik_kuehn.loritimepaper.messenger;

import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import com.jannik_kuehn.common.api.scheduler.PluginTask;
import com.jannik_kuehn.common.module.messaging.PluginMessaging;
import com.jannik_kuehn.common.module.messaging.StorageMessageType;
import com.jannik_kuehn.loritimepaper.LoriTimePaper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reports slave-observed world context to the master.
 */
public class SlaveSessionReporter extends PluginMessaging implements Listener, AutoCloseable {

    /**
     * Messenger used for outgoing plugin messages.
     */
    private final PaperPluginMessenger pluginMessenger;

    /**
     * Logger for slave session reporting.
     */
    private final WrappedLogger log;

    /**
     * Active world contexts keyed by player UUID.
     */
    private final Map<UUID, ActiveRemoteWorld> activeWorlds;

    /**
     * Periodic task that reports active world contexts to the master.
     */
    private final PluginTask updateTask;

    /**
     * Creates a slave session reporter.
     *
     * @param loriTimePaper the Paper plugin instance.
     * @param pluginMessenger the plugin messenger used for outgoing messages.
     * @param updateInterval the session flush interval in seconds.
     */
    public SlaveSessionReporter(final LoriTimePaper loriTimePaper, final PaperPluginMessenger pluginMessenger, final long updateInterval) {
        super(loriTimePaper.getPlugin());
        this.pluginMessenger = pluginMessenger;
        this.log = loriTimePaper.getPlugin().getLoggerFactory().create(SlaveSessionReporter.class);
        this.activeWorlds = new ConcurrentHashMap<>();
        this.updateTask = loriTimePlugin.getScheduler().scheduleAsync(updateInterval / 2L, updateInterval, this::reportWorlds);
    }

    /**
     * Starts tracking a remote session when a player joins the slave.
     *
     * @param event the join event.
     */
    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        reportWorld(player);
    }

    /**
     * Reports the final remote session when a player leaves the slave.
     *
     * @param event the quit event.
     */
    @EventHandler
    public void onPlayerLeave(final PlayerQuitEvent event) {
        reportWorld(event.getPlayer());
        activeWorlds.remove(event.getPlayer().getUniqueId());
    }

    /**
     * Reports a world switch when a player changes worlds on the slave.
     *
     * @param event the world-change event.
     */
    @EventHandler
    public void onPlayerChangedWorld(final PlayerChangedWorldEvent event) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();
        final ActiveRemoteWorld next = context(player, System.currentTimeMillis());
        final ActiveRemoteWorld previous = activeWorlds.get(uuid);
        if (previous != null && previous.world().equals(next.world())) {
            return;
        }
        activeWorlds.put(uuid, next);
        sendWorldSwitch(next);
    }

    private void reportWorlds() {
        for (final ActiveRemoteWorld context : activeWorlds.values()) {
            sendWorld(context.withObservedAt(System.currentTimeMillis()));
        }
    }

    private void reportWorld(final Player player) {
        final ActiveRemoteWorld context = context(player, System.currentTimeMillis());
        activeWorlds.put(player.getUniqueId(), context);
        sendWorld(context);
    }

    private ActiveRemoteWorld context(final Player player, final long observedAtMs) {
        return new ActiveRemoteWorld(player.getUniqueId(), player.getWorld().getName(), observedAtMs);
    }

    private void sendWorld(final ActiveRemoteWorld context) {
        log.debug("Reporting remote world context for player " + context.uuid());
        sendPluginMessage(SLAVED_TIME_STORAGE, context.uuid(), StorageMessageType.WORLD.wireValue(), STORAGE_PROTOCOL_VERSION,
                context.world(), context.observedAtMs());
    }

    private void sendWorldSwitch(final ActiveRemoteWorld context) {
        log.debug("Reporting remote world switch for player " + context.uuid());
        sendPluginMessage(SLAVED_TIME_STORAGE, context.uuid(), StorageMessageType.WORLD_SWITCH.wireValue(), STORAGE_PROTOCOL_VERSION,
                context.world(), context.observedAtMs());
    }

    /**
     * Sends a plugin message through the Paper messenger.
     *
     * @param channelIdentifier the target channel.
     * @param message the message payload.
     */
    @Override
    public void sendPluginMessage(final String channelIdentifier, final Object... message) {
        pluginMessenger.sendPluginMessage(channelIdentifier, message);
    }

    /**
     * Stops the periodic flush task and reports all active sessions.
     */
    @Override
    public void close() {
        updateTask.cancel();
        reportWorlds();
    }

    private record ActiveRemoteWorld(UUID uuid, String world, long observedAtMs) {
        private ActiveRemoteWorld withObservedAt(final long nextObservedAtMs) {
            return new ActiveRemoteWorld(uuid, world, nextObservedAtMs);
        }
    }
}
