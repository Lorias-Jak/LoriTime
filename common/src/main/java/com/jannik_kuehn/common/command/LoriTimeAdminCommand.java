package com.jannik_kuehn.common.command;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.common.CommonCommand;
import com.jannik_kuehn.common.api.common.CommonSender;
import com.jannik_kuehn.common.command.core.CommandMessages;
import com.jannik_kuehn.common.command.core.SubcommandRouter;
import com.jannik_kuehn.common.config.localization.Localization;

import java.util.Arrays;
import java.util.List;

/**
 * Runtime administration command for the current LoriTime instance.
 */
@SuppressWarnings({"PMD.CommentRequired", "PMD.AvoidLiteralsInIfCondition"})
public class LoriTimeAdminCommand implements CommonCommand {

    private final LoriTimePlugin plugin;

    private final Localization localization;

    private final SubcommandRouter<AdminAction> router;

    private final LoriTimeAdminActions actions;

    public LoriTimeAdminCommand(final LoriTimePlugin plugin, final Localization localization) {
        this.plugin = plugin;
        this.localization = localization;
        this.router = new SubcommandRouter<AdminAction>()
                .register(AdminAction.RELOAD, "reload")
                .register(AdminAction.DEBUG, "debug")
                .register(AdminAction.INFO, "info")
                .register(AdminAction.UPDATE, "update");
        this.actions = new LoriTimeAdminActions(plugin, localization, this::usage);
    }

    @Override
    public void execute(final CommonSender sender, final String... args) {
        if (!sender.hasPermission("loritime.admin")) {
            CommandMessages.send(localization, plugin.getLanguageSelector(), sender, "message.noPermission");
            return;
        }
        if (args.length < 1) {
            usage(sender);
            return;
        }
        router.find(args[0]).ifPresentOrElse(action -> plugin.getScheduler().runAsyncOnce(() -> execute(sender, action, args)),
                () -> usage(sender));
    }

    private void execute(final CommonSender sender, final AdminAction action, final String... args) {
        final String[] subCommandArgs = Arrays.copyOfRange(args, 1, args.length);
        switch (action) {
            case RELOAD -> actions.reload(sender, subCommandArgs);
            case DEBUG -> actions.debug(sender, subCommandArgs);
            case INFO -> actions.info(sender, subCommandArgs);
            case UPDATE -> actions.update(sender, subCommandArgs);
        }
    }

    @Override
    public List<String> handleTabComplete(final CommonSender source, final String... args) {
        if (!source.hasPermission("loritime.admin")) {
            return List.of();
        }
        if (args.length <= 1) {
            return router.complete(args.length == 0 ? "" : args[0]);
        }
        return List.of();
    }

    @Override
    public List<String> getAliases() {
        return List.of("lta", "ltadmin", "loritimea");
    }

    @Override
    public String getCommandName() {
        return "lta";
    }

    private void usage(final CommonSender sender) {
        CommandMessages.send(localization, plugin.getLanguageSelector(), sender, "message.command.loritimeadmin.usage");
    }

    private enum AdminAction {
        RELOAD,
        DEBUG,
        INFO,
        UPDATE
    }
}
