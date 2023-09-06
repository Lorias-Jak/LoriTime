package com.jannik_kuehn.loritime.velocity.listener;

import com.jannik_kuehn.loritime.common.LoriTimePlugin;
import com.jannik_kuehn.loritime.common.exception.StorageException;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;

import java.util.UUID;

public class PlayerNameVelocityListener {
    private final LoriTimePlugin plugin;
    public PlayerNameVelocityListener(LoriTimePlugin plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void postLogin(PostLoginEvent event) {
        Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();
        final String name = player.getUsername();
        plugin.getScheduler().runAsyncOnce(() -> {
            try {
                plugin.getNameStorage().setEntry(uuid, name);
            } catch (StorageException ex) {
                plugin.getLogger().warning("could not save player name and uuid " + name, ex);
            }
        });
    }
}
