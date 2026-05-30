package com.jannik_kuehn.loritimebungee.util;

import com.jannik_kuehn.common.api.LoriTimePlayer;
import com.jannik_kuehn.common.api.common.CommonConsoleSender;
import com.jannik_kuehn.common.api.common.CommonPlayerSender;
import com.jannik_kuehn.common.api.common.CommonSender;
import com.jannik_kuehn.common.api.common.CommonServer;
import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("PMD.CommentRequired")
public class BungeeServer implements CommonServer {

    private final ProxyServer proxyServer;

    private final String version;

    private final BungeeAudiences audiences;

    private String serverMode;

    public BungeeServer(final ProxyServer proxyServer, final String version, final BungeeAudiences audiences) {
        this.proxyServer = proxyServer;
        this.version = version;
        this.audiences = audiences;
    }

    @Override
    public Optional<CommonPlayerSender> getPlayer(final UUID uniqueId) {
        final ProxiedPlayer player = proxyServer.getPlayer(uniqueId);
        if (player == null) {
            return Optional.empty();
        }
        return Optional.of(new BungeePlayer(player));
    }

    @Override
    public Optional<CommonPlayerSender> getPlayer(final String name) {
        final ProxiedPlayer player = proxyServer.getPlayer(name);
        if (player == null) {
            return Optional.empty();
        }
        return Optional.of(new BungeePlayer(player));
    }

    @Override
    public CommonPlayerSender[] getOnlinePlayers() {
        return proxyServer.getPlayers().stream()
                .map(BungeePlayer::new)
                .toList().toArray(new CommonPlayerSender[0]);
    }

    @Override
    public Optional<String> getCurrentServer(final UUID uniqueId) {
        final ProxiedPlayer player = proxyServer.getPlayer(uniqueId);
        if (player == null || player.getServer() == null) {
            return Optional.empty();
        }
        return Optional.of(player.getServer().getInfo().getName());
    }

    @Override
    public List<String> getLiveServerNames() {
        return proxyServer.getServers().keySet().stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    @Override
    public boolean dispatchCommand(final CommonSender sender, final String command) {
        final CommandSender commandSource;
        if (sender instanceof CommonConsoleSender) {
            commandSource = proxyServer.getConsole();
        } else if (sender instanceof CommonPlayerSender playerSender) {
            commandSource = proxyServer.getPlayer(playerSender.getUniqueId());
        } else {
            return false;
        }
        if (commandSource == null) {
            return false;
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
    public void kickPlayer(final LoriTimePlayer player, final TextComponent message) {
        final Optional<UUID> optionalUUID = Optional.ofNullable(player.getUniqueId());
        if (optionalUUID.isEmpty()) {
            return;
        }
        final ProxiedPlayer proxiedPlayer = proxyServer.getPlayer(player.getUniqueId());
        proxiedPlayer.disconnect(BungeeComponentSerializer.get().serialize(message));
    }

    @Override
    public String getPluginVersion() {
        return version;
    }

    @Override
    public String getPluginJarName() {
        return "LoriTimeBungee.jar";
    }

    @Override
    public void sendMessageToConsole(final TextComponent message) {
        audiences.sender(proxyServer.getConsole()).sendMessage(message);
    }

}
