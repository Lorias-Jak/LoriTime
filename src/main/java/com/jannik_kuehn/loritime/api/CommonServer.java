package com.jannik_kuehn.loritime.api;

import com.jannik_kuehn.loritime.common.module.afk.AfkHandling;
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
}
