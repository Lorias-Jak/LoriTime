package com.jannik_kuehn.loritimepaper.util;

import com.jannik_kuehn.common.api.LoriTimePlayer;
import com.jannik_kuehn.common.platform.CommonConsoleSender;
import com.jannik_kuehn.common.platform.CommonPlayerSender;
import com.jannik_kuehn.common.platform.CommonSender;
import com.jannik_kuehn.common.platform.CommonServer;
import com.jannik_kuehn.common.storage.model.SessionContextDefaults;
import com.jannik_kuehn.loritimepaper.LoriTimePaper;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Paper implementation of the shared server abstraction.
 */
public class PaperServer implements CommonServer {
    private final LoriTimePaper loriTimePaper;

    private final String version;

    private final Server server;

    private String serverMode;

    /**
     * Creates a Paper server adapter.
     *
     * @param loriTimePaper Paper plugin bootstrap
     * @param version LoriTime plugin version
     */
    public PaperServer(final LoriTimePaper loriTimePaper, final String version) {
        this.loriTimePaper = loriTimePaper;
        this.server = Bukkit.getServer();
        this.version = version;
    }

    /**
     * Looks up an online Paper player by UUID.
     *
     * @param uniqueId player UUID
     * @return player sender when online
     */
    @Override
    public Optional<CommonPlayerSender> getPlayer(final UUID uniqueId) {
        final Optional<Player> player = Optional.ofNullable(server.getPlayer(uniqueId));
        return Optional.ofNullable(player.map(PaperPlayer::new).orElse(null));
    }

    /**
     * Looks up an online Paper player by name.
     *
     * @param name player name
     * @return player sender when online
     */
    @Override
    public Optional<CommonPlayerSender> getPlayer(final String name) {
        final Optional<Player> player = Optional.ofNullable(server.getPlayer(name));
        return Optional.ofNullable(player.map(PaperPlayer::new).orElse(null));
    }

    /**
     * Returns online Paper players.
     *
     * @return online player senders
     */
    @Override
    public CommonPlayerSender[] getOnlinePlayers() {
        return server.getOnlinePlayers().stream()
                .map(PaperPlayer::new)
                .toList().toArray(new CommonPlayerSender[0]);
    }

    /**
     * Returns the local server name for a player.
     *
     * @param uniqueId player UUID
     * @return current server name
     */
    @Override
    public Optional<String> getCurrentServer(final UUID uniqueId) {
        return getLocalServerName();
    }

    /**
     * Returns the configured local server name.
     *
     * @return local server name
     */
    @Override
    public Optional<String> getLocalServerName() {
        return Optional.of(loriTimePaper.getPlugin().getConfig()
                .getString("server.name", SessionContextDefaults.SERVER));
    }

    /**
     * Returns the local server name as the only live server on Paper.
     *
     * @return live server names
     */
    @Override
    public List<String> getLiveServerNames() {
        return getLocalServerName().stream().toList();
    }

    /**
     * Returns live world names from the Paper server.
     *
     * @param serverName optional server filter
     * @param uniqueId optional player UUID filter
     * @return live world names
     */
    @Override
    public List<String> getLiveWorldNames(final Optional<String> serverName, final Optional<UUID> uniqueId) {
        if (serverName.isPresent() && getLocalServerName().filter(serverName.get()::equalsIgnoreCase).isEmpty()) {
            return List.of();
        }
        return server.getWorlds().stream()
                .map(World::getName)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    /**
     * Dispatches a command through Bukkit.
     *
     * @param consoleSender shared sender
     * @param command command line
     * @return true when dispatch was attempted
     */
    @Override
    public boolean dispatchCommand(final CommonSender consoleSender, final String command) {
        final CommandSender commandSource;
        if (consoleSender instanceof CommonConsoleSender) {
            commandSource = server.getConsoleSender();
        } else if (consoleSender instanceof CommonPlayerSender playerSender) {
            commandSource = server.getPlayer(playerSender.getUniqueId());
        } else {
            return false;
        }
        if (commandSource == null) {
            return false;
        }
        server.dispatchCommand(commandSource, command);
        return true;
    }

    /**
     * Returns the Bukkit server version.
     *
     * @return server version
     */
    @Override
    public String getServerVersion() {
        return server.getVersion();
    }

    /**
     * Returns whether this Paper instance is a proxy.
     *
     * @return false for Paper
     */
    @Override
    public boolean isProxy() {
        return false;
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
     * Kicks an online Paper player.
     *
     * @param loriTimePlayer player identity
     * @param message kick message
     */
    @Override
    public void kickPlayer(final LoriTimePlayer loriTimePlayer, final Component message) {
        final Optional<UUID> optionalUUID = Optional.ofNullable(loriTimePlayer.getUniqueId());
        if (optionalUUID.isEmpty()) {
            return;
        }
        final Player player = Bukkit.getServer().getPlayer(optionalUUID.get());
        if (player == null) {
            return;
        }
        loriTimePaper.getPlugin().getScheduler().scheduleSync(() -> kickPlayer(player, message));
    }

    /**
     * Sends a message to the Paper console.
     *
     * @param message console message
     */
    @Override
    public void sendMessageToConsole(final Component message) {
        server.sendMessage(message);
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
     * Returns the Paper artifact name.
     *
     * @return plugin jar name
     */
    @Override
    public String getPluginJarName() {
        return "LoriTimePaper.jar";
    }

    private void kickPlayer(final Player player, final Component message) {
        player.kick(message);
    }

}
