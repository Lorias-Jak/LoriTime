package com.jannik_kuehn.loritimevelocity.util;

import com.jannik_kuehn.common.api.LoriTimePlayer;
import com.jannik_kuehn.common.api.common.CommonSender;
import com.jannik_kuehn.common.api.common.CommonServer;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.TextComponent;

import java.util.Optional;
import java.util.UUID;

public class VelocityServer implements CommonServer {
    private ProxyServer server;

    private String serverMode;

    public VelocityServer() {
        // Empty
    }

    public void enable(ProxyServer server) {
        this.server = server;
    }

    @Override
    public Optional<CommonSender> getPlayer(UUID uniqueId) {
        Optional<Player> player = server.getPlayer(uniqueId);
        return Optional.ofNullable(player.map(VelocityPlayer::new).orElse(null));
    }

    @Override
    public Optional<CommonSender> getPlayer(String name) {
        Optional<Player> player = server.getPlayer(name);
        return Optional.ofNullable(player.map(VelocityPlayer::new).orElse(null));
    }

    @Override
    public CommonSender[] getOnlinePlayers() {
        return server.getAllPlayers().stream()
                .map(VelocityPlayer::new)
                .toList().toArray(new VelocityPlayer[0]);
    }

    @Override
    public boolean dispatchCommand(CommonSender sender, String command) {
        CommandSource commandSource;
        if (sender.isConsole()) {
            commandSource = server.getConsoleCommandSource();
        } else {
            if (server.getPlayer(sender.getName()).isPresent()) {
                commandSource = server.getPlayer(sender.getName()).get();
            } else {
                return false;
            }
        }
        server.getCommandManager().executeImmediatelyAsync(commandSource, command);
        return true;
    }

    @Override
    public String getServerVersion() {
        return server.getVersion().getName() + " -> " + server.getVersion().getVersion();
    }

    @Override
    public boolean isProxy() {
        return true;
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
    public void kickPlayer(LoriTimePlayer loriTimePlayer, TextComponent message) {
        Optional<UUID> optionalUUID = Optional.ofNullable(loriTimePlayer.getUniqueId());
        if (optionalUUID.isEmpty()) {
            return;
        }
        Optional<Player> optionalPlayer = server.getPlayer(optionalUUID.get());
        if (optionalPlayer.isEmpty()) {
            return;
        }
        optionalPlayer.get().disconnect(message);
    }
}
