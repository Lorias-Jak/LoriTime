package com.jannik_kuehn.loritime.bungee;

import com.jannik_kuehn.loritime.api.LoriTimePlayer;
import com.jannik_kuehn.loritime.common.LoriTimePlugin;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class BungeePluginMessanger implements Listener {

    private final LoriTimeBungee bungeePlugin;
    private final LoriTimePlugin plugin;

    public BungeePluginMessanger(LoriTimeBungee bungeePlugin) {
        this.bungeePlugin = bungeePlugin;
        this.plugin = bungeePlugin.getPlugin();
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getTag().contains("loritime:")) {
            return;
        }
        if (!(event.getReceiver() instanceof ProxiedPlayer player)) {
            return;
        }
        if (event.isCancelled()) {
            return;
        }

        final byte[] data = event.getData();
        final LoriTimePlayer loriTimePlayer = new LoriTimePlayer(player.getUniqueId(), player.getName());
        plugin.getScheduler().runAsyncOnce(() -> {
            if (event.getTag().equals("loritime:afk")) {
                setAfkStatus(data, loriTimePlayer);
            }
        });
    }

    private void setAfkStatus(byte[] data, LoriTimePlayer player) {
        try (ByteArrayInputStream byteInputStream = new ByteArrayInputStream(data);
             DataInputStream input = new DataInputStream(byteInputStream)) {

            switch(input.readUTF()) {
                case "true":
                    plugin.getAfkStatusProvider().getAfkPlayerHandling().executePlayerAfk(player, input.readLong());
                    break;
                case "false":
                    plugin.getAfkStatusProvider().getAfkPlayerHandling().executePlayerResume(player);
                    break;
                default:
                    plugin.getLogger().warning("received invalid afk status!");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
