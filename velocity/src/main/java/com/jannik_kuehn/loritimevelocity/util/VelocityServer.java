package com.jannik_kuehn.loritimevelocity.util;

import com.jannik_kuehn.common.api.LoriTimePlayer;
import com.jannik_kuehn.common.api.common.CommonSender;
import com.jannik_kuehn.common.api.common.CommonServer;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.TextComponent;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("PMD.CommentRequired")
public class VelocityServer implements CommonServer {
    private final Logger logger;

    private ProxyServer server;

    private String version;

    private String serverMode;

    public VelocityServer(final Logger logger) {
        this.logger = logger;
    }

    public void enable(final ProxyServer server, final String version) {
        this.server = server;
        this.version = version;
    }

    @Override
    public Optional<CommonSender> getPlayer(final UUID uniqueId) {
        final Optional<Player> player = server.getPlayer(uniqueId);
        return Optional.ofNullable(player.map(VelocityPlayer::new).orElse(null));
    }

    @Override
    public Optional<CommonSender> getPlayer(final String name) {
        final Optional<Player> player = server.getPlayer(name);
        return Optional.ofNullable(player.map(VelocityPlayer::new).orElse(null));
    }

    @Override
    public CommonSender[] getOnlinePlayers() {
        return server.getAllPlayers().stream()
                .map(VelocityPlayer::new)
                .toList().toArray(new VelocityPlayer[0]);
    }

    @Override
    public boolean dispatchCommand(final CommonSender sender, final String command) {
        final CommandSource commandSource;
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
    public void setServerMode(final String serverMode) {
        this.serverMode = serverMode;
    }

    @Override
    public void kickPlayer(final LoriTimePlayer loriTimePlayer, final TextComponent message) {
        final Optional<UUID> optionalUUID = Optional.ofNullable(loriTimePlayer.getUniqueId());
        if (optionalUUID.isEmpty()) {
            return;
        }
        final Optional<Player> optionalPlayer = server.getPlayer(optionalUUID.get());
        if (optionalPlayer.isEmpty()) {
            return;
        }
        optionalPlayer.get().disconnect(message);
    }

    @Override
    public void sendMessageToConsole(final TextComponent message) {
        server.getConsoleCommandSource().sendMessage(message);
    }

    @Override
    public String getPluginVersion() {
        return version;
    }

    @Override
    public String getPluginJarName() {
        return "LoriTimeVelocity.jar";
    }

    @Override
    public java.util.logging.Logger getJavaLogger() {
        return null;
    }

    @Override
    public Logger getSl4jLogger() {
        return logger;
    }
}
