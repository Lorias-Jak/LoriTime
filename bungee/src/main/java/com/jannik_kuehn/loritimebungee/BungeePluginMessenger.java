package com.jannik_kuehn.loritimebungee;

import com.jannik_kuehn.common.module.messaging.PluginMessaging;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class BungeePluginMessenger extends PluginMessaging implements Listener {

    public BungeePluginMessenger(final LoriTimeBungee bungeePlugin) {
        super(bungeePlugin.getPlugin());
    }

    @EventHandler
    public void onPluginMessage(final PluginMessageEvent event) {
        if (!event.getTag().contains("loritime:")) {
            return;
        }
        if (event.getSender() instanceof final ProxiedPlayer player) {
            loriTimePlugin.getLogger().severe(player.getName() + " tried to change the plugin message of LoriTime!");
            return;
        }
        if (event.isCancelled()) {
            return;
        }

        final byte[] data = event.getData();
        processPluginMessage(event.getTag(), data);
    }
}
