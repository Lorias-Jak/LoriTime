package com.jannik_kuehn.common.command;

import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.LoriTimePlayer;
import com.jannik_kuehn.common.api.common.CommonCommand;
import com.jannik_kuehn.common.api.common.CommonPlayerSender;
import com.jannik_kuehn.common.api.common.CommonSender;
import com.jannik_kuehn.common.api.storage.ManualTimeAdjustment;
import com.jannik_kuehn.common.api.storage.TimeEntryReason;
import com.jannik_kuehn.common.api.storage.TimeScope;
import com.jannik_kuehn.common.command.core.CommandMessages;
import com.jannik_kuehn.common.command.core.CommandScopes;
import com.jannik_kuehn.common.command.core.CommandScopes.ParsedScope;
import com.jannik_kuehn.common.command.core.CommandScopes.ParsedTimedScope;
import com.jannik_kuehn.common.command.core.PlayerNameCompletions;
import com.jannik_kuehn.common.command.core.SubcommandRouter;
import com.jannik_kuehn.common.config.localization.Localization;
import com.jannik_kuehn.common.exception.StorageException;
import com.jannik_kuehn.common.utils.TimeParser;
import com.jannik_kuehn.common.utils.TimeUtil;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

/**
 * Canonical player mutation command.
 */
@SuppressWarnings({"PMD.CommentRequired", "PMD.TooManyMethods", "PMD.AvoidLiteralsInIfCondition",
        "PMD.AvoidDuplicateLiterals"})
public class LoriTimeModifyCommand implements CommonCommand {

    private final LoriTimePlugin plugin;

    private final WrappedLogger log;

    private final Localization localization;

    private final TimeParser parser;

    private final SubcommandRouter<ModifyAction> router;

    public LoriTimeModifyCommand(final LoriTimePlugin plugin, final Localization localization, final TimeParser parser) {
        this.plugin = plugin;
        this.log = plugin.getLoggerFactory().create(LoriTimeModifyCommand.class);
        this.localization = localization;
        this.parser = parser;
        this.router = new SubcommandRouter<ModifyAction>()
                .register(ModifyAction.ADD, "add", "modify", "mod")
                .register(ModifyAction.SET, "set")
                .register(ModifyAction.RESET, "reset")
                .register(ModifyAction.DELETE_USER, "deleteUser", "deleteuser");
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

    private void execute(final CommonSender sender, final ModifyAction action, final String... args) {
        final String[] subCommandArgs = Arrays.copyOfRange(args, 1, args.length);
        try {
            switch (action) {
                case ADD -> modify(sender, subCommandArgs);
                case SET -> set(sender, subCommandArgs);
                case RESET -> reset(sender, subCommandArgs);
                case DELETE_USER -> deleteUser(sender, subCommandArgs);
            }
        } catch (final StorageException e) {
            log.error("An exception ocurred during LoriTimeModifyCommand!", e);
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
        if (args.length == 2) {
            return PlayerNameCompletions.suggest(plugin, args[1]);
        }
        if (args.length > 2 && args[args.length - 1].isBlank()) {
            return List.of(CommandScopes.SERVER, CommandScopes.WORLD);
        }
        return List.of();
    }

    private void set(final CommonSender sender, final String... args) throws StorageException {
        if (args.length < 2) {
            CommandMessages.send(localization, plugin.getLanguageSelector(), sender, "message.command.loritimeadmin.set.usage");
            return;
        }
        final Optional<UUID> optionalUUID = plugin.getStorage().getUuid(args[0]);
        if (optionalUUID.isEmpty()) {
            printMissingUuidMessage(sender, args[0]);
            return;
        }

        final ParsedTimedScope timedScope = CommandScopes.parseTimedScope(args);
        if (timedScope == null) {
            CommandMessages.send(localization, plugin.getLanguageSelector(), sender, "message.command.loritimeadmin.set.usage");
            return;
        }
        final String[] timeArgs = timedScope.timeArgs();
        final OptionalLong optionalTime = parser.parseToSeconds(String.join(" ", timeArgs));
        if (optionalTime.isEmpty()) {
            printNotTimeMessage(sender, timeArgs);
            return;
        }
        final long time = optionalTime.getAsLong();

        final LoriTimePlayer player = plugin.getPlayerConverter().getOnlinePlayer(optionalUUID.get());
        if (time < 0) {
            sender.sendMessage(localization.formatTextComponent(localization.getRawMessage("message.command.loritimeadmin.set.negativeTime")
                    .replace("[player]", player.getName())
                    .replace("[time]", TimeUtil.formatTime(time, localization))));
            return;
        }

        final OptionalLong optionalCurrentTime = plugin.getStorage().getTime(player.getUniqueId(), timedScope.scope());
        if (optionalCurrentTime.isEmpty()) {
            sender.sendMessage(localization.formatTextComponent(localization.getRawMessage("message.command.loritime.notFound")
                    .replace("[player]", player.getName())));
            return;
        }

        final long currentTime = optionalCurrentTime.getAsLong();
        if (currentTime != time) {
            modifyOnlineTime(sender, player.getUniqueId(), time - currentTime, timedScope.scope());
        }
        sender.sendMessage(localization.formatTextComponent(localization.getRawMessage("message.command.loritimeadmin.set.success")
                .replace("[player]", player.getName())
                .replace("[time]", TimeUtil.formatTime(time, localization))));
    }

    private void modify(final CommonSender sender, final String... args) throws StorageException {
        if (args.length < 2) {
            CommandMessages.send(localization, plugin.getLanguageSelector(), sender,
                    "message.command.loritimeadmin.modify.usage");
            return;
        }

        final Optional<UUID> optionalUUID = plugin.getStorage().getUuid(args[0]);
        if (optionalUUID.isEmpty()) {
            printMissingUuidMessage(sender, args[0]);
            return;
        }

        final ParsedTimedScope timedScope = CommandScopes.parseTimedScope(args);
        if (timedScope == null) {
            CommandMessages.send(localization, plugin.getLanguageSelector(), sender,
                    "message.command.loritimeadmin.modify.usage");
            return;
        }
        final String[] timeArgs = timedScope.timeArgs();
        final OptionalLong optionalTime = parser.parseToSeconds(String.join(" ", timeArgs));
        if (optionalTime.isEmpty()) {
            printNotTimeMessage(sender, timeArgs);
            return;
        }

        final LoriTimePlayer player = plugin.getPlayerConverter().getOnlinePlayer(optionalUUID.get());
        final OptionalLong optionalCurrentTime = plugin.getStorage().getTime(player.getUniqueId(), timedScope.scope());
        if (optionalCurrentTime.isEmpty()) {
            sender.sendMessage(localization.formatTextComponent(localization.getRawMessage("message.command.loritime.notFound")
                    .replace("[player]", player.getName())));
            return;
        }
        final long time = optionalTime.getAsLong();
        if (optionalCurrentTime.getAsLong() + time < 0) {
            sender.sendMessage(localization.formatTextComponent(localization.getRawMessage("message.command.loritimeadmin.modify.negativeTimeSum")
                    .replace("[player]", player.getName())
                    .replace("[time]", TimeUtil.formatTime(time, localization))));
            return;
        }
        if (time != 0) {
            modifyOnlineTime(sender, player.getUniqueId(), time, timedScope.scope());
        }
        sender.sendMessage(localization.formatTextComponent(localization.getRawMessage("message.command.loritimeadmin.modify.success")
                .replace("[player]", player.getName())
                .replace("[time]", TimeUtil.formatTime(time, localization))));
    }

    private void reset(final CommonSender sender, final String... args) throws StorageException {
        final ParsedScope parsedScope = CommandScopes.parseResetScope(args);
        if (parsedScope == null) {
            CommandMessages.send(localization, plugin.getLanguageSelector(), sender,
                    "message.command.loritimeadmin.reset.usage");
            return;
        }

        final Optional<UUID> optionalUUID = plugin.getStorage().getUuid(parsedScope.playerName());
        if (optionalUUID.isEmpty()) {
            printMissingUuidMessage(sender, parsedScope.playerName());
            return;
        }
        final LoriTimePlayer player = plugin.getPlayerConverter().getOnlinePlayer(optionalUUID.get());

        final OptionalLong optionalCurrentTime = plugin.getStorage().getTime(player.getUniqueId(), parsedScope.scope());
        if (optionalCurrentTime.isEmpty()) {
            sender.sendMessage(localization.formatTextComponent(localization.getRawMessage("message.command.loritime.notFound")
                    .replace("[player]", player.getName())));
            return;
        }
        final long currentTime = optionalCurrentTime.getAsLong();
        if (currentTime != 0) {
            modifyOnlineTime(sender, player.getUniqueId(), -currentTime, parsedScope.scope());
        }
        sender.sendMessage(localization.formatTextComponent(localization.getRawMessage("message.command.loritimeadmin.reset.success")
                .replace("[player]", player.getName())));
    }

    private void deleteUser(final CommonSender sender, final String... args) throws StorageException {
        if (args.length < 2) {
            CommandMessages.send(localization, plugin.getLanguageSelector(), sender,
                    "message.command.loritimeadmin.deleteUser.usage");
            return;
        }

        final Optional<UUID> optionalUUID = plugin.getStorage().getUuid(args[0]);
        if (optionalUUID.isEmpty()) {
            printMissingUuidMessage(sender, args[0]);
            return;
        }

        final Optional<CommonPlayerSender> onlinePlayer = plugin.getServer().getPlayer(optionalUUID.get());
        if (onlinePlayer.isPresent() && onlinePlayer.get().isOnline()) {
            CommandMessages.send(localization, plugin.getLanguageSelector(), sender,
                    "message.command.loritimeadmin.deleteUser.userOnline");
            return;
        }

        final LoriTimePlayer player = plugin.getPlayerConverter().getOnlinePlayer(optionalUUID.get());
        try {
            plugin.getStorage().deletePlayer(player.getUniqueId());
            sender.sendMessage(localization.formatTextComponent(localization
                    .getRawMessage("message.command.loritimeadmin.deleteUser.success")
                    .replace("[player]", player.getName())));
        } catch (StorageException | SQLException e) {
            CommandMessages.send(localization, plugin.getLanguageSelector(), sender,
                    "message.command.loritimeadmin.deleteUser.issue");
            log.error("An exception occurred while deleting the user from the Plugin!", e);
        }
    }

    public void modifyOnlineTime(final CommonSender sender, final UUID uuid, final long modifyBy) {
        modifyOnlineTime(sender, uuid, modifyBy, TimeScope.GLOBAL);
    }

    public void modifyOnlineTime(final CommonSender sender, final UUID uuid, final long modifyBy, final TimeScope scope) {
        try {
            final UUID actorUuid = sender instanceof CommonPlayerSender playerSender ? playerSender.getUniqueId() : null;
            final String actorName = sender instanceof CommonPlayerSender ? sender.getName() : "CONSOLE";
            plugin.getStorage().addTime(new ManualTimeAdjustment(uuid, modifyBy,
                    TimeEntryReason.MANUAL_ADJUSTMENT, actorUuid, actorName, scope));
        } catch (final StorageException ex) {
            log.error("could not modify online time of " + uuid.toString(), ex);
        }
    }

    @Override
    public List<String> getAliases() {
        return List.of("ltm", "ltmodify");
    }

    @Override
    public String getCommandName() {
        return "ltmodify";
    }

    private void printMissingUuidMessage(final CommonSender sender, final String playerName) {
        sender.sendMessage(localization.formatTextComponent(localization.getRawMessage("message.command.loritimeadmin.missingUuid")
                .replace("[player]", playerName)));
    }

    private void printNotTimeMessage(final CommonSender sender, final String... notTime) {
        sender.sendMessage(localization.formatTextComponent(localization.getRawMessage("message.command.loritimeadmin.notTime")
                .replace("[argument]", String.join(" ", notTime))));
    }

    private void usage(final CommonSender sender) {
        CommandMessages.send(localization, plugin.getLanguageSelector(), sender, "message.command.loritimeadmin.usage");
    }

    private enum ModifyAction {
        ADD,
        SET,
        RESET,
        DELETE_USER
    }
}
