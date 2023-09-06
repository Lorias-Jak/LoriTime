package com.jannik_kuehn.loritime.velocity.util;

import com.jannik_kuehn.loritime.common.LoriTimePlugin;
import com.jannik_kuehn.loritime.api.CommonSender;
import com.jannik_kuehn.loritime.api.CommonServer;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import java.util.Optional;
import java.util.UUID;

public class VelocityServer implements CommonServer {
    private LoriTimePlugin plugin;
    private ProxyServer server;

    public VelocityServer() {
    }

    public void enable(LoriTimePlugin plugin, ProxyServer server) {
        this.plugin = plugin;
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

    public CommandManager getCommandManager() {
        return server.getCommandManager();
    }
}