package com.jannik_kuehn.loritime.api;

import java.util.List;

public interface CommonCommand {

    void execute(CommonSender sender, String... arguments);

    List<String> handleTabComplete(CommonSender source, String... args);

    String[] getAliases();

    String getCommandName();

}
