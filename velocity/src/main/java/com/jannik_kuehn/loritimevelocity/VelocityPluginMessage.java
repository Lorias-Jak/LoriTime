package com.jannik_kuehn.loritimevelocity;

import com.jannik_kuehn.common.module.messaging.PluginMessaging;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ServerConnection;

public class VelocityPluginMessage extends PluginMessaging {

    public VelocityPluginMessage(final LoriTimeVelocity velocityPlugin) {
        super(velocityPlugin.getPlugin());
    }

    @Subscribe
    public void onPluginMessage(final PluginMessageEvent event) {
        if (!event.getIdentifier().getId().contains("loritime:")) {
            return;
        }
        if (!(event.getSource() instanceof ServerConnection)) {
            return;
        }
        if (event.getResult().equals(PluginMessageEvent.ForwardResult.handled())) {
            return;
        }

        final byte[] data = event.getData();
        processPluginMessage(event.getIdentifier().getId(), data);
        event.setResult(PluginMessageEvent.ForwardResult.handled());
    }
}
