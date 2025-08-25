package com.jannik_kuehn.loritimepaper.listener;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.common.CommonSender;
import com.jannik_kuehn.loritimepaper.util.PaperPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * A listener for sending update notifications to players when they join the server.
 */
public class LoriTimeUpdatePaperListener implements Listener {
    /**
     * The {@link LoriTimePlugin} instance.
     */
    private final LoriTimePlugin loriTime;

    /**
     * Creates a new {@link LoriTimeUpdatePaperListener} instance.
     *
     * @param loriTime The {@link LoriTimePlugin} instance.
     */
    public LoriTimeUpdatePaperListener(final LoriTimePlugin loriTime) {
        this.loriTime = loriTime;
    }

    /**
     * Sends an update notification to the player when they join the server.
     *
     * @param event The {@link PlayerJoinEvent} event.
     */
    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final CommonSender sender = new PaperPlayer(event.getPlayer());
        loriTime.getUpdater().sendPlayerUpdateNotification(sender);
    }
}
