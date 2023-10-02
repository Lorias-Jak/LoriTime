package com.jannik_kuehn.loritime.bukkit.afk;

import com.jannik_kuehn.loritime.api.LoriTimePlayer;
import com.jannik_kuehn.loritime.bukkit.LoriTimeBukkit;
import com.jannik_kuehn.loritime.common.module.afk.AfkHandling;
import org.bukkit.Bukkit;

public class BukkitSlavedAfkHandling extends AfkHandling {

    private final LoriTimeBukkit bukkitPlugin;

    public BukkitSlavedAfkHandling(LoriTimeBukkit bukkitPlugin) {
        super(bukkitPlugin.getLoriTimePlugin());
        this.bukkitPlugin = bukkitPlugin;
    }

    @Override
    public void executePlayerAfk(LoriTimePlayer loriTimePlayer, long timeToRemove) {
        bukkitPlugin.getPluginMessanger().sendPluginMessage(Bukkit.getServer().getPlayer(
                loriTimePlayer.getUniqueId()), "loritime:afk", "true", timeToRemove);
    }

    @Override
    public void executePlayerResume(LoriTimePlayer loriTimePlayer) {
        bukkitPlugin.getPluginMessanger().sendPluginMessage(Bukkit.getServer().getPlayer(
                loriTimePlayer.getUniqueId()), "loritime:afk", "false");
    }
}
