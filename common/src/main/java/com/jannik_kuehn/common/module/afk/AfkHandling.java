package com.jannik_kuehn.common.module.afk;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.LoriTimePlayer;
import com.jannik_kuehn.common.api.common.CommonSender;
import com.jannik_kuehn.common.utils.TimeUtil;

import java.util.Optional;
import java.util.UUID;

public abstract class AfkHandling {

    protected final LoriTimePlugin loriTimePlugin;

    protected boolean afkEnabled;

    protected boolean removeTimeEnabled;

    protected boolean autoKickEnabled;

    public AfkHandling(final LoriTimePlugin plugin) {
        this.loriTimePlugin = plugin;
        reloadConfigValues();
    }

    public abstract void executePlayerAfk(LoriTimePlayer loriTimePlayer, long timeToRemove);

    public abstract void executePlayerResume(LoriTimePlayer loriTimePlayer);

    public void reloadConfigValues() {
        afkEnabled = loriTimePlugin.getConfig().getBoolean("afk.enabled", false);
        removeTimeEnabled = loriTimePlugin.getConfig().getBoolean("afk.removeTime", true);
        autoKickEnabled = loriTimePlugin.getConfig().getBoolean("afk.autoKick", true);
    }

    protected boolean hasPermission(final LoriTimePlayer loriTimePlayer, final String permission) {
        final Optional<CommonSender> optionalPlayer = loriTimePlugin.getServer().getPlayer(loriTimePlayer.getUniqueId());
        if (optionalPlayer.isEmpty()) {
            return false;
        }
        final CommonSender player = optionalPlayer.get();
        return player.hasPermission(permission);
    }

    protected void sendKickAnnounce(final LoriTimePlayer player, final long timeToRemove, final String permission) {
        for (final CommonSender onlinePlayer : loriTimePlugin.getServer().getOnlinePlayers()) {
            if (!onlinePlayer.hasPermission(permission)) {
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
            if (!onlinePlayer.hasPermission(permission)) {
                continue;
            }
            onlinePlayer.sendMessage(loriTimePlugin.getLocalization().formatTextComponent(loriTimePlugin.getLocalization()
                    .getRawMessage(message)
                    .replace("[player]", player.getName()))
            );
        }
    }

    protected void selfAfkMessage(final LoriTimePlayer player, final String message) {
        loriTimePlugin.getServer().getPlayer(player.getUniqueId()).get().sendMessage(loriTimePlugin.getLocalization().formatTextComponent(
                loriTimePlugin.getLocalization().getRawMessage(message)));
    }

    protected boolean isOnline(final UUID uuid) {
        final Optional<CommonSender> optionalPlayer = loriTimePlugin.getServer().getPlayer(uuid);
        return optionalPlayer.isPresent();
    }
}
