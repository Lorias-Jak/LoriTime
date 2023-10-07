package com.jannik_kuehn.loritime.bungee.listener;

import com.jannik_kuehn.loritime.common.LoriTimePlugin;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.UUID;

public class PlayerNameBungeeListener implements Listener {

    private final LoriTimePlugin plugin;

    public PlayerNameBungeeListener(LoriTimePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        final String name = event.getPlayer().getName();
        plugin.getScheduler().runAsyncOnce(() -> {
            try {
                plugin.getNameStorage().setEntry(uuid, name, true);
            } catch (Exception ex) {
                plugin.getLogger().warning("could not save player name and uuid " + name, ex);
            }
        });
    }
}
