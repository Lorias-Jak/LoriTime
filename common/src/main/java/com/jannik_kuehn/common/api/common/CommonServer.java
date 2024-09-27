package com.jannik_kuehn.common.api.common;

import com.jannik_kuehn.common.api.LoriTimePlayer;
import net.kyori.adventure.text.TextComponent;

import java.util.Optional;
import java.util.UUID;

public interface CommonServer {

    Optional<CommonSender> getPlayer(UUID uniqueId);

    Optional<CommonSender> getPlayer(String name);

    CommonSender[] getOnlinePlayers();

    boolean dispatchCommand(CommonSender consoleSender, String command);

    String getServerVersion();

    boolean isProxy();

    String getServerMode();

    void setServerMode(String serverMode);

    void kickPlayer(LoriTimePlayer player, TextComponent message);

    void sendMessageToConsole(TextComponent message);

    String getPluginVersion();

    java.util.logging.Logger getJavaLogger();

    org.slf4j.Logger getSl4jLogger();
}
