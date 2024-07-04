package com.jannik_kuehn.common.module.afk;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.LoriTimePlayer;
import com.jannik_kuehn.common.api.scheduler.PluginTask;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;

public class AfkStatusProvider {
    private final LoriTimePlugin plugin;

    private final ConcurrentHashMap<LoriTimePlayer, Long> afkCheckedPlayers;

    private final AfkHandling afkPlayerHandling;

    private PluginTask afkCheck;

    private long afkConfigTime;

    public AfkStatusProvider(final LoriTimePlugin loriTimePlugin, final AfkHandling afkHandling) {
        this.plugin = loriTimePlugin;
        this.afkCheckedPlayers = new ConcurrentHashMap<>();
        this.afkPlayerHandling = afkHandling;

        reloadConfigValues();
        restartAfkCheck();
    }

    public void reloadConfigValues() {
        afkPlayerHandling.reloadConfigValues();
        final OptionalLong afkConfigOptional = plugin.getParser().parseToSeconds(plugin.getConfig().getString("afk.after", "15m"));
        if (afkConfigOptional.isEmpty()) {
            plugin.getLogger().severe("Can not start the afk-check. No valid afk-config-value present! Skipping the process...");
            return;
        }
        afkConfigTime = afkConfigOptional.getAsLong() * 1000L;
    }

    public void restartAfkCheck() {
        stopAfkCheck();
        final boolean afkEnabled = plugin.getConfig().getBoolean("afk.enabled", false);
        if (afkEnabled) {
            startAfkCheck();
        }
    }

    private void startAfkCheck() {
        if (afkConfigTime <= 0) {
            plugin.getLogger().severe("afk.after time needs to be at least 1! Disabling afk feature...");
            return;
        }
        final int repeatTime = plugin.getConfig().getInt("afk.repeatCheck", 30);
        afkCheck = plugin.getScheduler().scheduleAsync(repeatTime / 2L, repeatTime, this::repeatedTimeCheck);
    }

    private void stopAfkCheck() {
        if (afkCheck != null) {
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
            if (plugin.getServer().getPlayer(player.getUniqueId()).isEmpty()) {
                afkCheckedPlayers.remove(player);
                continue;
            }
            if (getRealPlayer(player).isAfk()) {
                continue;
            }
            if (currentTime - playerAfkTime >= afkConfigTime) {
                getRealPlayer(player).setAFk(true);
                final long timeToRemove = (currentTime - playerAfkTime) / 1000L;
                afkPlayerHandling.executePlayerAfk(player, timeToRemove);
            }
        }

    }

    public void resetTimer(final LoriTimePlayer player) {
        if (player.isAfk()) {
            player.setAFk(false);
            afkPlayerHandling.executePlayerResume(player);
        }

        afkCheckedPlayers.remove(player);
        afkCheckedPlayers.put(player, System.currentTimeMillis());
    }

    public LoriTimePlayer getRealPlayer(final LoriTimePlayer player) {
        LoriTimePlayer playerToReturn = player;
        for (final LoriTimePlayer loriTimePlayer : afkCheckedPlayers.keySet()) {
            if (player.equals(loriTimePlayer)) {
                playerToReturn = loriTimePlayer;
            }
        }
        return playerToReturn;
    }

    public void playerLeft(final LoriTimePlayer player) {
        afkCheckedPlayers.remove(player);
    }

    public AfkHandling getAfkPlayerHandling() {
        return afkPlayerHandling;
    }

    public void setPlayerAfk(final LoriTimePlayer player) {
        final LoriTimePlayer target = getRealPlayer(player);
        if (!target.isAfk()) {
            target.setAFk(true);
            afkPlayerHandling.executePlayerAfk(target, 0);
        } else {
            target.setAFk(false);
            afkPlayerHandling.executePlayerResume(target);
        }
    }
}
