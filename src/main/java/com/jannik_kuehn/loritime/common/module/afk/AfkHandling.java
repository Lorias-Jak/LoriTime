package com.jannik_kuehn.loritime.common.module.afk;

import com.jannik_kuehn.loritime.api.CommonSender;
import com.jannik_kuehn.loritime.api.LoriTimePlayer;
import com.jannik_kuehn.loritime.common.LoriTimePlugin;
import com.jannik_kuehn.loritime.common.utils.TimeUtil;

import java.util.Optional;
import java.util.UUID;

public abstract class AfkHandling {

    protected final LoriTimePlugin loriTimePlugin;
    protected boolean afkEnabled;
    protected boolean removeTimeEnabled;
    protected boolean autoKickEnabled;

    public AfkHandling(LoriTimePlugin plugin) {
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

    protected boolean hasPermission(LoriTimePlayer loriTimePlayer, String permission) {
        Optional<CommonSender> optionalPlayer = loriTimePlugin.getServer().getPlayer(loriTimePlayer.getUniqueId());
        if (optionalPlayer.isEmpty()) {
            return false;
        }
        CommonSender player = optionalPlayer.get();
        return player.hasPermission(permission);
    }

    protected void sendKickAnnounce(LoriTimePlayer player, long timeToRemove, String permission) {
        for (CommonSender onlinePlayer : loriTimePlugin.getServer().getOnlinePlayers()) {
            if (!onlinePlayer.hasPermission(permission)) {
                continue;
            }
            onlinePlayer.sendMessage(loriTimePlugin.getLocalization().formatMiniMessage(loriTimePlugin.getLocalization()
                    .getRawMessage("message.afk.kickAnnounce")
                    .replace("[player]", player.getName())
                    .replace("[time]", TimeUtil.formatTime(timeToRemove, loriTimePlugin.getLocalization())))
            );
        }
    }
    protected void chatAnnounce(LoriTimePlayer player, String message, String permission) {
        for (CommonSender onlinePlayer : loriTimePlugin.getServer().getOnlinePlayers()) {
            if (!onlinePlayer.hasPermission(permission)) {
                continue;
            }
            onlinePlayer.sendMessage(loriTimePlugin.getLocalization().formatMiniMessage(loriTimePlugin.getLocalization()
                    .getRawMessage(message)
                    .replace("[player]", player.getName()))
            );
        }
    }

    protected void selfAfkMessage(LoriTimePlayer player, String message) {
        loriTimePlugin.getServer().getPlayer(player.getUniqueId()).get().sendMessage(loriTimePlugin.getLocalization().formatMiniMessage(
                loriTimePlugin.getLocalization().getRawMessage(message)));
    }

    protected boolean isOnline(UUID uuid) {
        Optional<CommonSender> optionalPlayer = loriTimePlugin.getServer().getPlayer(uuid);
        return optionalPlayer.isPresent();
    }
}
