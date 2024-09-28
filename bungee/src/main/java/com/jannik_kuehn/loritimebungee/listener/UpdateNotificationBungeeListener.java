package com.jannik_kuehn.loritimebungee.listener;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.common.CommonSender;
import com.jannik_kuehn.loritimebungee.util.BungeeSender;
import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

/**
 * A listener for sending update notifications to players when they join the server.
 */
public class UpdateNotificationBungeeListener implements Listener {
    /**
     * The {@link LoriTimePlugin} instance.
     */
    private final LoriTimePlugin loriTime;

    /**
     * The {@link BungeeAudiences} instance.
     */
    private final BungeeAudiences bungeeAudiences;

    /**
     * Creates a new {@link UpdateNotificationBungeeListener} instance.
     *
     * @param loriTime        The {@link LoriTimePlugin} instance.
     * @param bungeeAudiences The {@link BungeeAudiences} instance.
     */
    public UpdateNotificationBungeeListener(final LoriTimePlugin loriTime, final BungeeAudiences bungeeAudiences) {
        this.loriTime = loriTime;
        this.bungeeAudiences = bungeeAudiences;
    }

    /**
     * Sends an update notification to the player when they join the server.
     * The notification is sent asynchronously to prevent any lag.
     *
     * @param event The {@link PostLoginEvent} event.
     */
    @EventHandler
    public void onPostLogin(final PostLoginEvent event) {
        final CommonSender sender = new BungeeSender(bungeeAudiences, event.getPlayer());
        loriTime.getScheduler().runAsyncOnceLater(1L, () -> loriTime.getUpdateCheck().sendUpdateNotification(sender));
    }
}
