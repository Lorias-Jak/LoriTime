package com.jannik_kuehn.loritime.common.utils;


import java.util.Optional;
import java.util.UUID;

public interface CommonServer {
    Optional<CommonSender> getPlayer(UUID uniqueId);
    Optional<CommonSender> getPlayer(String name);
    CommonSender[] getOnlinePlayers();
    boolean dispatchCommand(CommonSender consoleSender, String command);
}
