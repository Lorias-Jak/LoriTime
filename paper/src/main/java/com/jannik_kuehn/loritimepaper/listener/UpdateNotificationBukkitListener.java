package com.jannik_kuehn.loritimepaper.listener;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.common.CommonSender;
import com.jannik_kuehn.loritimepaper.util.BukkitPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * A listener for sending update notifications to players when they join the server.
 */
public class UpdateNotificationBukkitListener implements Listener {
    /**
     * The {@link LoriTimePlugin} instance.
     */
    private final LoriTimePlugin loriTime;

    /**
     * Creates a new {@link UpdateNotificationBukkitListener} instance.
     *
     * @param loriTime The {@link LoriTimePlugin} instance.
     */
    public UpdateNotificationBukkitListener(final LoriTimePlugin loriTime) {
        this.loriTime = loriTime;
    }

    /**
     * Sends an update notification to the player when they join the server.
     * The notification is sent asynchronously to prevent any lag.
     *
     * @param event The {@link PlayerJoinEvent} event.
     */
    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final CommonSender sender = new BukkitPlayer(event.getPlayer());
        loriTime.getScheduler().runAsyncOnceLater(1L, () -> loriTime.getUpdateCheck().sendUpdateNotification(sender));
    }
}
