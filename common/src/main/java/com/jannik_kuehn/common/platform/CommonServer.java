package com.jannik_kuehn.common.platform;

import com.jannik_kuehn.common.api.LoriTimePlayer;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Platform-neutral server operations used by shared LoriTime code.
 */
public interface CommonServer {

    /**
     * Looks up an online player by UUID.
     *
     * @param uniqueId player UUID
     * @return player sender when online and visible to this server
     */
    Optional<CommonPlayerSender> getPlayer(UUID uniqueId);

    /**
     * Looks up an online player by name.
     *
     * @param name player name
     * @return player sender when online and visible to this server
     */
    Optional<CommonPlayerSender> getPlayer(String name);

    /**
     * Returns online players visible to this server.
     *
     * @return online players
     */
    CommonPlayerSender[] getOnlinePlayers();

    /**
     * Returns the current logical server for a player when known.
     *
     * @param uniqueId player UUID
     * @return current server name
     */
    default Optional<String> getCurrentServer(final UUID uniqueId) {
        return Optional.empty();
    }

    /**
     * Returns this instance's logical server name when configured.
     *
     * @return local server name
     */
    default Optional<String> getLocalServerName() {
        return Optional.empty();
    }

    /**
     * Returns live server names known by the platform.
     *
     * @return live server names
     */
    default List<String> getLiveServerNames() {
        return List.of();
    }

    /**
     * Returns live world names known by the platform.
     *
     * @param serverName optional server filter
     * @param uniqueId   optional player UUID filter
     * @return live world names
     */
    default List<String> getLiveWorldNames(final Optional<String> serverName, final Optional<UUID> uniqueId) {
        return List.of();
    }

    /**
     * Returns the LoriTime plugin version.
     *
     * @return plugin version
     */
    String getPluginVersion();

    /**
     * Returns the platform plugin artifact name.
     *
     * @return plugin jar name
     */
    String getPluginJarName();

    /**
     * Returns the platform server version.
     *
     * @return server version
     */
    String getServerVersion();

    /**
     * Returns the configured LoriTime server mode.
     *
     * @return server mode
     */
    String getServerMode();

    /**
     * Updates the configured LoriTime server mode.
     *
     * @param serverMode server mode
     */
    void setServerMode(String serverMode);

    /**
     * Returns whether this instance is a proxy platform.
     *
     * @return true for proxy platforms
     */
    boolean isProxy();

    /**
     * Dispatches a command as the console.
     *
     * @param consoleSender console sender
     * @param command       command line
     * @return true when the platform accepted the command
     */
    boolean dispatchCommand(CommonSender consoleSender, String command);

    /**
     * Kicks a player with a component message.
     *
     * @param player  player to kick
     * @param message kick message
     */
    void kickPlayer(LoriTimePlayer player, Component message);

    /**
     * Sends a component message to the console.
     *
     * @param message console message
     */
    void sendMessageToConsole(Component message);
}
