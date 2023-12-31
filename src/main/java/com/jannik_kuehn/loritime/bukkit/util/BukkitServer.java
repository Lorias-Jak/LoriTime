package com.jannik_kuehn.loritime.bukkit.util;

import com.jannik_kuehn.loritime.api.CommonSender;
import com.jannik_kuehn.loritime.api.CommonServer;
import com.jannik_kuehn.loritime.api.LoriTimePlayer;
import com.jannik_kuehn.loritime.bukkit.BukkitPluginMessanger;
import com.jannik_kuehn.loritime.bukkit.LoriTimeBukkit;
import com.jannik_kuehn.loritime.common.LoriTimePlugin;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

public class BukkitServer implements CommonServer {

    private LoriTimeBukkit bukkitPlugin;
    private LoriTimePlugin loriTimePlugin;
    private String serverMode;
    private Server server;
    private BukkitPluginMessanger pluginMessanger;

    public BukkitServer() {
        // Empty
    }

    public void enable(LoriTimeBukkit bukkitPlugin) {
        this.bukkitPlugin = bukkitPlugin;
        this.loriTimePlugin = bukkitPlugin.getLoriTimePlugin();
        this.server = Bukkit.getServer();
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
    public void kickPlayer(LoriTimePlayer loriTimePlayer, TextComponent message) {
        Optional<UUID> optionalUUID = Optional.ofNullable(loriTimePlayer.getUniqueId());
        if (optionalUUID.isEmpty()) {
            return;
        }
        Player player = Bukkit.getServer().getPlayer(optionalUUID.get());
        if (player == null) {
            return;
        }
        loriTimePlugin.getScheduler().scheduleSync(() -> kickPlayer(player, message));
    }

    private void kickPlayer(Player player, TextComponent message) {
        player.kick(message);
    }
}
