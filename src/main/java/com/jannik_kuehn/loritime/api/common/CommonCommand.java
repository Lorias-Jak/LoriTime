package com.jannik_kuehn.loritime.api.common;

import java.util.List;

public interface CommonCommand {

    void execute(CommonSender sender, String... arguments);

    List<String> handleTabComplete(CommonSender source, String... args);

    List<String> getAliases();

    String getCommandName();

}
