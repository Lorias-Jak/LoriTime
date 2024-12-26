package com.jannik_kuehn.loritimepaper.messenger;

import com.jannik_kuehn.common.api.logger.LoriTimeLogger;
import com.jannik_kuehn.common.module.messaging.PluginMessaging;
import com.jannik_kuehn.loritimepaper.LoriTimePaper;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Messenger for the plugin messages.
 * Extends the {@link PluginMessaging} class.
 */
public class PaperPluginMessenger extends PluginMessaging {
    /**
     * The {@link LoriTimePaper} instance.
     */
    private final LoriTimePaper paperPlugin;

    /**
     * The {@link LoriTimeLogger} instance.
     */
    private final LoriTimeLogger log;

    /**
     * Creates a new instance of the {@link PaperPluginMessenger}.
     *
     * @param paperPlugin The {@link LoriTimePaper} instance.
     */
    public PaperPluginMessenger(final LoriTimePaper paperPlugin) {
        super(paperPlugin.getPlugin());
        this.paperPlugin = paperPlugin;
        this.log = paperPlugin.getPlugin().getLoggerFactory().create(PaperPluginMessenger.class, "PaperPluginMessenger");
    }

    @Override
    public void sendPluginMessage(final String channelIdentifier, final Object... message) {
        log.debug("Sending PluginMessage with channel: " + channelIdentifier);
        final UUID uuid = (UUID) message[0];
        final byte[] data = getDataAsByte(message);

        if (data != null) {
            final Player paperPlayer = paperPlugin.getServer().getPlayer(uuid);
            if (paperPlayer != null) {
                log.debug("Sending PluginMessage to player: " + paperPlayer.getName());
                paperPlayer.sendPluginMessage(paperPlugin, channelIdentifier, data);
            }
        } else {
            log.warn("could not send plugin message, data is null");
        }
    }
}
