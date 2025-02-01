package com.jannik_kuehn.common.api.common;

import com.jannik_kuehn.common.api.LoriTimePlayer;
import net.kyori.adventure.text.TextComponent;

import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("PMD.CommentRequired")
public interface CommonServer {

    Optional<CommonSender> getPlayer(UUID uniqueId);

    Optional<CommonSender> getPlayer(String name);

    CommonSender[] getOnlinePlayers();

    String getPluginVersion();

    String getPluginJarName();

    String getServerVersion();

    String getServerMode();

    void setServerMode(String serverMode);

    java.util.logging.Logger getJavaLogger();

    org.slf4j.Logger getSl4jLogger();

    boolean isProxy();

    boolean dispatchCommand(CommonSender consoleSender, String command);

    void kickPlayer(LoriTimePlayer player, TextComponent message);

    void sendMessageToConsole(TextComponent message);
}
