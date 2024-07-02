package com.jannik_kuehn.loritime.bukkit.listener;

import com.jannik_kuehn.loritime.api.common.CommonSender;
import com.jannik_kuehn.loritime.bukkit.util.BukkitPlayer;
import com.jannik_kuehn.loritime.common.LoriTimePlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class UpdateNotificationBukkitListener implements Listener {

    private final LoriTimePlugin loriTime;

    public UpdateNotificationBukkitListener(LoriTimePlugin loriTime) {
        this.loriTime = loriTime;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        CommonSender sender = new BukkitPlayer(event.getPlayer());
        loriTime.getScheduler().runAsyncOnceLater(1L, () -> loriTime.getUpdateCheck().playerUpdateNotification(sender));
    }
}
