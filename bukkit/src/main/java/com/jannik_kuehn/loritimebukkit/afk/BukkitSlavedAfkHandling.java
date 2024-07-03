package com.jannik_kuehn.loritimebukkit.afk;

import com.jannik_kuehn.common.api.LoriTimePlayer;
import com.jannik_kuehn.common.module.afk.AfkHandling;
import com.jannik_kuehn.loritimebukkit.LoriTimeBukkit;
import org.bukkit.Bukkit;

public class BukkitSlavedAfkHandling extends AfkHandling {

    private final LoriTimeBukkit bukkitPlugin;

    public BukkitSlavedAfkHandling(LoriTimeBukkit bukkitPlugin) {
        super(bukkitPlugin.getPlugin());
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
