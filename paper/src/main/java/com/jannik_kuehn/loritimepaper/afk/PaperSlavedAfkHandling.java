package com.jannik_kuehn.loritimepaper.afk;

import com.jannik_kuehn.common.api.LoriTimePlayer;
import com.jannik_kuehn.common.api.logger.LoriTimeLogger;
import com.jannik_kuehn.common.module.afk.AfkHandling;
import com.jannik_kuehn.loritimepaper.LoriTimePaper;

/**
 * The {@link AfkHandling} implementation for the slaved servers.
 */
public class PaperSlavedAfkHandling extends AfkHandling {
    /**
     * The {@link LoriTimePaper} instance.
     */
    private final LoriTimePaper loriTimePaper;

    /**
     * The {@link LoriTimeLogger} instance.
     */
    private final LoriTimeLogger log;

    /**
     * Creates a new instance of the {@link PaperSlavedAfkHandling}.
     *
     * @param loriTimePaper The {@link LoriTimePaper} instance.
     */
    public PaperSlavedAfkHandling(final LoriTimePaper loriTimePaper) {
        super(loriTimePaper.getPlugin());
        this.loriTimePaper = loriTimePaper;
        this.log = loriTimePaper.getPlugin().getLoggerFactory().create(PaperSlavedAfkHandling.class, "SlavedAfkHandling");
    }

    @Override
    public void executePlayerAfk(final LoriTimePlayer loriTimePlayer, final long timeToRemove) {
        log.debug("Executing AFK for player: " + loriTimePlayer.getName() + ". Time to remove: "
                + timeToRemove + ". Sending PluginMessage");
        loriTimePaper.getPaperPluginMessenger().sendPluginMessage("loritime:afk",
                loriTimePlayer.getUniqueId(), "true", timeToRemove);
    }

    @Override
    public void executePlayerResume(final LoriTimePlayer loriTimePlayer) {
        log.debug("Executing resume for player: " + loriTimePlayer.getName() + ". Sending PluginMessage");
        loriTimePaper.getPaperPluginMessenger().sendPluginMessage("loritime:afk",
                loriTimePlayer.getUniqueId(), "false");
    }
}
