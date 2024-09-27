package com.jannik_kuehn.loritimebungee.listener;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.logger.LoriTimeLogger;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.UUID;

public class PlayerNameBungeeListener implements Listener {

    private final LoriTimePlugin plugin;

    private final LoriTimeLogger log;

    public PlayerNameBungeeListener(final LoriTimePlugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLoggerFactory().create(PlayerNameBungeeListener.class);
    }

    @EventHandler
    public void onPostLogin(final PostLoginEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        final String name = event.getPlayer().getName();
        plugin.getScheduler().runAsyncOnce(() -> {
            try {
                plugin.getNameStorage().setEntry(uuid, name, true);
            } catch (final Exception ex) {
                log.warn("could not save player name and uuid " + name, ex);
            }
        });
    }
}
