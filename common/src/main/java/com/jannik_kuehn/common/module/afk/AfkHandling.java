package com.jannik_kuehn.common.module.afk;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.LoriTimePlayer;
import com.jannik_kuehn.common.api.common.CommonSender;
import com.jannik_kuehn.common.api.logger.LoriTimeLogger;
import com.jannik_kuehn.common.utils.TimeUtil;

import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("PMD.CommentRequired")
public abstract class AfkHandling {

    protected final LoriTimePlugin loriTimePlugin;

    private final LoriTimeLogger log;

    protected boolean afkEnabled;

    protected boolean removeTimeEnabled;

    protected boolean autoKickEnabled;

    @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
    public AfkHandling(final LoriTimePlugin plugin) {
        this.loriTimePlugin = plugin;
        this.log = loriTimePlugin.getLoggerFactory().create(AfkHandling.class, "AfkHandling");
        reloadConfigValues();
    }

    public abstract void executePlayerAfk(LoriTimePlayer loriTimePlayer, long timeToRemove);

    public abstract void executePlayerResume(LoriTimePlayer loriTimePlayer);

    public void reloadConfigValues() {
        log.debug("Reloading config values of AFKHandling");
        afkEnabled = loriTimePlugin.getConfig().getBoolean("afk.enabled", false);
        removeTimeEnabled = loriTimePlugin.getConfig().getBoolean("afk.removeTime", true);
        autoKickEnabled = loriTimePlugin.getConfig().getBoolean("afk.autoKick", true);
    }

    protected boolean hasPermission(final LoriTimePlayer loriTimePlayer, final String permission) {
        log.debug("Checking for players permission: " + permission + " for player: " + loriTimePlayer.getName());
        final Optional<CommonSender> optionalPlayer = loriTimePlugin.getServer().getPlayer(loriTimePlayer.getUniqueId());
        if (optionalPlayer.isEmpty()) {
            log.debug("Cant find a player with the UUID: " + loriTimePlayer.getUniqueId());
            return false;
        }
        final CommonSender player = optionalPlayer.get();
        return player.hasPermission(permission);
    }

    protected void sendKickAnnounce(final LoriTimePlayer player, final long timeToRemove, final String permission) {
        for (final CommonSender onlinePlayer : loriTimePlugin.getServer().getOnlinePlayers()) {
            log.debug("Sending kick announce to player: " + onlinePlayer.getName());
            if (!onlinePlayer.hasPermission(permission)) {
                log.debug("Skipping player: " + onlinePlayer.getName() + " because of missing permission: " + permission);
                continue;
            }
            onlinePlayer.sendMessage(loriTimePlugin.getLocalization().formatTextComponent(loriTimePlugin.getLocalization()
                    .getRawMessage("message.afk.kickAnnounce")
                    .replace("[player]", player.getName())
                    .replace("[time]", TimeUtil.formatTime(timeToRemove, loriTimePlugin.getLocalization())))
            );
        }
    }

    protected void chatAnnounce(final LoriTimePlayer player, final String message, final String permission) {
        for (final CommonSender onlinePlayer : loriTimePlugin.getServer().getOnlinePlayers()) {
            log.debug("Sending chat announce to player: " + onlinePlayer.getName());
            if (!onlinePlayer.hasPermission(permission)) {
                log.debug("Skipping player: " + onlinePlayer.getName() + " because of missing permission: " + permission);
                continue;
            }
            onlinePlayer.sendMessage(loriTimePlugin.getLocalization().formatTextComponent(loriTimePlugin.getLocalization()
                    .getRawMessage(message)
                    .replace("[player]", player.getName()))
            );
        }
    }

    protected void selfAfkMessage(final LoriTimePlayer player, final String message) {
        log.debug("Sending self afk message to player: " + player.getName());
        loriTimePlugin.getServer().getPlayer(player.getUniqueId()).get().sendMessage(loriTimePlugin.getLocalization().formatTextComponent(
                loriTimePlugin.getLocalization().getRawMessage(message)));
    }

    protected boolean isOnline(final UUID uuid) {
        log.debug("Checking if player with UUID: " + uuid + " is online");
        final Optional<CommonSender> optionalPlayer = loriTimePlugin.getServer().getPlayer(uuid);
        return optionalPlayer.isPresent();
    }
}
