package com.jannik_kuehn.common.module.afk;

import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.LoriTimePlayer;
import com.jannik_kuehn.common.api.storage.TimeScope;
import com.jannik_kuehn.common.exception.StorageException;
import com.jannik_kuehn.common.storage.model.AfkPeriodEndReason;
import com.jannik_kuehn.common.storage.model.ManualTimeAdjustment;
import com.jannik_kuehn.common.storage.model.PlayerSessionContext;
import com.jannik_kuehn.common.storage.model.SessionContextDefaults;
import com.jannik_kuehn.common.storage.model.TimeEntryReason;
import com.jannik_kuehn.common.utils.TimeUtil;

import java.time.Instant;
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
     * @param timeToRemove   the AFK time to remove in seconds.
     */
    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public void executePlayerAfk(final LoriTimePlayer loriTimePlayer, final long timeToRemove) {
        log.debug("Executing AFK for player: " + loriTimePlayer.getName() + ". Time to remove: " + timeToRemove);
        if (!afkEnabled || !isOnline(loriTimePlayer.getUniqueId())) {
            log.debug("AFK is not enabled or player is not online. Skipping the process");
            return;
        }

        final AfkTransition transition = determineAfkStartTransition(loriTimePlayer, timeToRemove);
        persistAfkStart(loriTimePlayer, timeToRemove);
        if (removeTimeEnabled && !hasPermission(loriTimePlayer, "loritime.afk.bypass.timeRemove")) {
            try {
                log.debug("Removing online time for player " + loriTimePlayer.getUniqueId()
                        + ". Time to remove: " + transition.timeToRemove());
                loriTimePlugin.getAccumulator().flushOnlineTimeCache();
                final TimeScope scope = loriTimePlugin.getAccumulator().getActiveSessionContext(loriTimePlayer.getUniqueId())
                        .map(context -> TimeScope.world(context.server(), context.world()))
                        .orElse(TimeScope.world(SessionContextDefaults.SERVER, SessionContextDefaults.WORLD));
                loriTimePlugin.getStorage().addTime(new ManualTimeAdjustment(loriTimePlayer.getUniqueId(),
                        -transition.timeToRemove(), TimeEntryReason.AFK_ADJUSTMENT, "SYSTEM", scope));
            } catch (final Exception e) {
                log.warn("Error while removing online time while afk for player " + loriTimePlayer.getUniqueId(), e);
            }
        }

        if (transition.type() == AfkTransitionType.KICK) {
            log.debug("Kicking player " + loriTimePlayer.getName() + " because he's afk for too long");
            loriTimePlugin.markAfkKick(loriTimePlayer.getUniqueId());
            loriTimePlugin.getServer().kickPlayer(loriTimePlayer, loriTimePlugin.getLocalization()
                    .formatTextComponentWithoutPrefix(loriTimePlugin.getLocalization().getRawMessage("message.afk.kick")
                            .replace("[player]", loriTimePlayer.getName())
                            .replace("[time]", TimeUtil.formatTime(transition.timeToRemove(), loriTimePlugin.getLocalization()))
                    ));
            sendKickAnnounce(loriTimePlayer, transition.timeToRemove(), "loritime.afk.announce.kick");
            persistAfkEnd(loriTimePlayer.getUniqueId(), AfkPeriodEndReason.KICKED);
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
        final AfkTransition transition = new AfkTransition(loriTimePlayer, AfkTransitionType.RESUME, 0L);
        chatAnnounce(loriTimePlayer, "message.afk.resumeAnnounce", "loritime.afk.announce.afkAnnounce");
        selfAfkMessage(loriTimePlayer, "message.afk.afkResume");
        persistAfkEnd(loriTimePlayer.getUniqueId(), AfkPeriodEndReason.RESUMED);
        if (transition.type() == AfkTransitionType.RESUME && playersStoppedForAfk.remove(loriTimePlayer.getUniqueId())) {
            startAccumulatingOnlineTime(transition.player());
        }
    }

    private void persistAfkStart(final LoriTimePlayer player, final long secondsAfk) {
        final long now = System.currentTimeMillis();
        final java.util.Optional<PlayerSessionContext> context = loriTimePlugin.getAccumulator()
                .getActiveSessionContext(player.getUniqueId());
        final String server = context.map(PlayerSessionContext::server).orElse(SessionContextDefaults.SERVER);
        final String world = context.map(PlayerSessionContext::world).orElse(SessionContextDefaults.WORLD);
        loriTimePlugin.getStatisticsStorage().ifPresent(storage -> {
            try {
                storage.openAfkPeriod(player.getUniqueId(), player.getName(), server, world,
                        Instant.ofEpochMilli(now).minusSeconds(Math.max(0L, secondsAfk)));
            } catch (final StorageException ex) {
                log.error("Could not open AFK period for " + player.getUniqueId(), ex);
            }
        });
    }

    private void persistAfkEnd(final UUID playerId, final AfkPeriodEndReason reason) {
        loriTimePlugin.getStatisticsStorage().ifPresent(storage -> {
            try {
                storage.closeAfkPeriod(playerId, Instant.now(), reason);
            } catch (final StorageException ex) {
                log.error("Could not close AFK period for " + playerId + " as " + reason, ex);
            }
        });
    }

    private AfkTransition determineAfkStartTransition(final LoriTimePlayer loriTimePlayer, final long timeToRemove) {
        final AfkTransitionType type = autoKickEnabled && !hasPermission(loriTimePlayer, "loritime.afk.bypass.kick")
                ? AfkTransitionType.KICK : AfkTransitionType.START;
        return new AfkTransition(loriTimePlayer, type, timeToRemove);
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
                    SessionContextDefaults.SERVER, SessionContextDefaults.WORLD, now);
        } catch (final StorageException e) {
            log.error("error while starting accumulation of online time for player " + loriTimePlayer, e);
        }
    }
}
