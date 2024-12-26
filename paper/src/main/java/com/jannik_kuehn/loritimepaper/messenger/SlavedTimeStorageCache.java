package com.jannik_kuehn.loritimepaper.messenger;

import com.jannik_kuehn.common.api.logger.LoriTimeLogger;
import com.jannik_kuehn.common.api.scheduler.PluginTask;
import com.jannik_kuehn.common.api.storage.TimeStorage;
import com.jannik_kuehn.common.exception.StorageException;
import com.jannik_kuehn.common.module.messaging.PluginMessaging;
import com.jannik_kuehn.common.utils.UuidUtil;
import com.jannik_kuehn.loritimepaper.LoriTimePaper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalLong;
import java.util.UUID;

/**
 * Cache for the time storage of the slave servers.
 */
public class SlavedTimeStorageCache extends PluginMessaging implements TimeStorage, PluginMessageListener, Listener {
    /**
     * The channel identifier for the plugin messages.
     */
    private final LoriTimePaper loriTimePaper;

    /**
     * The {@link PaperPluginMessenger} instance.
     */
    private final PaperPluginMessenger pluginMessenger;

    /**
     * The {@link LoriTimeLogger} instance.
     */
    private final LoriTimeLogger log;

    /**
     * The map of tracked players with their online time.
     */
    private final Map<String, Long> trackedPlayers;

    /**
     * The {@link PluginTask} instance for fetching the online time.
     */
    private final PluginTask fetchTask;

    /**
     * Creates a new instance of the {@link SlavedTimeStorageCache}.
     *
     * @param loriTimePaper   The {@link LoriTimePaper} instance.
     * @param pluginMessenger The {@link PaperPluginMessenger} instance.
     * @param updateInterval  The update interval for fetching the online time.
     */
    public SlavedTimeStorageCache(final LoriTimePaper loriTimePaper, final PaperPluginMessenger pluginMessenger, final long updateInterval) {
        super(loriTimePaper.getPlugin());
        this.loriTimePaper = loriTimePaper;
        this.pluginMessenger = pluginMessenger;

        this.log = loriTimePaper.getPlugin().getLoggerFactory().create(SlavedTimeStorageCache.class);
        this.trackedPlayers = new HashMap<>();
        this.fetchTask = loriTimePlugin.getScheduler().scheduleAsync(0, updateInterval, this::fetchOnlineTime);
    }

    private void fetchOnlineTime() {
        for (final String uuidString : trackedPlayers.keySet()) {
            final UUID uuid = UUID.fromString(uuidString);
            pluginMessenger.sendPluginMessage(SLAVED_TIME_STORAGE, uuid, "get");
        }
    }

    @Override
    public OptionalLong getTime(final UUID uuid) {
        return trackedPlayers.containsKey(uuid.toString()) ? OptionalLong.of(trackedPlayers.get(uuid.toString())) : OptionalLong.empty();
    }

    @Override
    public void addTime(final UUID uuid, final long additionalTime) {
        sendPluginMessage(SLAVED_TIME_STORAGE, uuid, "add", additionalTime);
    }

    @Override
    public void addTimes(final Map<UUID, Long> additionalTimes) {
        for (final Map.Entry<UUID, Long> entry : additionalTimes.entrySet()) {
            sendPluginMessage(SLAVED_TIME_STORAGE, entry.getKey(), "add", entry.getValue());
        }
    }

    @Override
    public Map<String, ?> getAllTimeEntries() {
        return Map.copyOf(trackedPlayers);
    }

    @Override
    public void removeTimeHolder(final UUID uniqueId) throws StorageException, SQLException {
        trackedPlayers.remove(uniqueId.toString());
    }

    @Override
    public void close() {
        fetchTask.cancel();
    }

    @Override
    @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
    public void onPluginMessageReceived(final String channel, final Player player, final byte[] message) {
        if (SLAVED_TIME_STORAGE.equalsIgnoreCase(channel)) {
            log.debug("Received PluginMessage from " + player.getName() + " with channel " + channel);
            try (ByteArrayInputStream byteInputStream = new ByteArrayInputStream(message);
                 DataInputStream input = new DataInputStream(byteInputStream)) {

                final byte[] uuidBytes = new byte[16];
                input.readFully(uuidBytes);
                final UUID playerUUID = UuidUtil.fromBytes(uuidBytes);
                final String inputString = input.readUTF();
                if ("send".equals(inputString)) {
                    trackedPlayers.put(playerUUID.toString(), input.readLong());
                }
            } catch (final IOException e) {
                log.error("could not deserialize plugin message", e);
            }
        }
    }

    @Override
    public void sendPluginMessage(final String channelIdentifier, final Object... message) {
        final byte[] data = getDataAsByte(message);
        if (data != null) {
            final Player player = loriTimePaper.getServer().getPlayer((UUID) message[0]);
            if (player != null) {
                player.sendPluginMessage(loriTimePaper, channelIdentifier, data);
            }
        } else {
            log.warn("could not send plugin message, data is null");
        }
    }

    /**
     * Handles the {@link PlayerJoinEvent} to add the player to the tracked players.
     *
     * @param event The {@link PlayerJoinEvent}.
     */
    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        trackedPlayers.put(event.getPlayer().getUniqueId().toString(), 0L);
        loriTimePlugin.getScheduler().runAsyncOnceLater(1L, () -> sendPluginMessage(SLAVED_TIME_STORAGE,
                event.getPlayer().getUniqueId(), "get"));
    }

    /**
     * Handles the {@link PlayerQuitEvent} to remove the player from the tracked players.
     *
     * @param event The {@link PlayerQuitEvent}.
     */
    @EventHandler
    public void onPlayerLeave(final PlayerQuitEvent event) {
        trackedPlayers.remove(event.getPlayer().getUniqueId().toString());
    }
}
