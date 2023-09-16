package com.jannik_kuehn.loritime.api;


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
}
