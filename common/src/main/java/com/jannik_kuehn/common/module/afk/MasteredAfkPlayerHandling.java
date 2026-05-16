package com.jannik_kuehn.common.module.afk;

import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.LoriTimePlayer;
import com.jannik_kuehn.common.api.storage.ManualTimeAdjustment;
import com.jannik_kuehn.common.api.storage.TimeEntryReason;
import com.jannik_kuehn.common.exception.StorageException;
import com.jannik_kuehn.common.utils.TimeUtil;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles AFK actions on instances that own canonical time storage.
 */
public class MasteredAfkPlayerHandling extends AfkHandling {
    /**
     * Logger for mastered AFK handling.
     */
    private final WrappedLogger log;

    /**
     * Players whose active accumulation was stopped while they were AFK.
     */
    private final Set<UUID> playersStoppedForAfk;

    /**
     * Creates a mastered AFK handler.
     *
     * @param plugin the plugin instance.
     */
    public MasteredAfkPlayerHandling(final LoriTimePlugin plugin) {
        super(plugin);
        this.log = plugin.getLoggerFactory().create(MasteredAfkPlayerHandling.class, "MasteredAfkPlayerHandling");
        this.playersStoppedForAfk = ConcurrentHashMap.newKeySet();
    }

    /**
     * Applies AFK side effects for a player, including optional time removal and kick handling.
     *
     * @param loriTimePlayer the player that became AFK.
     * @param timeToRemove the AFK time to remove in seconds.
     */
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
                loriTimePlugin.getAccumulator().flushOnlineTimeCache();
                loriTimePlugin.getAccumulatingStorage().addTime(new ManualTimeAdjustment(loriTimePlayer.getUniqueId(),
                        -timeToRemove, TimeEntryReason.AFK_ADJUSTMENT, "SYSTEM"));
            } catch (final Exception e) {
                log.warn("Error while removing online time while afk for player " + loriTimePlayer.getUniqueId(), e);
            }
        }

        if (autoKickEnabled && !hasPermission(loriTimePlayer, "loritime.afk.bypass.kick")) {
            log.debug("Kicking player " + loriTimePlayer.getName() + " because he's afk for too long");
            loriTimePlugin.markAfkKick(loriTimePlayer.getUniqueId());
            loriTimePlugin.getServer().kickPlayer(loriTimePlayer, loriTimePlugin.getLocalization()
                    .formatTextComponentWithoutPrefix(loriTimePlugin.getLocalization().getRawMessage("message.afk.kick")
                            .replace("[player]", loriTimePlayer.getName())
                            .replace("[time]", TimeUtil.formatTime(timeToRemove, loriTimePlugin.getLocalization()))
                    ));
            sendKickAnnounce(loriTimePlayer, timeToRemove, "loritime.afk.announce.kick");
            return;
        }

        chatAnnounce(loriTimePlayer, "message.afk.afkAnnounce", "loritime.afk.announce.afkAnnounce");
        selfAfkMessage(loriTimePlayer, "message.afk.afkSelf");
        if (!hasPermission(loriTimePlayer, "loritime.afk.bypass.stopCount")) {
            stopAccumulatingAndSaveOnlineTime(loriTimePlayer);
        }
    }

    /**
     * Applies resume side effects for a player leaving AFK state.
     *
     * @param loriTimePlayer the player that resumed.
     */
    @Override
    public void executePlayerResume(final LoriTimePlayer loriTimePlayer) {
        log.debug("Executing resume for player: " + loriTimePlayer.getName());
        if (!afkEnabled || !isOnline(loriTimePlayer.getUniqueId())) {
            log.debug("AFK is not enabled or player is not online. Skipping the process");
            return;
        }
        chatAnnounce(loriTimePlayer, "message.afk.resumeAnnounce", "loritime.afk.announce.afkAnnounce");
        selfAfkMessage(loriTimePlayer, "message.afk.afkResume");
        if (playersStoppedForAfk.remove(loriTimePlayer.getUniqueId())) {
            startAccumulatingOnlineTime(loriTimePlayer);
        }
    }

    private void stopAccumulatingAndSaveOnlineTime(final LoriTimePlayer loriTimePlayer) {
        log.debug("Stopping accumulation of online time for player " + loriTimePlayer.getName());
        final long now = System.currentTimeMillis();
        try {
            loriTimePlugin.getAccumulator().stopAccumulatingAndSaveOnlineTime(loriTimePlayer.getUniqueId(), now,
                    TimeEntryReason.PLAYER_AFK);
            playersStoppedForAfk.add(loriTimePlayer.getUniqueId());
        } catch (final StorageException e) {
            log.error("error while stopping accumulation of online time for player " + loriTimePlayer.getName(), e);
        }
    }

    private void startAccumulatingOnlineTime(final LoriTimePlayer loriTimePlayer) {
        log.debug("Starting accumulation of online time for player " + loriTimePlayer.getName());
        final long now = System.currentTimeMillis();
        try {
            loriTimePlugin.getAccumulator().startAccumulating(loriTimePlayer.getUniqueId(), loriTimePlayer.getName(),
                    "default", "global", now);
        } catch (final StorageException e) {
            log.error("error while starting accumulation of online time for player " + loriTimePlayer, e);
        }
    }
}
