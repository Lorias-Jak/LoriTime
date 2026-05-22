package com.jannik_kuehn.common.api.common;

import com.jannik_kuehn.common.api.LoriTimePlayer;
import net.kyori.adventure.text.TextComponent;

import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("PMD.CommentRequired")
public interface CommonServer {

    Optional<CommonPlayerSender> getPlayer(UUID uniqueId);

    Optional<CommonPlayerSender> getPlayer(String name);

    CommonPlayerSender[] getOnlinePlayers();

    String getPluginVersion();

    String getPluginJarName();

    String getServerVersion();

    String getServerMode();

    void setServerMode(String serverMode);

    boolean isProxy();

    boolean dispatchCommand(CommonSender consoleSender, String command);

    void kickPlayer(LoriTimePlayer player, TextComponent message);

    void sendMessageToConsole(TextComponent message);
}
