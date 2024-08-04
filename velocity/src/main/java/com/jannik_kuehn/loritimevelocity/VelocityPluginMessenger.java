package com.jannik_kuehn.loritimevelocity;

import com.jannik_kuehn.common.api.common.CommonSender;
import com.jannik_kuehn.common.module.messaging.PluginMessaging;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;

import java.util.Optional;
import java.util.UUID;

public class VelocityPluginMessenger extends PluginMessaging {
    private final LoriTimeVelocity loriTimeVelocity;

    public VelocityPluginMessenger(final LoriTimeVelocity loriTimeVelocity) {
        super(loriTimeVelocity.getPlugin());
        this.loriTimeVelocity = loriTimeVelocity;
    }

    @Subscribe
    public void onPluginMessage(final PluginMessageEvent event) {
        if (!event.getIdentifier().getId().contains("loritime:")) {
            return;
        }
        if (!(event.getSource() instanceof ServerConnection)) {
            return;
        }
        if (event.getResult().equals(PluginMessageEvent.ForwardResult.handled())) {
            return;
        }

        final byte[] data = event.getData();
        processPluginMessage(event.getIdentifier().getId(), data);
        event.setResult(PluginMessageEvent.ForwardResult.handled());
    }

    @Override
    public void sendPluginMessage(final String channelIdentifier, final Object... message) {
        final ServerConnection connection = getConnection(message);
        if (connection == null) {
            return;
        }

        final MinecraftChannelIdentifier identifier = MinecraftChannelIdentifier.from(channelIdentifier);
        final byte[] data = getDataAsByte(message);
        connection.sendPluginMessage(identifier, data);
    }

    private ServerConnection getConnection(final Object... message) {
        if (message.length <= 1) {
            return null;
        }

        if (!(message[0] instanceof UUID)) {
            return null;
        }

        final Optional<CommonSender> optionalCommonSender = loriTimePlugin.getServer().getPlayer((UUID) message[0]);
        if (optionalCommonSender.isEmpty()) {
            return null;
        }

        final UUID senderUUID = optionalCommonSender.get().getUniqueId();
        final Optional<Player> optionalPlayer = loriTimeVelocity.getProxyServer().getPlayer(senderUUID);
        if (optionalPlayer.isEmpty()) {
            return null;
        }

        final Optional<ServerConnection> serverConnection = optionalPlayer.get().getCurrentServer();
        return serverConnection.orElse(null);
    }
}
