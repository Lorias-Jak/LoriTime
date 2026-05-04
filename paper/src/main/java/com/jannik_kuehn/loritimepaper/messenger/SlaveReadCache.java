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
@SuppressWarnings("PMD.CommentRequired")
public class SlaveReadCache extends PluginMessaging implements PluginMessageListener, Listener {

    private final PaperPluginMessenger pluginMessenger;

    private final WrappedLogger log;

    private final Map<UUID, Long> cachedTimes;

    public SlaveReadCache(final LoriTimePaper loriTimePaper, final PaperPluginMessenger pluginMessenger) {
        super(loriTimePaper.getPlugin());
        this.pluginMessenger = pluginMessenger;
        this.log = loriTimePaper.getPlugin().getLoggerFactory().create(SlaveReadCache.class);
        this.cachedTimes = new ConcurrentHashMap<>();
    }

    public OptionalLong getTime(final UUID uuid) {
        final Long value = cachedTimes.get(uuid);
        return value == null ? OptionalLong.empty() : OptionalLong.of(value);
    }

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

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        cachedTimes.put(uuid, 0L);
        loriTimePlugin.getScheduler().runAsyncOnceLater(1L, () -> requestRefresh(uuid));
    }

    @EventHandler
    public void onPlayerLeave(final PlayerQuitEvent event) {
        cachedTimes.remove(event.getPlayer().getUniqueId());
    }
}
