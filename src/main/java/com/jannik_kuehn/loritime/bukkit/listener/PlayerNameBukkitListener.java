package com.jannik_kuehn.loritime.bukkit.listener;

import com.jannik_kuehn.loritime.common.LoriTimePlugin;
import com.jannik_kuehn.loritime.common.exception.StorageException;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

public class PlayerNameBukkitListener implements Listener {

    private final LoriTimePlugin plugin;

    public PlayerNameBukkitListener(LoriTimePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void playerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();
        final String name = player.getName();
        plugin.getScheduler().runAsyncOnce(() -> {
            try {
                plugin.getNameStorage().setEntry(uuid, name);
            } catch (StorageException e) {
                plugin.getLogger().warning("could not save player name and uuid " + name, e);
            }
        });
    }
}
