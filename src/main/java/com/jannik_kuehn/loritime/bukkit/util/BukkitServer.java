package com.jannik_kuehn.loritime.bukkit.util;

import com.jannik_kuehn.loritime.api.CommonSender;
import com.jannik_kuehn.loritime.api.CommonServer;
import com.jannik_kuehn.loritime.common.LoriTimePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

public class BukkitServer implements CommonServer {
    private LoriTimePlugin plugin;
    private Server server;

    public void enable(LoriTimePlugin plugin, Server server) {
        this.plugin = plugin;
        this.server = server;
    }

    @Override
    public Optional<CommonSender> getPlayer(UUID uniqueId) {
        Optional<Player> player = Optional.ofNullable(server.getPlayer(uniqueId));
        return Optional.ofNullable(player.map(BukkitPlayer::new).orElse(null));
    }

    @Override
    public Optional<CommonSender> getPlayer(String name) {
        Optional<Player> player = Optional.ofNullable(server.getPlayer(name));
        return Optional.ofNullable(player.map(BukkitPlayer::new).orElse(null));
    }

    @Override
    public CommonSender[] getOnlinePlayers() {
        return server.getOnlinePlayers().stream()
                .map(BukkitPlayer::new)
                .toList().toArray(new BukkitPlayer[0]);
    }

    @Override
    public boolean dispatchCommand(CommonSender consoleSender, String command) {
        CommandSender commandSource;
        if (consoleSender.isConsole()) {
            commandSource = server.getConsoleSender();
        } else {
            Player player = server.getPlayer(consoleSender.getName());
            if (server.getPlayer(consoleSender.getName()) != null) {
                commandSource = player;
            } else {
                return false;
            }
        }
        server.dispatchCommand(commandSource, command);
        return true;
    }

    @Override
    public String getServerVersion() {
        return Bukkit.getVersion();
    }
}
