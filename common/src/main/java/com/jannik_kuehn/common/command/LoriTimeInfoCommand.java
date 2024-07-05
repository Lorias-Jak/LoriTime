package com.jannik_kuehn.common.command;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.common.CommonCommand;
import com.jannik_kuehn.common.api.common.CommonSender;
import com.jannik_kuehn.common.config.localization.Localization;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.List;

public class LoriTimeInfoCommand implements CommonCommand {

    private final LoriTimePlugin plugin;

    private final Localization localization;

    public LoriTimeInfoCommand(final LoriTimePlugin plugin, final Localization localization) {
        this.plugin = plugin;
        this.localization = localization;
    }

    @Override
    public void execute(final CommonSender sender, final String... args) {
        if (!sender.hasPermission("loritime.info")) {
            printUtilityMessage(sender, "message.nopermission");
            return;
        }

        plugin.getScheduler().runAsyncOnce(() -> {
            final MiniMessage miniMessage = MiniMessage.builder().build();
            final String serverVersion = "<#A4A4A4>Server version: <#FF3232>" + plugin.getServer().getServerVersion();
            final String pluginVersion = "<#A4A4A4>Plugin version: <#FF3232>" + plugin.getServer().getPluginVersion();

            sender.sendMessage(localization.formatTextComponent("Version Information"));
            sender.sendMessage("");
            sender.sendMessage((TextComponent) miniMessage.deserialize(serverVersion));
            sender.sendMessage((TextComponent) miniMessage.deserialize(pluginVersion));
        });
    }

    @Override
    public List<String> handleTabComplete(final CommonSender source, final String... args) {
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

    private void printUtilityMessage(final CommonSender sender, final String messageKey) {
        sender.sendMessage(localization.formatTextComponent(localization.getRawMessage(messageKey)));
    }
}
