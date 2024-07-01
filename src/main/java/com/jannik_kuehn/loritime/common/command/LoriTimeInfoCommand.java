package com.jannik_kuehn.loritime.common.command;

import com.jannik_kuehn.loritime.api.common.CommonCommand;
import com.jannik_kuehn.loritime.common.LoriTimePlugin;
import com.jannik_kuehn.loritime.common.config.localization.Localization;
import com.jannik_kuehn.loritime.api.common.CommonSender;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.List;

public class LoriTimeInfoCommand implements CommonCommand {

    private final LoriTimePlugin plugin;
    private final Localization localization;

    public LoriTimeInfoCommand(LoriTimePlugin plugin, Localization localization) {
        this.plugin = plugin;
        this.localization = localization;
    }

    @Override
    public void execute(CommonSender sender, String... args) {
        if (!sender.hasPermission("loritime.info")) {
            printUtilityMessage(sender, "message.nopermission");
            return;
        }

        plugin.getScheduler().runAsyncOnce(() -> {
            MiniMessage miniMessage = MiniMessage.builder().build();
            String serverVersion = "<#A4A4A4>Server version: <#FF3232>" + plugin.getServer().getServerVersion();
            String pluginVersion = "<#A4A4A4>Plugin version: <#FF3232>" + plugin.getPluginVersion();

            sender.sendMessage(localization.formatTextComponent("Version Information"));
            sender.sendMessage("");
            sender.sendMessage((TextComponent) miniMessage.deserialize(serverVersion));
            sender.sendMessage((TextComponent) miniMessage.deserialize(pluginVersion));
        });
    }

    @Override
    public List<String> handleTabComplete(CommonSender source, String... args) {
        return new ArrayList<>();
    }

    @Override
    public List<String> getAliases() {
        return List.of("lti", "linfo", "ltimeinfo", "loritinfo");
    }

    @Override
    public String getCommandName() {
        return "loritimeinfo";
    }

    private void printUtilityMessage(CommonSender sender, String messageKey) {
        sender.sendMessage(localization.formatTextComponent(localization.getRawMessage(messageKey)));
    }
}
