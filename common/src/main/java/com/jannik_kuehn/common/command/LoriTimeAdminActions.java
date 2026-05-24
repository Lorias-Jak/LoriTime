package com.jannik_kuehn.common.command;

import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.common.CommonSender;
import com.jannik_kuehn.common.api.scheduler.PluginTask;
import com.jannik_kuehn.common.command.core.CommandMessages;
import com.jannik_kuehn.common.config.localization.Localization;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.function.Consumer;

/**
 * Handles runtime administration subcommands.
 */
@SuppressWarnings("PMD.CommentRequired")
final class LoriTimeAdminActions {

    private static final String DEBUG_CONFIG_PATH = "general.debug";

    private final LoriTimePlugin plugin;

    private final Localization localization;

    private final Consumer<CommonSender> usage;

    private final WrappedLogger log;

    private boolean isDebugging;

    private PluginTask autoDisableTask;

    /* default */ LoriTimeAdminActions(final LoriTimePlugin plugin, final Localization localization,
                                       final Consumer<CommonSender> usage) {
        this.plugin = plugin;
        this.localization = localization;
        this.usage = usage;
        this.log = plugin.getLoggerFactory().create(LoriTimeAdminCommand.class, "LoriTimeAdminCommand");
        this.isDebugging = plugin.getConfig().getBoolean(DEBUG_CONFIG_PATH);
        plugin.getScheduler().runAsyncOnce(this::autoDisableCheck);
    }

    /* default */ void reload(final CommonSender sender, final String... args) {
        if (hasUnexpectedArgs(sender, args)) {
            return;
        }
        plugin.reload();
        CommandMessages.send(localization, sender, "message.command.loritimeadmin.reload.success");
    }

    /* default */ void debug(final CommonSender sender, final String... args) {
        if (hasUnexpectedArgs(sender, args)) {
            return;
        }
        changeDebugMode(sender);
        autoDisableCheck();
    }

    /* default */ void info(final CommonSender sender, final String... args) {
        if (hasUnexpectedArgs(sender, args)) {
            return;
        }
        final MiniMessage miniMessage = MiniMessage.builder().build();
        final String serverVersion = "<#A4A4A4>Server version: <#FF3232>" + plugin.getServer().getServerVersion();
        final String pluginVersion = "<#A4A4A4>Plugin version: <#FF3232>" + plugin.getServer().getPluginVersion();

        sender.sendMessage(localization.formatTextComponent("Version Information"));
        sender.sendMessage("");
        sender.sendMessage((TextComponent) miniMessage.deserialize(serverVersion));
        sender.sendMessage((TextComponent) miniMessage.deserialize(pluginVersion));
    }

    /* default */ void update(final CommonSender sender, final String... args) {
        if (hasUnexpectedArgs(sender, args)) {
            return;
        }
        if (plugin.getUpdater() == null || !plugin.getUpdater().isUpdateAvailable()) {
            sender.sendMessage(localization.formatTextComponent(localization.getRawMessage("message.updater.notFound")));
            return;
        }
        plugin.getUpdater().update(sender);
    }

    private boolean hasUnexpectedArgs(final CommonSender sender, final String... args) {
        if (args.length == 0) {
            return false;
        }
        usage.accept(sender);
        return true;
    }

    private void changeDebugMode(final CommonSender sender) {
        final boolean configValue = plugin.getConfig().getBoolean(DEBUG_CONFIG_PATH);
        if (configValue) {
            if (autoDisableTask != null) {
                autoDisableTask.cancel();
            }
            stopDebugging();
            CommandMessages.send(localization, sender, "message.command.debug.disabled");
        } else {
            startDebugging();
            CommandMessages.send(localization, sender, "message.command.debug.enabled");
        }
    }

    private void startDebugging() {
        isDebugging = true;
        plugin.getConfig().setTemporaryValue(DEBUG_CONFIG_PATH, true);
        log.info("Debug mode has been enabled.");
    }

    private void stopDebugging() {
        isDebugging = false;
        plugin.getConfig().setTemporaryValue(DEBUG_CONFIG_PATH, false);
        log.info("Debug mode has been disabled.");
    }

    private void autoDisableCheck() {
        final int configTimeToDisable = plugin.getConfig().getInt("general.debugAutoDisableTime", 30);
        if (configTimeToDisable <= 0) {
            log.debug("Debug mode will not be disabled automatically.");
            return;
        }
        final long timeToDisable = configTimeToDisable * 60L;
        autoDisableTask = plugin.getScheduler().runAsyncOnceLater(timeToDisable, () -> {
            if (isDebugging) {
                log.debug("Auto disabling the debug mode.");
                stopDebugging();
            }
        });
    }
}
