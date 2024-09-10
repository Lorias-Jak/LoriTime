package com.jannik_kuehn.loritimebukkit.messenger;

import com.jannik_kuehn.common.module.messaging.PluginMessaging;
import com.jannik_kuehn.loritimebukkit.LoriTimeBukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class BukkitPluginMessenger extends PluginMessaging {

    private final LoriTimeBukkit bukkitPlugin;

    public BukkitPluginMessenger(final LoriTimeBukkit bukkitPlugin) {
        super(bukkitPlugin.getPlugin());
        this.bukkitPlugin = bukkitPlugin;
    }

    @Override
    public void sendPluginMessage(final String channelIdentifier, final Object... message) {
        final UUID uuid = (UUID) message[0];
        final byte[] data = getDataAsByte(message);

        if (data != null) {
            final Player bukkitPlayer = bukkitPlugin.getServer().getPlayer(uuid);
            if (bukkitPlayer != null) {
                bukkitPlayer.sendPluginMessage(bukkitPlugin, channelIdentifier, data);
            }
        } else {
            loriTimePlugin.getLogger().warning("could not send plugin message, data is null");
        }
    }
}
