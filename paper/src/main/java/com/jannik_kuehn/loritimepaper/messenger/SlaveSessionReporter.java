package com.jannik_kuehn.loritimepaper.messenger;

import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import com.jannik_kuehn.common.api.scheduler.PluginTask;
import com.jannik_kuehn.common.api.storage.TimeEntryReason;
import com.jannik_kuehn.common.module.messaging.PluginMessaging;
import com.jannik_kuehn.loritimepaper.LoriTimePaper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reports slave-observed session chunks to the master.
 */
@SuppressWarnings("PMD.CommentRequired")
public class SlaveSessionReporter extends PluginMessaging implements Listener, AutoCloseable {

    private static final int PROTOCOL_VERSION = 2;

    private final PaperPluginMessenger pluginMessenger;

    private final WrappedLogger log;

    private final Map<UUID, ActiveRemoteSession> activeSessions;

    private final PluginTask flushTask;

    public SlaveSessionReporter(final LoriTimePaper loriTimePaper, final PaperPluginMessenger pluginMessenger, final long updateInterval) {
        super(loriTimePaper.getPlugin());
        this.pluginMessenger = pluginMessenger;
        this.log = loriTimePaper.getPlugin().getLoggerFactory().create(SlaveSessionReporter.class);
        this.activeSessions = new ConcurrentHashMap<>();
        this.flushTask = loriTimePlugin.getScheduler().scheduleAsync(updateInterval / 2L, updateInterval, this::flushSessions);
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        activeSessions.put(player.getUniqueId(), context(player, System.currentTimeMillis()));
    }

    @EventHandler
    public void onPlayerLeave(final PlayerQuitEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        final ActiveRemoteSession session = activeSessions.remove(uuid);
        if (session != null) {
            sendSession(session, System.currentTimeMillis(), TimeEntryReason.PLAYER_LEAVE);
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

    private ActiveRemoteSession context(final Player player, final long startedAt) {
        return new ActiveRemoteSession(player.getUniqueId(), player.getName(), player.getServer().getName(),
                player.getWorld().getName(), startedAt);
    }

    private void sendSession(final ActiveRemoteSession session, final long stoppedAt, final TimeEntryReason reason) {
        log.debug("Reporting remote session for player " + session.uuid());
        sendPluginMessage(SLAVED_TIME_STORAGE, session.uuid(), "session", PROTOCOL_VERSION, session.name(),
                session.server(), session.world(), session.startedAt(), stoppedAt, reason.name());
    }

    @Override
    public void sendPluginMessage(final String channelIdentifier, final Object... message) {
        pluginMessenger.sendPluginMessage(channelIdentifier, message);
    }

    @Override
    public void close() {
        flushTask.cancel();
        flushSessions();
    }

    private record ActiveRemoteSession(UUID uuid, String name, String server, String world, long startedAt) {
        private ActiveRemoteSession withStartedAt(final long nextStartedAt) {
            return new ActiveRemoteSession(uuid, name, server, world, nextStartedAt);
        }
    }
}
