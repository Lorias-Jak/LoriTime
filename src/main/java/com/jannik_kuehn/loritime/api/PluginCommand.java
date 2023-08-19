package com.jannik_kuehn.loritime.api;

public interface PluginCommand {

    void execute(PluginCommandSender sender, String... arguments);
}
