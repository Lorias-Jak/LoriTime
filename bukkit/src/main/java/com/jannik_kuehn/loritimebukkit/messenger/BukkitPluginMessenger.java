package com.jannik_kuehn.loritimebukkit.messenger;

import com.jannik_kuehn.common.module.messaging.PluginMessaging;
import com.jannik_kuehn.loritimebukkit.LoriTimeBukkit;

public class BukkitPluginMessenger extends PluginMessaging {

    private final LoriTimeBukkit bukkitPlugin;

    public BukkitPluginMessenger(final LoriTimeBukkit bukkitPlugin) {
        super(bukkitPlugin.getPlugin());
        this.bukkitPlugin = bukkitPlugin;
    }

    @Override
    public void sendPluginMessage(final String channelIdentifier, final Object... message) {
        final byte[] data = getDataAsByte(message);

        if (data != null) {
            bukkitPlugin.getServer().sendPluginMessage(bukkitPlugin, channelIdentifier, data);
        } else {
            loriTimePlugin.getLogger().warning("could not send plugin message, data is null");
        }
    }
}
