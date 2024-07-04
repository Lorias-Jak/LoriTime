package com.jannik_kuehn.loritimebukkit.listener;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.common.CommonSender;
import com.jannik_kuehn.loritimebukkit.util.BukkitPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class UpdateNotificationBukkitListener implements Listener {

    private final LoriTimePlugin loriTime;

    public UpdateNotificationBukkitListener(final LoriTimePlugin loriTime) {
        this.loriTime = loriTime;
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final CommonSender sender = new BukkitPlayer(event.getPlayer());
        loriTime.getScheduler().runAsyncOnceLater(1L, () -> loriTime.getUpdateCheck().sendUpdateNotification(sender));
    }
}
