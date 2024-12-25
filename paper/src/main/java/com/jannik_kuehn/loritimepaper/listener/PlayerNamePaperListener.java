package com.jannik_kuehn.loritimepaper.listener;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.logger.LoriTimeLogger;
import com.jannik_kuehn.common.exception.StorageException;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

/**
 * Listener for saving player names and UUIDs.
 */
public class PlayerNamePaperListener implements Listener {
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
    public PlayerNamePaperListener(final LoriTimePlugin loriTimePlugin) {
        this.loriTimePlugin = loriTimePlugin;
        this.log = loriTimePlugin.getLoggerFactory().create(PlayerNamePaperListener.class);
    }

    /**
     * Saves the player name and UUID when a player joins the server.
     * It also does a lookup to check if the player name has changed.
     *
     * @param event The {@link PlayerJoinEvent} event.
     */
    @EventHandler
    public void playerJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();
        final String name = player.getName();
        loriTimePlugin.getScheduler().runAsyncOnce(() -> {
            try {
                loriTimePlugin.getNameStorage().setEntry(uuid, name, true);
            } catch (final StorageException e) {
                log.warn("could not save player name and uuid " + name, e);
            }
        });
    }
}
