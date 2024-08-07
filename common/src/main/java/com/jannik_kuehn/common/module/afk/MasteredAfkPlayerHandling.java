package com.jannik_kuehn.common.module.afk;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.LoriTimePlayer;
import com.jannik_kuehn.common.exception.StorageException;
import com.jannik_kuehn.common.utils.TimeUtil;

public class MasteredAfkPlayerHandling extends AfkHandling {

    public MasteredAfkPlayerHandling(final LoriTimePlugin plugin) {
        super(plugin);
    }

    @Override
    public void executePlayerAfk(final LoriTimePlayer loriTimePlayer, final long timeToRemove) {
        if (!afkEnabled || !isOnline(loriTimePlayer.getUniqueId())) {
            return;
        }

        if (removeTimeEnabled && !hasPermission(loriTimePlayer, "loritime.afk.bypass.timeRemove")) {
            try {
                loriTimePlugin.getTimeStorage().flushOnlineTimeCache();
                loriTimePlugin.getTimeStorage().addTime(loriTimePlayer.getUniqueId(), -timeToRemove);
            } catch (final Exception e) {
                loriTimePlugin.getLogger().warning("Error while removing online time while afk for player " + loriTimePlayer.getUniqueId(), e);
            }
        }

        if (autoKickEnabled && !hasPermission(loriTimePlayer, "loritime.afk.bypass.kick")) {
            loriTimePlugin.getServer().kickPlayer(loriTimePlayer, loriTimePlugin.getLocalization()
                    .formatTextComponentWithoutPrefix(loriTimePlugin.getLocalization().getRawMessage("message.afk.kick")
                            .replace("[player]", loriTimePlayer.getName())
                            .replace("[time]", TimeUtil.formatTime(timeToRemove, loriTimePlugin.getLocalization()))
                    ));
            sendKickAnnounce(loriTimePlayer, timeToRemove, "loritime.afk.announce.kick");
        } else if (hasPermission(loriTimePlayer, "loritime.afk.bypass.stopCount")) {
            chatAnnounce(loriTimePlayer, "message.afk.afkAnnounce", "loritime.afk.announce.afkAnnounce");
            selfAfkMessage(loriTimePlayer, "message.afk.afkSelf");
            stopAccumulatingAndSaveOnlineTime(loriTimePlayer);
        }
    }

    @Override
    public void executePlayerResume(final LoriTimePlayer loriTimePlayer) {
        if (!afkEnabled || !isOnline(loriTimePlayer.getUniqueId())) {
            return;
        }
        chatAnnounce(loriTimePlayer, "message.afk.resumeAnnounce", "loritime.afk.announce.afkAnnounce");
        selfAfkMessage(loriTimePlayer, "message.afk.afkResume");
        startAccumulatingOnlineTime(loriTimePlayer);
    }

    private void stopAccumulatingAndSaveOnlineTime(final LoriTimePlayer loriTimePlayer) {
        final long now = System.currentTimeMillis();
        try {
            loriTimePlugin.getTimeStorage().stopAccumulatingAndSaveOnlineTime(loriTimePlayer.getUniqueId(), now);
        } catch (final StorageException e) {
            loriTimePlugin.getLogger().error("error while stopping accumulation of online time for player " + loriTimePlayer.getName(), e);
        }
    }

    private void startAccumulatingOnlineTime(final LoriTimePlayer loriTimePlayer) {
        final long now = System.currentTimeMillis();
        try {
            loriTimePlugin.getTimeStorage().startAccumulating(loriTimePlayer.getUniqueId(), now);
        } catch (final StorageException e) {
            loriTimePlugin.getLogger().error("error while starting accumulation of online time for player " + loriTimePlayer, e);
        }
    }
}
