package com.jannik_kuehn.loritimebukkit;

import com.jannik_kuehn.common.module.messaging.PluginMessaging;
import org.bukkit.plugin.messaging.PluginMessageRecipient;

public class BukkitPluginMessenger extends PluginMessaging {

    private final LoriTimeBukkit bukkitPlugin;

    public BukkitPluginMessenger(final LoriTimeBukkit bukkitPlugin) {
        super(bukkitPlugin.getPlugin());
        this.bukkitPlugin = bukkitPlugin;
    }

    public void sendPluginMessage(final PluginMessageRecipient target, final String channel, final Object... message) {
        final byte[] data = getDataAsByte(message);

        if (data != null) {
            target.sendPluginMessage(bukkitPlugin, channel, data);
        } else {
            loriTimePlugin.getLogger().warning("could not send plugin message, data is null");
        }
    }
}
