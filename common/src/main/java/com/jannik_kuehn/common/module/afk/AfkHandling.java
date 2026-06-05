package com.jannik_kuehn.common.module.afk;

import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.LoriTimePlayer;
import com.jannik_kuehn.common.api.common.CommonPlayerSender;
import com.jannik_kuehn.common.utils.TimeUtil;

import java.util.Optional;
import java.util.UUID;

/**
 * Base class for platform-specific AFK side effects.
 */
public abstract class AfkHandling {

    /**
     * LoriTime plugin runtime used by AFK handlers.
     */
    protected final LoriTimePlugin loriTimePlugin;

    private final WrappedLogger log;

    /**
     * Whether AFK handling is enabled in configuration.
     */
    protected boolean afkEnabled;

    /**
     * Whether AFK time should be removed from online time totals.
     */
    protected boolean removeTimeEnabled;

    /**
     * Whether AFK players should be kicked automatically.
     */
    protected boolean autoKickEnabled;

    /**
     * Creates an AFK handler.
     *
     * @param plugin LoriTime plugin runtime
     */
    @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
    public AfkHandling(final LoriTimePlugin plugin) {
        this.loriTimePlugin = plugin;
        this.log = loriTimePlugin.getLoggerFactory().create(AfkHandling.class, "AfkHandling");
        reloadConfigValues();
    }

    /**
     * Applies platform-specific effects when a player becomes AFK.
     *
     * @param loriTimePlayer player becoming AFK
     * @param timeToRemove seconds to remove from online time, when configured
     */
    public abstract void executePlayerAfk(LoriTimePlayer loriTimePlayer, long timeToRemove);

    /**
     * Applies platform-specific effects when a player resumes from AFK.
     *
     * @param loriTimePlayer player resuming from AFK
     */
    public abstract void executePlayerResume(LoriTimePlayer loriTimePlayer);

    /**
     * Reloads AFK configuration values used by this handler.
     */
    public void reloadConfigValues() {
        log.debug("Reloading config values of AFKHandling");
        afkEnabled = loriTimePlugin.getConfig().getBoolean("afk.enabled", false);
        removeTimeEnabled = loriTimePlugin.getConfig().getBoolean("afk.removeTime", true);
        autoKickEnabled = loriTimePlugin.getConfig().getBoolean("afk.autoKick", true);
    }

    /**
     * Checks whether an online player has a permission.
     *
     * @param loriTimePlayer player identity
     * @param permission permission node
     * @return true when the online player has the permission
     */
    protected boolean hasPermission(final LoriTimePlayer loriTimePlayer, final String permission) {
        log.debug("Checking for players permission: " + permission + " for player: " + loriTimePlayer.getName());
        final Optional<CommonPlayerSender> optionalPlayer = loriTimePlugin.getServer().getPlayer(loriTimePlayer.getUniqueId());
        if (optionalPlayer.isEmpty()) {
            log.debug("Cant find a player with the UUID: " + loriTimePlayer.getUniqueId());
            return false;
        }
        final CommonPlayerSender player = optionalPlayer.get();
        return player.hasPermission(permission);
    }

    /**
     * Announces an AFK kick to players with a permission.
     *
     * @param player AFK player
     * @param timeToRemove seconds removed from the player's online time
     * @param permission permission required to receive the announcement
     */
    protected void sendKickAnnounce(final LoriTimePlayer player, final long timeToRemove, final String permission) {
        for (final CommonPlayerSender onlinePlayer : loriTimePlugin.getServer().getOnlinePlayers()) {
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

    /**
     * Sends a localized chat announcement to players with a permission.
     *
     * @param player player named in the announcement
     * @param message localization message key
     * @param permission permission required to receive the announcement
     */
    protected void chatAnnounce(final LoriTimePlayer player, final String message, final String permission) {
        for (final CommonPlayerSender onlinePlayer : loriTimePlugin.getServer().getOnlinePlayers()) {
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

    /**
     * Sends a localized AFK message to the affected player.
     *
     * @param player target player
     * @param message localization message key
     */
    protected void selfAfkMessage(final LoriTimePlayer player, final String message) {
        log.debug("Sending self afk message to player: " + player.getName());
        loriTimePlugin.getServer().getPlayer(player.getUniqueId()).get().sendMessage(loriTimePlugin.getLocalization().formatTextComponent(
                loriTimePlugin.getLocalization().getRawMessage(message)));
    }

    /**
     * Returns whether a player is currently online.
     *
     * @param uuid player UUID
     * @return true when the player is online
     */
    protected boolean isOnline(final UUID uuid) {
        log.debug("Checking if player with UUID: " + uuid + " is online");
        final Optional<CommonPlayerSender> optionalPlayer = loriTimePlugin.getServer().getPlayer(uuid);
        return optionalPlayer.isPresent();
    }
}
