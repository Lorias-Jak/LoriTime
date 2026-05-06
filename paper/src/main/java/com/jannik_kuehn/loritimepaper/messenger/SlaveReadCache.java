package com.jannik_kuehn.loritimepaper.messenger;

import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
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
import java.util.Map;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Read cache for slave instances.
 */
public class SlaveReadCache extends PluginMessaging implements PluginMessageListener, Listener {
    /**
     * The {@link PaperPluginMessenger} instance.
     */
    private final PaperPluginMessenger pluginMessenger;

    /**
     * The {@link WrappedLogger} instance.
     */
    private final WrappedLogger log;

    /**
     * The cached times.
     */
    private final Map<UUID, Long> cachedTimes;

    /**
     * Initializes a new instance of the {@code SlaveReadCache} class.
     *
     * @param loriTimePaper   The {@link LoriTimePaper} instance, providing access to the plugin's main features.
     * @param pluginMessenger The {@link PaperPluginMessenger} instance used for sending plugin messages.
     */
    public SlaveReadCache(final LoriTimePaper loriTimePaper, final PaperPluginMessenger pluginMessenger) {
        super(loriTimePaper.getPlugin());
        this.pluginMessenger = pluginMessenger;
        this.log = loriTimePaper.getPlugin().getLoggerFactory().create(SlaveReadCache.class);
        this.cachedTimes = new ConcurrentHashMap<>();
    }

    /**
     * Retrieves the cached time associated with the specified UUID.
     *
     * @param uuid The UUID of the entity for which the cached time is being retrieved.
     * @return {@code OptionalLong} containing the cached time if present, or an empty {@code OptionalLong} if no time
     * is cached for the given UUID.
     */
    public OptionalLong getTime(final UUID uuid) {
        final Long value = cachedTimes.get(uuid);
        return value == null ? OptionalLong.empty() : OptionalLong.of(value);
    }

    /**
     * Sends a request to refresh the cached time data for the specified UUID. This method utilizes
     * the plugin messaging system to communicate the request to the designated storage channel.
     *
     * @param uuid The unique identifier of the entity for which the refresh request is being made.
     */
    public void requestRefresh(final UUID uuid) {
        pluginMessenger.sendPluginMessage(SLAVED_TIME_STORAGE, uuid, "get");
    }

    @Override
    @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
    public void onPluginMessageReceived(final String channel, final Player player, final byte[] message) {
        if (!SLAVED_TIME_STORAGE.equalsIgnoreCase(channel)) {
            return;
        }
        try (ByteArrayInputStream byteInputStream = new ByteArrayInputStream(message);
             DataInputStream input = new DataInputStream(byteInputStream)) {
            final byte[] uuidBytes = new byte[16];
            input.readFully(uuidBytes);
            final UUID playerUUID = UuidUtil.fromBytes(uuidBytes);
            final String action = input.readUTF();
            if ("send".equals(action)) {
                cachedTimes.put(playerUUID, input.readLong());
            }
        } catch (final IOException e) {
            log.error("could not deserialize slave read cache message", e);
        }
    }

    @Override
    public void sendPluginMessage(final String channelIdentifier, final Object... message) {
        pluginMessenger.sendPluginMessage(channelIdentifier, message);
    }

    /**
     * Handles the event triggered when a player joins the server. Upon a player joining, their unique identifier (UUID)
     * is mapped to an initial cached time in the local cache. Additionally, an asynchronous task is scheduled
     * to request a refresh of the cached time data for the player after a delay.
     *
     * @param event The {@link PlayerJoinEvent} that is triggered when a player joins the server. This event provides
     *              access to the player's data and interactions, including their unique identifier.
     */
    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        cachedTimes.put(uuid, 0L);
        loriTimePlugin.getScheduler().runAsyncOnceLater(1L, () -> requestRefresh(uuid));
    }

    /**
     * Handles the event triggered when a player leaves the server.
     * Upon a player leaving, their unique identifier (UUID) is removed
     * from the local cache to free up resources associated with the player.
     *
     * @param event The {@link PlayerQuitEvent} that is triggered when a player leaves the server.
     *              This event provides access to the player's data, including their unique identifier.
     */
    @EventHandler
    public void onPlayerLeave(final PlayerQuitEvent event) {
        cachedTimes.remove(event.getPlayer().getUniqueId());
    }
}
