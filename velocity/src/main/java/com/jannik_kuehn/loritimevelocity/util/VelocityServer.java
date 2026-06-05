package com.jannik_kuehn.loritimevelocity.util;

import com.jannik_kuehn.common.api.LoriTimePlayer;
import com.jannik_kuehn.common.api.common.CommonConsoleSender;
import com.jannik_kuehn.common.api.common.CommonPlayerSender;
import com.jannik_kuehn.common.api.common.CommonSender;
import com.jannik_kuehn.common.api.common.CommonServer;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Velocity implementation of the shared server abstraction.
 */
public class VelocityServer implements CommonServer {

    private ProxyServer server;

    private String version;

    private String serverMode;

    /**
     * Creates an uninitialized Velocity server adapter.
     */
    public VelocityServer() {
        // Empty
    }

    /**
     * Attaches the Velocity proxy runtime to this adapter.
     *
     * @param server Velocity proxy server
     * @param version LoriTime plugin version
     */
    public void enable(final ProxyServer server, final String version) {
        this.server = server;
        this.version = version;
    }

    /**
     * Looks up an online Velocity player by UUID.
     *
     * @param uniqueId player UUID
     * @return player sender when online
     */
    @Override
    public Optional<CommonPlayerSender> getPlayer(final UUID uniqueId) {
        final Optional<Player> player = server.getPlayer(uniqueId);
        return Optional.ofNullable(player.map(VelocityPlayer::new).orElse(null));
    }

    /**
     * Looks up an online Velocity player by name.
     *
     * @param name player name
     * @return player sender when online
     */
    @Override
    public Optional<CommonPlayerSender> getPlayer(final String name) {
        final Optional<Player> player = server.getPlayer(name);
        return Optional.ofNullable(player.map(VelocityPlayer::new).orElse(null));
    }

    /**
     * Returns online Velocity players.
     *
     * @return online player senders
     */
    @Override
    public CommonPlayerSender[] getOnlinePlayers() {
        return server.getAllPlayers().stream()
                .map(VelocityPlayer::new)
                .toList().toArray(new CommonPlayerSender[0]);
    }

    /**
     * Returns the backend server a player is connected to.
     *
     * @param uniqueId player UUID
     * @return current server name
     */
    @Override
    public Optional<String> getCurrentServer(final UUID uniqueId) {
        return server.getPlayer(uniqueId)
                .flatMap(Player::getCurrentServer)
                .map(connection -> connection.getServerInfo().getName());
    }

    /**
     * Returns registered backend server names.
     *
     * @return live server names
     */
    @Override
    public List<String> getLiveServerNames() {
        return server.getAllServers().stream()
                .map(registeredServer -> registeredServer.getServerInfo().getName())
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    /**
     * Dispatches a command through Velocity.
     *
     * @param sender shared sender
     * @param command command line
     * @return true when dispatch was attempted
     */
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

    /**
     * Returns the Velocity server version.
     *
     * @return server version
     */
    @Override
    public String getServerVersion() {
        return server.getVersion().getName() + " -> " + server.getVersion().getVersion();
    }

    /**
     * Returns whether this instance is a proxy.
     *
     * @return true for Velocity
     */
    @Override
    public boolean isProxy() {
        return true;
    }

    /**
     * Returns the configured LoriTime server mode.
     *
     * @return server mode
     */
    @Override
    public String getServerMode() {
        return serverMode;
    }

    /**
     * Updates the configured LoriTime server mode.
     *
     * @param serverMode server mode
     */
    @Override
    public void setServerMode(final String serverMode) {
        this.serverMode = serverMode;
    }

    /**
     * Disconnects an online Velocity player.
     *
     * @param loriTimePlayer player identity
     * @param message disconnect message
     */
    @Override
    public void kickPlayer(final LoriTimePlayer loriTimePlayer, final Component message) {
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

    /**
     * Sends a message to the Velocity console.
     *
     * @param message console message
     */
    @Override
    public void sendMessageToConsole(final Component message) {
        server.getConsoleCommandSource().sendMessage(message);
    }

    /**
     * Returns the LoriTime plugin version.
     *
     * @return plugin version
     */
    @Override
    public String getPluginVersion() {
        return version;
    }

    /**
     * Returns the Velocity artifact name.
     *
     * @return plugin jar name
     */
    @Override
    public String getPluginJarName() {
        return "LoriTimeVelocity.jar";
    }

}
