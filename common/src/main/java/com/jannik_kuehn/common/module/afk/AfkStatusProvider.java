package com.jannik_kuehn.common.module.afk;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.LoriTimePlayer;
import com.jannik_kuehn.common.api.logger.LoriTimeLogger;
import com.jannik_kuehn.common.api.scheduler.PluginTask;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;

public class AfkStatusProvider {
    private final LoriTimePlugin loriTimePlugin;

    private final LoriTimeLogger log;

    private final ConcurrentHashMap<LoriTimePlayer, Long> afkCheckedPlayers;

    private final AfkHandling afkPlayerHandling;

    private PluginTask afkCheck;

    private long afkConfigTime;

    public AfkStatusProvider(final LoriTimePlugin loriTimePlugin, final AfkHandling afkHandling) {
        this.loriTimePlugin = loriTimePlugin;
        this.log = loriTimePlugin.getLoggerFactory().create(AfkStatusProvider.class, "AfkStatusProvider");
        this.afkCheckedPlayers = new ConcurrentHashMap<>();
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
        final HashMap<LoriTimePlayer, Long> playersToCheck = new HashMap<>(afkCheckedPlayers);
        for (final Map.Entry<LoriTimePlayer, Long> entry : playersToCheck.entrySet()) {
            final LoriTimePlayer player = entry.getKey();
            final long playerAfkTime = entry.getValue();
            final long currentTime = System.currentTimeMillis();
            if (loriTimePlugin.getServer().getPlayer(player.getUniqueId()).isEmpty()) {
                log.debug("Player is not online anymore. Removing player from afk-check. Continue with next player");
                afkCheckedPlayers.remove(player);
                continue;
            }
            if (getRealPlayer(player).isAfk()) {
                log.debug("Player is already afk. Continue with next player");
                continue;
            }
            if (currentTime - playerAfkTime >= afkConfigTime) {
                log.debug("Player is afk now. Setting player to afk");
                getRealPlayer(player).setAFk(true);
                final long timeToRemove = (currentTime - playerAfkTime) / 1000L;
                afkPlayerHandling.executePlayerAfk(player, timeToRemove);
            }
        }

    }

    public void resetTimer(final LoriTimePlayer player) {
        if (player.isAfk()) {
            log.debug("Player is afk. Resuming player cause of reset");
            player.setAFk(false);
            afkPlayerHandling.executePlayerResume(player);
        }
        afkCheckedPlayers.put(player, System.currentTimeMillis());
    }

    public LoriTimePlayer getRealPlayer(final LoriTimePlayer player) {
        LoriTimePlayer playerToReturn = player;
        for (final LoriTimePlayer loriTimePlayer : afkCheckedPlayers.keySet()) {
            if (player.equals(loriTimePlayer)) {
                playerToReturn = loriTimePlayer;
            }
        }
        afkCheckedPlayers.put(playerToReturn, System.currentTimeMillis());
        return playerToReturn;
    }

    public void playerLeft(final LoriTimePlayer player) {
        afkCheckedPlayers.remove(player);
    }

    public AfkHandling getAfkPlayerHandling() {
        return afkPlayerHandling;
    }

    public void switchPlayerAfk(final LoriTimePlayer player) {
        switchPlayerAFK(player, 0);
    }

    public void switchPlayerAFK(final LoriTimePlayer player, final long timeToRemove) {
        log.debug("Switching player afk status from '" + player.getName() + "'");
        final LoriTimePlayer target = getRealPlayer(player);
        if (!target.isAfk()) {
            log.debug("Setting player" + target.getName() + "to afk");
            target.setAFk(true);
            afkPlayerHandling.executePlayerAfk(target, timeToRemove);
        } else {
            log.debug("Resuming player '" + target.getName() + "'");
            target.setAFk(false);
            afkPlayerHandling.executePlayerResume(target);
        }
    }

    public void setPlayerAFK(final LoriTimePlayer player, final long timeToRemove) {
        log.debug("Setting player '" + player.getName() + "' to afk");
        final LoriTimePlayer target = getRealPlayer(player);
        if (target.isAfk()) {
            log.debug("Player '" + target.getName() + "' is already afk. Skipping the process...");
            return;
        }
        target.setAFk(true);
        afkPlayerHandling.executePlayerAfk(target, timeToRemove);
    }

    public void resumePlayerAFK(final LoriTimePlayer player) {
        log.debug("Resuming player '" + player.getName() + "'");
        final LoriTimePlayer target = getRealPlayer(player);
        if (!target.isAfk()) {
            log.debug("Player '" + target.getName() + "' is not afk. Skipping the resuming");
            return;
        }
        target.setAFk(false);
        afkPlayerHandling.executePlayerResume(target);
    }
}
