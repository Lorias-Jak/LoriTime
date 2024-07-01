package com.jannik_kuehn.loritime.velocity.listener;

import com.jannik_kuehn.loritime.api.CommonSender;
import com.jannik_kuehn.loritime.common.LoriTimePlugin;
import com.jannik_kuehn.loritime.velocity.util.VelocitySender;
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
