package com.jannik_kuehn.loritimebukkit.messenger;

import com.jannik_kuehn.common.api.logger.LoriTimeLogger;
import com.jannik_kuehn.common.module.messaging.PluginMessaging;
import com.jannik_kuehn.loritimebukkit.LoriTimeBukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Messenger for the plugin messages.
 * Extends the {@link PluginMessaging} class.
 */
public class BukkitPluginMessenger extends PluginMessaging {
    /**
     * The {@link LoriTimeBukkit} instance.
     */
    private final LoriTimeBukkit bukkitPlugin;

    /**
     * The {@link LoriTimeLogger} instance.
     */
    private final LoriTimeLogger log;

    /**
     * Creates a new instance of the {@link BukkitPluginMessenger}.
     *
     * @param bukkitPlugin The {@link LoriTimeBukkit} instance.
     */
    public BukkitPluginMessenger(final LoriTimeBukkit bukkitPlugin) {
        super(bukkitPlugin.getPlugin());
        this.bukkitPlugin = bukkitPlugin;
        this.log = bukkitPlugin.getPlugin().getLoggerFactory().create(BukkitPluginMessenger.class, "BukkitPluginMessenger");
    }

    @Override
    public void sendPluginMessage(final String channelIdentifier, final Object... message) {
        log.debug("Sending PluginMessage with channel: " + channelIdentifier);
        final UUID uuid = (UUID) message[0];
        final byte[] data = getDataAsByte(message);

        if (data != null) {
            final Player bukkitPlayer = bukkitPlugin.getServer().getPlayer(uuid);
            if (bukkitPlayer != null) {
                log.debug("Sending PluginMessage to player: " + bukkitPlayer.getName());
                bukkitPlayer.sendPluginMessage(bukkitPlugin, channelIdentifier, data);
            }
        } else {
            log.warn("could not send plugin message, data is null");
        }
    }
}
