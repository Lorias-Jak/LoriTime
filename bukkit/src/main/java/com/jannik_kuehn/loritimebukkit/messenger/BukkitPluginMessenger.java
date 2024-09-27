package com.jannik_kuehn.loritimebukkit.messenger;

import com.jannik_kuehn.common.api.logger.LoriTimeLogger;
import com.jannik_kuehn.common.module.messaging.PluginMessaging;
import com.jannik_kuehn.loritimebukkit.LoriTimeBukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class BukkitPluginMessenger extends PluginMessaging {

    private final LoriTimeBukkit bukkitPlugin;

    private final LoriTimeLogger log;

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
