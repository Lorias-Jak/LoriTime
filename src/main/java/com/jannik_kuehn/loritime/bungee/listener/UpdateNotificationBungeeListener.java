package com.jannik_kuehn.loritime.bungee.listener;

import com.jannik_kuehn.loritime.api.CommonSender;
import com.jannik_kuehn.loritime.bungee.util.BungeeSender;
import com.jannik_kuehn.loritime.common.LoriTimePlugin;
import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class UpdateNotificationBungeeListener implements Listener {

    private final LoriTimePlugin loriTime;

    private final BungeeAudiences bungeeAudiences;

    public UpdateNotificationBungeeListener(LoriTimePlugin loriTime, BungeeAudiences bungeeAudiences) {
        this.loriTime = loriTime;
        this.bungeeAudiences = bungeeAudiences;
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        CommonSender sender = new BungeeSender(bungeeAudiences, event.getPlayer());
        loriTime.getScheduler().runAsyncOnceLater(1L, () -> loriTime.getUpdateCheck().playerUpdateNotification(sender));
    }
}
