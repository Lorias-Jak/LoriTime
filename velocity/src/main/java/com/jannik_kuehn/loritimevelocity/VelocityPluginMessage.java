package com.jannik_kuehn.loritimevelocity;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.LoriTimePlayer;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class VelocityPluginMessage {
    private final LoriTimePlugin plugin;

    public VelocityPluginMessage(final LoriTimeVelocity velocityPlugin) {
        this.plugin = velocityPlugin.getPlugin();
    }

    @Subscribe
    public void onPluginMessage(final PluginMessageEvent event) {
        if (!event.getIdentifier().getId().contains("loritime:")) {
            return;
        }
        if (!(event.getTarget() instanceof final Player player)) {
            return;
        }
        if (event.getResult().equals(PluginMessageEvent.ForwardResult.handled())) {
            return;
        }

        final byte[] data = event.getData();
        final LoriTimePlayer loriTimePlayer = new LoriTimePlayer(player.getUniqueId(), player.getUsername());

        plugin.getScheduler().runAsyncOnce(() -> {
            if (event.getIdentifier().getId().equals("loritime:afk")) {
                setAfkStatus(data, loriTimePlayer);
            }
        });
        event.setResult(PluginMessageEvent.ForwardResult.handled());
    }

    private void setAfkStatus(final byte[] data, final LoriTimePlayer player) {
        try (ByteArrayInputStream byteInputStream = new ByteArrayInputStream(data);
             DataInputStream input = new DataInputStream(byteInputStream)) {

            switch (input.readUTF()) {
                case "true":
                    plugin.getAfkStatusProvider().getAfkPlayerHandling().executePlayerAfk(player, input.readLong());
                    break;
                case "false":
                    plugin.getAfkStatusProvider().getAfkPlayerHandling().executePlayerResume(player);
                    break;
                default:
                    plugin.getLogger().warning("received invalid afk status!");
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

}
