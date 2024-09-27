package com.jannik_kuehn.loritimebukkit.afk;

import com.jannik_kuehn.common.api.LoriTimePlayer;
import com.jannik_kuehn.common.api.logger.LoriTimeLogger;
import com.jannik_kuehn.common.module.afk.AfkHandling;
import com.jannik_kuehn.loritimebukkit.LoriTimeBukkit;

public class BukkitSlavedAfkHandling extends AfkHandling {

    private final LoriTimeBukkit loriTimeBukkit;

    private final LoriTimeLogger log;

    public BukkitSlavedAfkHandling(final LoriTimeBukkit loriTimeBukkit) {
        super(loriTimeBukkit.getPlugin());
        this.loriTimeBukkit = loriTimeBukkit;
        this.log = loriTimeBukkit.getPlugin().getLoggerFactory().create(BukkitSlavedAfkHandling.class, "SlavedAfkHandling");
    }

    @Override
    public void executePlayerAfk(final LoriTimePlayer loriTimePlayer, final long timeToRemove) {
        log.debug("Executing AFK for player: " + loriTimePlayer.getName() + ". Time to remove: "
                + timeToRemove + ". Sending PluginMessage");
        loriTimeBukkit.getBukkitPluginMessenger().sendPluginMessage("loritime:afk",
                loriTimePlayer.getUniqueId(), "true", timeToRemove);
    }

    @Override
    public void executePlayerResume(final LoriTimePlayer loriTimePlayer) {
        log.debug("Executing resume for player: " + loriTimePlayer.getName() + ". Sending PluginMessage");
        loriTimeBukkit.getBukkitPluginMessenger().sendPluginMessage("loritime:afk",
                loriTimePlayer.getUniqueId(), "false");
    }
}
