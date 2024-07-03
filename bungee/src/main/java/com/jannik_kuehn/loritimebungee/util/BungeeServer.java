package com.jannik_kuehn.loritimebungee.util;

import com.jannik_kuehn.common.api.LoriTimePlayer;
import com.jannik_kuehn.common.api.common.CommonSender;
import com.jannik_kuehn.common.api.common.CommonServer;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.Optional;
import java.util.UUID;

public class BungeeServer implements CommonServer {

    private ProxyServer server;

    private String serverMode;

    public BungeeServer() {
        // Empty
    }

    public void enable(ProxyServer server) {
        this.server = server;
    }

    @Override
    public Optional<CommonSender> getPlayer(UUID uniqueId) {
        return Optional.of(new BungeePlayer(server.getPlayer(uniqueId)));
    }

    @Override
    public Optional<CommonSender> getPlayer(String name) {
        return Optional.of(new BungeePlayer(server.getPlayer(name)));
    }

    @Override
    public CommonSender[] getOnlinePlayers() {
        return server.getPlayers().stream()
                .map(BungeePlayer::new)
                .toList().toArray(new BungeePlayer[0]);
    }

    @Override
    public boolean dispatchCommand(CommonSender sender, String command) {
        CommandSender commandSource;
        if (sender.isConsole()) {
            commandSource = server.getConsole();
        } else {
            if (getPlayer(sender.getUniqueId()).isPresent()) {
                commandSource = server.getPlayer(sender.getUniqueId());
            } else {
                return false;
            }
        }
        server.getPluginManager().dispatchCommand(commandSource, command);
        return true;
    }

    @Override
    public String getServerVersion() {
        return server.getVersion();
    }

    @Override
    public boolean isProxy() {
        return false;
    }

    @Override
    public String getServerMode() {
        return serverMode;
    }

    @Override
    public void setServerMode(String serverMode) {
        this.serverMode = serverMode;
    }

    @Override
    public void kickPlayer(LoriTimePlayer player, TextComponent message) {
        Optional<UUID> optionalUUID = Optional.ofNullable(player.getUniqueId());
        if (optionalUUID.isEmpty()) {
            return;
        }
        ProxiedPlayer proxiedPlayer = server.getPlayer(player.getUniqueId());
        proxiedPlayer.disconnect(BungeeComponentSerializer.get().serialize(message));
    }
}
