package com.jannik_kuehn.loritime.common.utils;


import java.util.UUID;

public interface CommonServer {
    CommonSender getPlayer(UUID uniqueId);
    CommonSender getPlayer(String name);
    CommonSender[] getOnlinePlayers();
    boolean dispatchCommand(CommonSender consoleSender, String command);
}
