package com.jannik_kuehn.loritimevelocity.listener;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.logger.LoriTimeLogger;
import com.jannik_kuehn.common.exception.StorageException;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;

import java.util.UUID;

/**
 * Listener for saving player names and UUIDs.
 */
public class PlayerNameVelocityListener {
    /**
     * The {@link LoriTimePlugin} instance.
     */
    private final LoriTimePlugin loriTimePlugin;

    /**
     * The {@link LoriTimeLogger} instance.
     */
    private final LoriTimeLogger log;

    /**
     * The default constructor.
     *
     * @param loriTimePlugin The {@link LoriTimePlugin} instance.
     */
    public PlayerNameVelocityListener(final LoriTimePlugin loriTimePlugin) {
        this.loriTimePlugin = loriTimePlugin;
        this.log = loriTimePlugin.getLoggerFactory().create(PlayerNameVelocityListener.class);
    }

    /**
     * Saves the player name and UUID when a player joins the server.
     * It also does a lookup to check if the player name has changed.
     *
     * @param event The {@link PostLoginEvent} event.
     */
    @Subscribe
    public void postLogin(final PostLoginEvent event) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();
        final String name = player.getUsername();
        loriTimePlugin.getScheduler().runAsyncOnce(() -> {
            try {
                loriTimePlugin.getNameStorage().setEntry(uuid, name, true);
            } catch (final StorageException ex) {
                log.warn("could not save player name and uuid " + name, ex);
            }
        });
    }
}
