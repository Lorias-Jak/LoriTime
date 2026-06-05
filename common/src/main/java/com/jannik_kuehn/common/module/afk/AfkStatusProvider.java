package com.jannik_kuehn.common.module.afk;

import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.player.TrackedLoriTimePlayer;
import com.jannik_kuehn.common.scheduler.PluginTask;

import java.util.OptionalLong;

/**
 * Tracks AFK timers and delegates AFK state transitions to an {@link AfkHandling}.
 */
@SuppressWarnings("PMD.TooManyMethods")
public class AfkStatusProvider {
    private final LoriTimePlugin loriTimePlugin;

    private final WrappedLogger log;

    private final AfkHandling afkPlayerHandling;

    private PluginTask afkCheck;

    private long afkConfigTime;

    /**
     * Creates and starts an AFK status provider.
     *
     * @param loriTimePlugin LoriTime plugin runtime
     * @param afkHandling    platform-specific AFK handler
     */
    public AfkStatusProvider(final LoriTimePlugin loriTimePlugin, final AfkHandling afkHandling) {
        this.loriTimePlugin = loriTimePlugin;
        this.log = loriTimePlugin.getLoggerFactory().create(AfkStatusProvider.class, "AfkStatusProvider");
        this.afkPlayerHandling = afkHandling;

        reloadConfigValues();
        restartAfkCheck();
    }

    /**
     * Reloads AFK timing and handler configuration.
     */
    public final void reloadConfigValues() {
        log.debug("Reloading config values of AfkStatusProvider");
        afkPlayerHandling.reloadConfigValues();
        final OptionalLong afkConfigOptional = loriTimePlugin.getParser().parseToSeconds(loriTimePlugin.getConfig().getString("afk.after", "15m"));
        if (afkConfigOptional.isEmpty()) {
            log.error("Can not start the afk-check. No valid afk-config-value present! Skipping the process...");
            return;
        }
        afkConfigTime = afkConfigOptional.getAsLong() * 1000L;
        log.debug("Reloaded afkConfigTime. New value: " + afkConfigTime);
    }

    /**
     * Restarts the periodic AFK check according to the current configuration.
     */
    public final void restartAfkCheck() {
        log.debug("Restarting afk-check");
        stopAfkCheck();
        final boolean afkEnabled = loriTimePlugin.getConfig().getBoolean("afk.enabled", false) && !loriTimePlugin.getServer().isProxy();
        log.debug("Afk enabled: " + afkEnabled);
        if (afkEnabled) {
            startAfkCheck();
        }
    }

    private void startAfkCheck() {
        if (afkConfigTime <= 0) {
            log.error("afk.after time needs to be at least 1! Disabling afk feature...");
            return;
        }
        log.debug("Starting afk-check");
        final int repeatTime = loriTimePlugin.getConfig().getInt("afk.repeatCheck", 30);
        afkCheck = loriTimePlugin.getScheduler().scheduleAsync(repeatTime / 2L, repeatTime, this::repeatedTimeCheck);
    }

    private void stopAfkCheck() {
        log.debug("Check for afk-check running");
        if (afkCheck != null) {
            log.debug("Stopped afk-check");
            afkCheck.cancel();
            afkCheck = null;
        }
    }

    private void repeatedTimeCheck() {
        log.debug("RepeatedTimeCheck started, checking for player afk status");
        loriTimePlugin.getPlayerConverter().getOnlinePlayers().forEach(this::computeAfkPlayers);
    }

    private void computeAfkPlayers(final TrackedLoriTimePlayer loriTimePlayer) {
        if (loriTimePlugin.getServer().getPlayer(loriTimePlayer.getUniqueId()).isEmpty()) {
            log.debug("Player is not online anymore. Continue with next player");
            return;
        }
        if (loriTimePlayer.isAfk()) {
            log.debug("Player is already afk. Continue with next player");
            return;
        }
        final long playerAfkTime = loriTimePlayer.getLastResumeTime();
        final long currentTime = System.currentTimeMillis();
        if (currentTime - playerAfkTime >= afkConfigTime) {
            log.debug("Player is afk now. Setting player to afk");
            final long timeToRemove = (currentTime - playerAfkTime) / 1000L;
            setPlayerAFK(loriTimePlayer, timeToRemove);
        }
    }

    /**
     * Resets a player's AFK timer and resumes them if they were AFK.
     *
     * @param player tracked player
     */
    public void resetTimer(final TrackedLoriTimePlayer player) {
        if (player.isAfk()) {
            resumePlayerAFK(player);
        }
        player.setLastResumeTime();
    }

    /**
     * Toggles a player's AFK state without removing time.
     *
     * @param player tracked player
     */
    public void switchPlayerAfk(final TrackedLoriTimePlayer player) {
        switchPlayerAFK(player, 0);
    }

    /**
     * Toggles a player's AFK state.
     *
     * @param player       tracked player
     * @param timeToRemove seconds to remove when entering AFK
     */
    public void switchPlayerAFK(final TrackedLoriTimePlayer player, final long timeToRemove) {
        log.debug("Switching player afk status from '" + player.getName() + "'");
        if (player.isAfk()) {
            log.debug("Resuming player '" + player.getName() + "'");
            player.setAfk(false);
            afkPlayerHandling.executePlayerResume(player);
            return;
        }
        log.debug("Setting player" + player.getName() + "to afk");
        player.setAfk(true);
        afkPlayerHandling.executePlayerAfk(player, timeToRemove);
    }

    /**
     * Marks a player as AFK.
     *
     * @param player       tracked player
     * @param timeToRemove seconds to remove when entering AFK
     */
    public void setPlayerAFK(final TrackedLoriTimePlayer player, final long timeToRemove) {
        log.debug("Setting player '" + player.getName() + "' to afk");
        if (player.isAfk()) {
            log.debug("Player '" + player.getName() + "' is already afk. Skipping the process...");
            return;
        }
        player.setAfk(true);
        afkPlayerHandling.executePlayerAfk(player, timeToRemove);
    }

    /**
     * Resumes a player from AFK.
     *
     * @param player tracked player
     */
    public void resumePlayerAFK(final TrackedLoriTimePlayer player) {
        log.debug("Resuming player '" + player.getName() + "'");
        if (!player.isAfk()) {
            log.debug("Player '" + player.getName() + "' is not afk. Skipping the resuming");
            return;
        }
        player.setAfk(false);
        afkPlayerHandling.executePlayerResume(player);
    }
}
