package com.jannik_kuehn.common.api.common;

import java.util.List;

@SuppressWarnings("PMD.CommentRequired")
public interface CommonCommand {

    void execute(CommonSender sender, String... arguments);

    List<String> handleTabComplete(CommonSender source, String... args);

    List<String> getAliases();

    String getCommandName();

}
