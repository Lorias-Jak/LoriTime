package com.jannik_kuehn.common.module.afk;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.LoriTimePlayer;
import com.jannik_kuehn.common.api.logger.LoriTimeLogger;
import com.jannik_kuehn.common.exception.StorageException;
import com.jannik_kuehn.common.utils.TimeUtil;

@SuppressWarnings("PMD.CommentRequired")
public class MasteredAfkPlayerHandling extends AfkHandling {
    private final LoriTimeLogger log;

    public MasteredAfkPlayerHandling(final LoriTimePlugin plugin) {
        super(plugin);
        this.log = plugin.getLoggerFactory().create(MasteredAfkPlayerHandling.class, "MasteredAfkPlayerHandling");
    }

    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public void executePlayerAfk(final LoriTimePlayer loriTimePlayer, final long timeToRemove) {
        log.debug("Executing AFK for player: " + loriTimePlayer.getName() + ". Time to remove: " + timeToRemove);
        if (!afkEnabled || !isOnline(loriTimePlayer.getUniqueId())) {
            log.debug("AFK is not enabled or player is not online. Skipping the process");
            return;
        }

        if (removeTimeEnabled && !hasPermission(loriTimePlayer, "loritime.afk.bypass.timeRemove")) {
            try {
                log.debug("Removing online time for player " + loriTimePlayer.getUniqueId()
                        + ". Time to remove: " + timeToRemove);
                loriTimePlugin.getTimeStorage().flushOnlineTimeCache();
                loriTimePlugin.getTimeStorage().addTime(loriTimePlayer.getUniqueId(), -timeToRemove);
            } catch (final Exception e) {
                log.warn("Error while removing online time while afk for player " + loriTimePlayer.getUniqueId(), e);
            }
        }

        if (autoKickEnabled && !hasPermission(loriTimePlayer, "loritime.afk.bypass.kick")) {
            log.debug("Kicking player " + loriTimePlayer.getName() + " because he's afk for too long");
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
        log.debug("Executing resume for player: " + loriTimePlayer.getName());
        if (!afkEnabled || !isOnline(loriTimePlayer.getUniqueId())) {
            log.debug("AFK is not enabled or player is not online. Skipping the process");
            return;
        }
        chatAnnounce(loriTimePlayer, "message.afk.resumeAnnounce", "loritime.afk.announce.afkAnnounce");
        selfAfkMessage(loriTimePlayer, "message.afk.afkResume");
        startAccumulatingOnlineTime(loriTimePlayer);
    }

    private void stopAccumulatingAndSaveOnlineTime(final LoriTimePlayer loriTimePlayer) {
        log.debug("Stopping accumulation of online time for player " + loriTimePlayer.getName());
        final long now = System.currentTimeMillis();
        try {
            loriTimePlugin.getTimeStorage().stopAccumulatingAndSaveOnlineTime(loriTimePlayer.getUniqueId(), now);
        } catch (final StorageException e) {
            log.error("error while stopping accumulation of online time for player " + loriTimePlayer.getName(), e);
        }
    }

    private void startAccumulatingOnlineTime(final LoriTimePlayer loriTimePlayer) {
        log.debug("Starting accumulation of online time for player " + loriTimePlayer.getName());
        final long now = System.currentTimeMillis();
        try {
            loriTimePlugin.getTimeStorage().startAccumulating(loriTimePlayer.getUniqueId(), now);
        } catch (final StorageException e) {
            log.error("error while starting accumulation of online time for player " + loriTimePlayer, e);
        }
    }
}
