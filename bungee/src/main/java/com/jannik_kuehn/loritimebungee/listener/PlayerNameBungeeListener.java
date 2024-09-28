package com.jannik_kuehn.loritimebungee.listener;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.logger.LoriTimeLogger;
import com.jannik_kuehn.common.exception.StorageException;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.UUID;

/**
 * Listener for saving player names and UUIDs.
 */
public class PlayerNameBungeeListener implements Listener {
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
    public PlayerNameBungeeListener(final LoriTimePlugin loriTimePlugin) {
        this.loriTimePlugin = loriTimePlugin;
        this.log = loriTimePlugin.getLoggerFactory().create(PlayerNameBungeeListener.class);
    }

    /**
     * Saves the player name and UUID when a player joins the server.
     * It also does a lookup to check if the player name has changed.
     *
     * @param event The {@link PostLoginEvent} event.
     */
    @EventHandler
    public void onPostLogin(final PostLoginEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        final String name = event.getPlayer().getName();
        loriTimePlugin.getScheduler().runAsyncOnce(() -> {
            try {
                loriTimePlugin.getNameStorage().setEntry(uuid, name, true);
            } catch (final StorageException ex) {
                log.warn("could not save player name and uuid " + name, ex);
            }
        });
    }
}
