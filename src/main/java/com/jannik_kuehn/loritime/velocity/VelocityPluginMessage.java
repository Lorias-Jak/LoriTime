package com.jannik_kuehn.loritime.velocity;

import com.jannik_kuehn.loritime.api.LoriTimePlayer;
import com.jannik_kuehn.loritime.common.LoriTimePlugin;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class VelocityPluginMessage {

    private final LoriTimeVelocity velocityPlugin;
    private final LoriTimePlugin plugin;

    public VelocityPluginMessage(LoriTimeVelocity velocityPlugin) {
        this.velocityPlugin = velocityPlugin;
        this.plugin = velocityPlugin.getPlugin();
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().getId().contains("loritime:")) {
            return;
        }
        if (!(event.getTarget() instanceof Player player)) {
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
