package com.jannik_kuehn.loritimevelocity;

import com.jannik_kuehn.common.api.common.CommonSender;
import com.jannik_kuehn.common.api.logger.LoriTimeLogger;
import com.jannik_kuehn.common.module.messaging.PluginMessaging;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;

import java.util.Optional;
import java.util.UUID;

/**
 * The {@link PluginMessaging} implementation for Velocity.
 * This class is responsible for sending and receiving PluginMessages.
 */
public class VelocityPluginMessenger extends PluginMessaging {
    /**
     * The {@link LoriTimeVelocity} instance.
     */
    private final LoriTimeVelocity loriTimeVelocity;

    /**
     * The {@link LoriTimeLogger} instance.
     */
    private final LoriTimeLogger log;

    /**
     * Creates a new {@link VelocityPluginMessenger} instance.
     *
     * @param loriTimeVelocity The {@link LoriTimeVelocity} instance.
     */
    public VelocityPluginMessenger(final LoriTimeVelocity loriTimeVelocity) {
        super(loriTimeVelocity.getPlugin());
        this.loriTimeVelocity = loriTimeVelocity;
        this.log = loriTimeVelocity.getPlugin().getLoggerFactory().create(VelocityPluginMessenger.class, "VelocityPluginMessenger");
    }

    /**
     * Handles the received PluginMessage.
     *
     * @param event The {@link PluginMessageEvent} instance.
     */
    @Subscribe
    public void onPluginMessage(final PluginMessageEvent event) {
        if (!event.getIdentifier().getId().contains("loritime:")) {
            return;
        }
        log.debug("Received PluginMessage from Channel: " + event.getIdentifier().getId());
        if (!(event.getSource() instanceof ServerConnection)) {
            log.debug("Received PluginMessage from a non ServerConnection");
            return;
        }
        if (event.getResult().equals(PluginMessageEvent.ForwardResult.handled())) {
            log.debug("Received PluginMessage was already handled");
            return;
        }
        log.debug("Received PluginMessage was not handled yet, continue processing");
        final byte[] data = event.getData();
        processPluginMessage(event.getIdentifier().getId(), data);
        event.setResult(PluginMessageEvent.ForwardResult.handled());
    }

    @Override
    public void sendPluginMessage(final String channelIdentifier, final Object... message) {
        final ServerConnection connection = getConnection(message);
        if (connection == null) {
            log.debug("Could not find a connection to send the PluginMessage");
            return;
        }

        final MinecraftChannelIdentifier identifier = MinecraftChannelIdentifier.from(channelIdentifier);
        final byte[] data = getDataAsByte(message);
        log.debug("Sending PluginMessage with channel: " + channelIdentifier);
        connection.sendPluginMessage(identifier, data);
    }

    @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
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
