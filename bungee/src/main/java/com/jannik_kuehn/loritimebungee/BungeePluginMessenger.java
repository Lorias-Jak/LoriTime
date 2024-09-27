package com.jannik_kuehn.loritimebungee;

import com.jannik_kuehn.common.api.common.CommonSender;
import com.jannik_kuehn.common.api.logger.LoriTimeLogger;
import com.jannik_kuehn.common.module.messaging.PluginMessaging;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.Optional;
import java.util.UUID;

public class BungeePluginMessenger extends PluginMessaging implements Listener {
    private final LoriTimeBungee loriTimeBungee;

    private final LoriTimeLogger log;

    public BungeePluginMessenger(final LoriTimeBungee loriTimeBungee) {
        super(loriTimeBungee.getPlugin());
        this.loriTimeBungee = loriTimeBungee;
        this.log = loriTimeBungee.getPlugin().getLoggerFactory().create(BungeePluginMessenger.class);
    }

    @EventHandler
    public void onPluginMessage(final PluginMessageEvent event) {
        if (!event.getTag().contains("loritime:")) {
            return;
        }
        if (event.getSender() instanceof final ProxiedPlayer player) {
            log.error(player.getName() + " tried to change the plugin message of LoriTime!");
            return;
        }
        if (event.isCancelled()) {
            return;
        }

        final byte[] data = event.getData();
        processPluginMessage(event.getTag(), data);
    }

    @Override
    public void sendPluginMessage(final String channelIdentifier, final Object... message) {
        final Server server = getServer(message);
        if (server == null) {
            return;
        }

        final byte[] data = getDataAsByte(message);
        server.sendData(channelIdentifier, data);
    }

    private Server getServer(final Object... message) {
        if (message.length <= 1) {
            return null;
        }

        if (!(message[0] instanceof UUID)) {
            return null;
        }

        final Optional<CommonSender> optionalSender = loriTimePlugin.getServer().getPlayer((UUID) message[0]);
        if (optionalSender.isEmpty()) {
            return null;
        }
        final UUID uuid = optionalSender.get().getUniqueId();
        final ProxiedPlayer player = loriTimeBungee.getProxy().getPlayer(uuid);
        return player.getServer();
    }
}
