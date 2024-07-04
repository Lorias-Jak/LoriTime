package com.jannik_kuehn.loritimevelocity.listener;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.exception.StorageException;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;

import java.util.UUID;

public class PlayerNameVelocityListener {
    private final LoriTimePlugin plugin;

    public PlayerNameVelocityListener(final LoriTimePlugin plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void postLogin(final PostLoginEvent event) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();
        final String name = player.getUsername();
        plugin.getScheduler().runAsyncOnce(() -> {
            try {
                plugin.getNameStorage().setEntry(uuid, name, true);
            } catch (final StorageException ex) {
                plugin.getLogger().warning("could not save player name and uuid " + name, ex);
            }
        });
    }
}
