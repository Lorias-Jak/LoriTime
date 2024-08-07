package com.jannik_kuehn.loritimebukkit.messenger;

import com.jannik_kuehn.common.api.scheduler.PluginTask;
import com.jannik_kuehn.common.api.storage.TimeStorage;
import com.jannik_kuehn.common.module.messaging.PluginMessaging;
import com.jannik_kuehn.common.utils.UuidUtil;
import com.jannik_kuehn.loritimebukkit.LoriTimeBukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalLong;
import java.util.UUID;

public class SlavedTimeStorageCache extends PluginMessaging implements TimeStorage, PluginMessageListener, Listener {
    private final LoriTimeBukkit loriTimeBukkit;

    private final BukkitPluginMessenger pluginMessenger;

    private final Map<String, Long> trackedPlayers;

    PluginTask fetchTask;

    public SlavedTimeStorageCache(final LoriTimeBukkit loriTimeBukkit, final BukkitPluginMessenger pluginMessenger, final long updateInterval) {
        super(loriTimeBukkit.getPlugin());
        this.loriTimeBukkit = loriTimeBukkit;
        this.pluginMessenger = pluginMessenger;
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
    public void close() {
        fetchTask.cancel();
    }

    @Override
    public void onPluginMessageReceived(@NotNull final String channel, @NotNull final Player player, final byte @NotNull [] message) {
        if (channel.equalsIgnoreCase(SLAVED_TIME_STORAGE)) {
            try (ByteArrayInputStream byteInputStream = new ByteArrayInputStream(message);
                 DataInputStream input = new DataInputStream(byteInputStream)) {

                final byte[] uuidBytes = new byte[16];
                input.readFully(uuidBytes);
                final UUID playerUUID = UuidUtil.fromBytes(uuidBytes);
                final String inputString = input.readUTF();
                if (inputString.equals("send")) {
                    trackedPlayers.put(playerUUID.toString(), input.readLong());
                }
            } catch (final IOException e) {
                loriTimePlugin.getLogger().error("could not deserialize plugin message", e);
            }
        }
    }

    @Override
    public void sendPluginMessage(final String channelIdentifier, final Object... message) {
        final byte[] data = getDataAsByte(message);
        if (data != null) {
            loriTimeBukkit.getServer().sendPluginMessage(loriTimeBukkit, channelIdentifier, data);
        } else {
            loriTimePlugin.getLogger().warning("could not send plugin message, data is null");
        }
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        trackedPlayers.put(event.getPlayer().getUniqueId().toString(), 0L);
        loriTimePlugin.getScheduler().runAsyncOnceLater(1L, () -> sendPluginMessage(SLAVED_TIME_STORAGE, event.getPlayer().getUniqueId(), "get"));
    }

    @EventHandler
    public void onPlayerLeave(final PlayerQuitEvent event) {
        trackedPlayers.remove(event.getPlayer().getUniqueId().toString());
    }
}
