package com.jannik_kuehn.loritimevelocity.listener;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.common.CommonSender;
import com.jannik_kuehn.loritimevelocity.util.VelocitySender;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;

public class UpdateNotificationVelocityListener {

    private final LoriTimePlugin loriTime;

    public UpdateNotificationVelocityListener(LoriTimePlugin loriTime) {
        this.loriTime = loriTime;
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        CommonSender sender = new VelocitySender(loriTime, event.getPlayer());
        loriTime.getScheduler().runAsyncOnceLater(1L, () -> loriTime.getUpdateCheck().playerUpdateNotification(sender));
    }
}
