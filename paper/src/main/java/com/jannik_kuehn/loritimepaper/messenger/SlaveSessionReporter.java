package com.jannik_kuehn.loritimepaper.messenger;

import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import com.jannik_kuehn.common.api.scheduler.PluginTask;
import com.jannik_kuehn.common.api.storage.TimeEntryReason;
import com.jannik_kuehn.common.module.messaging.PluginMessaging;
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
 * Reports slave-observed session chunks to the master.
 */
public class SlaveSessionReporter extends PluginMessaging implements Listener, AutoCloseable {

    /**
     * Protocol version for slave session messages.
     */
    private static final int PROTOCOL_VERSION = 2;

    /**
     * Messenger used for outgoing plugin messages.
     */
    private final PaperPluginMessenger pluginMessenger;

    /**
     * Logger for slave session reporting.
     */
    private final WrappedLogger log;

    /**
     * Active remote sessions keyed by player UUID.
     */
    private final Map<UUID, ActiveRemoteSession> activeSessions;

    /**
     * Periodic task that flushes active sessions to the master.
     */
    private final PluginTask flushTask;

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
        this.activeSessions = new ConcurrentHashMap<>();
        this.flushTask = loriTimePlugin.getScheduler().scheduleAsync(updateInterval / 2L, updateInterval, this::flushSessions);
    }

    /**
     * Starts tracking a remote session when a player joins the slave.
     *
     * @param event the join event.
     */
    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        activeSessions.put(player.getUniqueId(), context(player, System.currentTimeMillis()));
    }

    /**
     * Reports the final remote session when a player leaves the slave.
     *
     * @param event the quit event.
     */
    @EventHandler
    public void onPlayerLeave(final PlayerQuitEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        final ActiveRemoteSession session = activeSessions.remove(uuid);
        if (session != null) {
            sendSession(session, System.currentTimeMillis(), TimeEntryReason.PLAYER_LEAVE);
        }
    }

    /**
     * Reports a context switch when a player changes worlds on the slave.
     *
     * @param event the world-change event.
     */
    @EventHandler
    public void onPlayerChangedWorld(final PlayerChangedWorldEvent event) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();
        final long now = System.currentTimeMillis();
        final ActiveRemoteSession next = context(player, now);
        final ActiveRemoteSession previous = activeSessions.get(uuid);
        if (previous == null || (previous.server().equals(next.server()) && previous.world().equals(next.world()))) {
            return;
        }
        if (activeSessions.replace(uuid, previous, next)) {
            sendSession(previous, now, TimeEntryReason.CONTEXT_SWITCH);
        }
    }

    private void flushSessions() {
        final long now = System.currentTimeMillis();
        for (final Map.Entry<UUID, ActiveRemoteSession> entry : activeSessions.entrySet()) {
            final ActiveRemoteSession session = entry.getValue();
            final ActiveRemoteSession next = session.withStartedAt(now);
            if (activeSessions.replace(entry.getKey(), session, next)) {
                sendSession(session, now, TimeEntryReason.AUTO_FLUSH);
            }
        }
    }

    private ActiveRemoteSession context(final Player player, final long startedAtMs) {
        return new ActiveRemoteSession(player.getUniqueId(), player.getName(),
                loriTimePlugin.getConfig().getString("server.name", "default"),
                player.getWorld().getName(), startedAtMs);
    }

    private void sendSession(final ActiveRemoteSession session, final long stoppedAtMs, final TimeEntryReason reason) {
        log.debug("Reporting remote session for player " + session.uuid());
        sendPluginMessage(SLAVED_TIME_STORAGE, session.uuid(), "session", PROTOCOL_VERSION, session.name(),
                session.server(), session.world(), session.startedAtMs(), stoppedAtMs, reason.name());
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
        flushTask.cancel();
        flushSessions();
    }

    private record ActiveRemoteSession(UUID uuid, String name, String server, String world, long startedAtMs) {
        private ActiveRemoteSession withStartedAt(final long nextStartedAtMs) {
            return new ActiveRemoteSession(uuid, name, server, world, nextStartedAtMs);
        }
    }
}
