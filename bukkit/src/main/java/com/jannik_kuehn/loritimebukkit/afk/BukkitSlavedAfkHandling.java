package com.jannik_kuehn.loritimebukkit.afk;

import com.jannik_kuehn.common.api.LoriTimePlayer;
import com.jannik_kuehn.common.api.logger.LoriTimeLogger;
import com.jannik_kuehn.common.module.afk.AfkHandling;
import com.jannik_kuehn.loritimebukkit.LoriTimeBukkit;

/**
 * The {@link AfkHandling} implementation for the slaved servers.
 */
public class BukkitSlavedAfkHandling extends AfkHandling {
    /**
     * The {@link LoriTimeBukkit} instance.
     */
    private final LoriTimeBukkit loriTimeBukkit;

    /**
     * The {@link LoriTimeLogger} instance.
     */
    private final LoriTimeLogger log;

    /**
     * Creates a new instance of the {@link BukkitSlavedAfkHandling}.
     *
     * @param loriTimeBukkit The {@link LoriTimeBukkit} instance.
     */
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
