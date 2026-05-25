package com.jannik_kuehn.loritimevelocity.util;

import com.jannik_kuehn.common.api.LoriTimePlayer;
import com.jannik_kuehn.common.api.common.CommonConsoleSender;
import com.jannik_kuehn.common.api.common.CommonPlayerSender;
import com.jannik_kuehn.common.api.common.CommonSender;
import com.jannik_kuehn.common.api.common.CommonServer;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.TextComponent;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("PMD.CommentRequired")
public class VelocityServer implements CommonServer {

    private ProxyServer server;

    private String version;

    private String serverMode;

    public VelocityServer() {
        // Empty
    }

    public void enable(final ProxyServer server, final String version) {
        this.server = server;
        this.version = version;
    }

    @Override
    public Optional<CommonPlayerSender> getPlayer(final UUID uniqueId) {
        final Optional<Player> player = server.getPlayer(uniqueId);
        return Optional.ofNullable(player.map(VelocityPlayer::new).orElse(null));
    }

    @Override
    public Optional<CommonPlayerSender> getPlayer(final String name) {
        final Optional<Player> player = server.getPlayer(name);
        return Optional.ofNullable(player.map(VelocityPlayer::new).orElse(null));
    }

    @Override
    public CommonPlayerSender[] getOnlinePlayers() {
        return server.getAllPlayers().stream()
                .map(VelocityPlayer::new)
                .toList().toArray(new CommonPlayerSender[0]);
    }

    @Override
    public Optional<String> getCurrentServer(final UUID uniqueId) {
        return server.getPlayer(uniqueId)
                .flatMap(Player::getCurrentServer)
                .map(connection -> connection.getServerInfo().getName());
    }

    @Override
    public List<String> getLiveServerNames() {
        return server.getAllServers().stream()
                .map(registeredServer -> registeredServer.getServerInfo().getName())
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    @Override
    public boolean dispatchCommand(final CommonSender sender, final String command) {
        final CommandSource commandSource;
        if (sender instanceof CommonConsoleSender) {
            commandSource = server.getConsoleCommandSource();
        } else if (sender instanceof CommonPlayerSender playerSender) {
            final Optional<Player> optionalPlayer = server.getPlayer(playerSender.getUniqueId());
            if (optionalPlayer.isEmpty()) {
                return false;
            }
            commandSource = optionalPlayer.get();
        } else {
            return false;
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

}
