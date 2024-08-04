package com.jannik_kuehn.loritimebukkit.afk;

import com.jannik_kuehn.common.api.LoriTimePlayer;
import com.jannik_kuehn.common.module.afk.AfkHandling;
import com.jannik_kuehn.loritimebukkit.LoriTimeBukkit;

public class BukkitSlavedAfkHandling extends AfkHandling {

    private final LoriTimeBukkit bukkitPlugin;

    public BukkitSlavedAfkHandling(final LoriTimeBukkit bukkitPlugin) {
        super(bukkitPlugin.getPlugin());
        this.bukkitPlugin = bukkitPlugin;
    }

    @Override
    public void executePlayerAfk(final LoriTimePlayer loriTimePlayer, final long timeToRemove) {
        bukkitPlugin.getBukkitPluginMessenger().sendPluginMessage("loritime:afk",
                loriTimePlayer.getUniqueId(), "true", timeToRemove);
    }

    @Override
    public void executePlayerResume(final LoriTimePlayer loriTimePlayer) {
        bukkitPlugin.getBukkitPluginMessenger().sendPluginMessage("loritime:afk",
                loriTimePlayer.getUniqueId(), "false");
    }
}
