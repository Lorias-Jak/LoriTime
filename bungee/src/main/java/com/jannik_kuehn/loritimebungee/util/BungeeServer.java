package com.jannik_kuehn.loritimebungee.util;

import com.jannik_kuehn.common.api.LoriTimePlayer;
import com.jannik_kuehn.common.api.common.CommonSender;
import com.jannik_kuehn.common.api.common.CommonServer;
import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

public class BungeeServer implements CommonServer {
    private final Logger bungeeLogger;

    private final ProxyServer proxyServer;

    private final String version;

    private final BungeeAudiences audiences;

    private String serverMode;

    public BungeeServer(final Logger bungeeLogger, final ProxyServer proxyServer, final String version, final BungeeAudiences audiences) {
        this.bungeeLogger = bungeeLogger;
        this.proxyServer = proxyServer;
        this.version = version;
        this.audiences = audiences;
    }

    @Override
    public Optional<CommonSender> getPlayer(final UUID uniqueId) {
        return Optional.of(new BungeePlayer(proxyServer.getPlayer(uniqueId)));
    }

    @Override
    public Optional<CommonSender> getPlayer(final String name) {
        return Optional.of(new BungeePlayer(proxyServer.getPlayer(name)));
    }

    @Override
    public CommonSender[] getOnlinePlayers() {
        return proxyServer.getPlayers().stream()
                .map(BungeePlayer::new)
                .toList().toArray(new BungeePlayer[0]);
    }

    @Override
    public boolean dispatchCommand(final CommonSender sender, final String command) {
        final CommandSender commandSource;
        if (sender.isConsole()) {
            commandSource = proxyServer.getConsole();
        } else {
            if (getPlayer(sender.getUniqueId()).isPresent()) {
                commandSource = proxyServer.getPlayer(sender.getUniqueId());
            } else {
                return false;
            }
        }
        proxyServer.getPluginManager().dispatchCommand(commandSource, command);
        return true;
    }

    @Override
    public String getServerVersion() {
        return proxyServer.getVersion();
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
    public void setServerMode(final String serverMode) {
        this.serverMode = serverMode;
    }

    @Override
    public void kickPlayer(final LoriTimePlayer player, final TextComponent message) {
        final Optional<UUID> optionalUUID = Optional.ofNullable(player.getUniqueId());
        if (optionalUUID.isEmpty()) {
            return;
        }
        final ProxiedPlayer proxiedPlayer = proxyServer.getPlayer(player.getUniqueId());
        proxiedPlayer.disconnect(BungeeComponentSerializer.get().serialize(message));
    }

    @Override
    public void sendMessageToConsole(final TextComponent message) {
        audiences.sender(proxyServer.getConsole()).sendMessage(message);
    }

    @Override
    public String getPluginVersion() {
        return version;
    }

    @Override
    public Logger getJavaLogger() {
        return bungeeLogger;
    }

    @Override
    public org.slf4j.Logger getSl4jLogger() {
        return null;
    }
}
