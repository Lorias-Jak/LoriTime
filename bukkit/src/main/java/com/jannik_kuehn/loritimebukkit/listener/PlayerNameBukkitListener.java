package com.jannik_kuehn.loritimebukkit.listener;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.logger.LoriTimeLogger;
import com.jannik_kuehn.common.exception.StorageException;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

public class PlayerNameBukkitListener implements Listener {

    private final LoriTimePlugin plugin;

    private final LoriTimeLogger log;

    public PlayerNameBukkitListener(final LoriTimePlugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLoggerFactory().create(PlayerNameBukkitListener.class);
    }

    @EventHandler
    public void playerJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();
        final String name = player.getName();
        plugin.getScheduler().runAsyncOnce(() -> {
            try {
                plugin.getNameStorage().setEntry(uuid, name, true);
            } catch (final StorageException e) {
                log.warn("could not save player name and uuid " + name, e);
            }
        });
    }
}
