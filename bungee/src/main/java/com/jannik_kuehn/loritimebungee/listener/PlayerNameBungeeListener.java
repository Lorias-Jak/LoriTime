package com.jannik_kuehn.loritimebungee.listener;

import com.jannik_kuehn.common.LoriTimePlugin;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.UUID;

public class PlayerNameBungeeListener implements Listener {

    private final LoriTimePlugin plugin;

    public PlayerNameBungeeListener(final LoriTimePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPostLogin(final PostLoginEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        final String name = event.getPlayer().getName();
        plugin.getScheduler().runAsyncOnce(() -> {
            try {
                plugin.getNameStorage().setEntry(uuid, name, true);
            } catch (final Exception ex) {
                plugin.getLogger().warning("could not save player name and uuid " + name, ex);
            }
        });
    }
}
