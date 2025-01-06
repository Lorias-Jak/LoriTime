package com.jannik_kuehn.loritimevelocity.listener;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.common.CommonSender;
import com.jannik_kuehn.loritimevelocity.util.VelocitySender;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;

/**
 * A listener for sending update notifications to players when they join the server.
 */
public class LoriTimeUpdateVelocityListener {
    /**
     * The {@link LoriTimePlugin} instance.
     */
    private final LoriTimePlugin loriTime;

    /**
     * Creates a new {@link LoriTimeUpdateVelocityListener} instance.
     *
     * @param loriTime The {@link LoriTimePlugin} instance.
     */
    public LoriTimeUpdateVelocityListener(final LoriTimePlugin loriTime) {
        this.loriTime = loriTime;
    }

    /**
     * Sends an update notification to the player when they join the server.
     * The notification is sent asynchronously to prevent any lag.
     *
     * @param event The {@link PostLoginEvent} event.
     */
    @Subscribe
    public void onPostLogin(final PostLoginEvent event) {
        final CommonSender sender = new VelocitySender(event.getPlayer());
        //ToDo
//        loriTime.getScheduler().runAsyncOnceLater(1L, () -> loriTime.getUpdateCheck().sendUpdateNotification(sender));
    }
}
