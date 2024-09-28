package com.jannik_kuehn.common.module.afk;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.LoriTimePlayer;
import com.jannik_kuehn.common.api.logger.LoriTimeLogger;
import com.jannik_kuehn.common.api.scheduler.PluginTask;

import java.util.OptionalLong;

public class AfkStatusProvider {
    private final LoriTimePlugin loriTimePlugin;

    private final LoriTimeLogger log;

    private final AfkHandling afkPlayerHandling;

    private PluginTask afkCheck;

    private long afkConfigTime;

    public AfkStatusProvider(final LoriTimePlugin loriTimePlugin, final AfkHandling afkHandling) {
        this.loriTimePlugin = loriTimePlugin;
        this.log = loriTimePlugin.getLoggerFactory().create(AfkStatusProvider.class, "AfkStatusProvider");
        this.afkPlayerHandling = afkHandling;

        reloadConfigValues();
        restartAfkCheck();
    }

    public void reloadConfigValues() {
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

    public void restartAfkCheck() {
        log.debug("Restarting afk-check");
        stopAfkCheck();
        final boolean afkEnabled = loriTimePlugin.getConfig().getBoolean("afk.enabled", false);
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

    private void computeAfkPlayers(final LoriTimePlayer loriTimePlayer) {
        final long playerAfkTime = loriTimePlayer.getLastResumeTime();
        final long currentTime = System.currentTimeMillis();
        if (loriTimePlugin.getServer().getPlayer(loriTimePlayer.getUniqueId()).isEmpty()) {
            log.debug("Player is not online anymore. Continue with next player");
            return;
        }
        if (loriTimePlayer.isAfk()) {
            log.debug("Player is already afk. Continue with next player");
            return;
        }
        if (currentTime - playerAfkTime >= afkConfigTime) {
            log.debug("Player is afk now. Setting player to afk");
            final long timeToRemove = (currentTime - playerAfkTime) / 1000L;
            setPlayerAFK(loriTimePlayer, timeToRemove);
        }
    }

    public void resetTimer(final LoriTimePlayer player) {
        if (player.isAfk()) {
            resumePlayerAFK(player);
        }
        player.setLastResumeTime();
    }

    public void switchPlayerAfk(final LoriTimePlayer player) {
        switchPlayerAFK(player, 0);
    }

    public void switchPlayerAFK(final LoriTimePlayer player, final long timeToRemove) {
        log.debug("Switching player afk status from '" + player.getName() + "'");
        if (!player.isAfk()) {
            log.debug("Setting player" + player.getName() + "to afk");
            player.setAFk(true);
            afkPlayerHandling.executePlayerAfk(player, timeToRemove);
        } else {
            log.debug("Resuming player '" + player.getName() + "'");
            player.setAFk(false);
            afkPlayerHandling.executePlayerResume(player);
        }
    }

    public void setPlayerAFK(final LoriTimePlayer player, final long timeToRemove) {
        log.debug("Setting player '" + player.getName() + "' to afk");
        if (player.isAfk()) {
            log.debug("Player '" + player.getName() + "' is already afk. Skipping the process...");
            return;
        }
        player.setAFk(true);
        afkPlayerHandling.executePlayerAfk(player, timeToRemove);
    }

    public void resumePlayerAFK(final LoriTimePlayer player) {
        log.debug("Resuming player '" + player.getName() + "'");
        if (!player.isAfk()) {
            log.debug("Player '" + player.getName() + "' is not afk. Skipping the resuming");
            return;
        }
        player.setAFk(false);
        afkPlayerHandling.executePlayerResume(player);
    }
}
