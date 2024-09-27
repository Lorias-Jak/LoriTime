package com.jannik_kuehn.common.command;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.common.CommonCommand;
import com.jannik_kuehn.common.api.common.CommonSender;
import com.jannik_kuehn.common.api.logger.LoriTimeLogger;
import com.jannik_kuehn.common.config.localization.Localization;

import java.util.List;

public class LoriTimeDebugCommand implements CommonCommand {
    private final LoriTimePlugin loriTimePlugin;

    private final Localization localization;

    private final LoriTimeLogger log;

    private boolean isDebugging;

    public LoriTimeDebugCommand(final LoriTimePlugin loriTimePlugin, final Localization localization) {
        this.loriTimePlugin = loriTimePlugin;
        this.localization = localization;
        this.log = loriTimePlugin.getLoggerFactory().create(LoriTimeDebugCommand.class, "LoriTimeDebugCommand");
        this.isDebugging = loriTimePlugin.getConfig().getBoolean("general.debug");

        loriTimePlugin.getScheduler().runAsyncOnce(this::autoDisableCheck);
    }

    @Override
    public void execute(final CommonSender sender, final String... arguments) {
        if (!sender.hasPermission("loritime.debug")) {
            sender.sendMessage(localization.formatTextComponent(localization.getRawMessage("message.nopermission")));
            return;
        }
        changeDebugMode(sender);
        autoDisableCheck();
    }

    private void changeDebugMode(final CommonSender sender) {
        final boolean configValue = loriTimePlugin.getConfig().getBoolean("general.debug");
        if (configValue) {
            stopDebugging();
            sender.sendMessage(localization.formatTextComponent(localization.getRawMessage("message.command.debug.disabled")));
        } else {
            startDebugging();
            sender.sendMessage(localization.formatTextComponent(localization.getRawMessage("message.command.debug.enabled")));
        }
    }

    private void startDebugging() {
        isDebugging = true;
        loriTimePlugin.getConfig().setTemporaryValue("general.debug", true);
        log.info("Debug mode has been enabled.");
    }

    private void stopDebugging() {
        isDebugging = false;
        loriTimePlugin.getConfig().setTemporaryValue("general.debug", false);
        log.info("Debug mode has been disabled.");
    }

    private void autoDisableCheck() {
        final int configTimeToDisable = loriTimePlugin.getConfig().getInt("general.debugAutoDisableTime", 30);
        if (configTimeToDisable <= 0) {
            log.debug("Debug mode will not be disabled automatically.");
            return;
        }
        final long timeToDisable = configTimeToDisable * 60L;
        loriTimePlugin.getScheduler().runAsyncOnceLater(timeToDisable, () -> {
            if (isDebugging) {
                log.debug("Auto disabling the debug mode.");
                stopDebugging();
            }
        });
    }

    @Override
    public List<String> handleTabComplete(final CommonSender source, final String... args) {
        return List.of();
    }

    @Override
    public List<String> getAliases() {
        return List.of("loritimedebug");
    }

    @Override
    public String getCommandName() {
        return "ltdebug";
    }
}
