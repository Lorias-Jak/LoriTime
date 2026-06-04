package com.jannik_kuehn.common.api.common;

import com.jannik_kuehn.common.api.LoriTimePlayer;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("PMD.CommentRequired")
public interface CommonServer {

    Optional<CommonPlayerSender> getPlayer(UUID uniqueId);

    Optional<CommonPlayerSender> getPlayer(String name);

    CommonPlayerSender[] getOnlinePlayers();

    default Optional<String> getCurrentServer(final UUID uniqueId) {
        return Optional.empty();
    }

    default Optional<String> getLocalServerName() {
        return Optional.empty();
    }

    default List<String> getLiveServerNames() {
        return List.of();
    }

    default List<String> getLiveWorldNames(final Optional<String> serverName, final Optional<UUID> uniqueId) {
        return List.of();
    }

    String getPluginVersion();

    String getPluginJarName();

    String getServerVersion();

    String getServerMode();

    void setServerMode(String serverMode);

    boolean isProxy();

    boolean dispatchCommand(CommonSender consoleSender, String command);

    void kickPlayer(LoriTimePlayer player, Component message);

    void sendMessageToConsole(Component message);
}
