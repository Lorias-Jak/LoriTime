package com.jannik_kuehn.loritime.api;

import com.jannik_kuehn.loritime.common.utils.CommonSender;
import com.velocitypowered.api.command.CommandSource;

import java.util.List;

public interface CommonCommand {

    void execute(CommonSender sender, String... arguments);

    List<String> handleTabComplete(CommandSource source, String... args);

    String[] getAliases();

    String getCommandName();

}
