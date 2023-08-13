package com.jannik_kuehn.loritime.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * The {@link LoriTimeVelocity} class is the main-class of the Plugin for the Velocity Handling.
 * It starts the plugin correctly up and shuts it down.
 */
@Plugin(id = "loritime",
        name = "LoriTime",
        version ="1.0.0-SNAPSHOT",
        url = "https://github.com/Lorias-Jak/LoriTime",
        description = "A plugin to capture the time that a player spends on your server network",
        authors = {"Lorias-Jak"}
)
public class LoriTimeVelocity {

    /**
     * The {@link ProxyServer} instance.
     */
    private final ProxyServer proxyServer;

    /**
     * The {@link Logger} instance.
     */
    private final Logger logger;

    /**
     * The Plugin directory as {@link Path}.
     */
    private final Path pluginDirectory;

    /**
     * The starting instance for Velocity to hookl in.
     *
     * @param server the {@link ProxyServer}.
     * @param logger the {@link Logger}.
     * @param dataDirectory the plugins directory as {@link Path}.
     */
    @Inject
    public LoriTimeVelocity(final ProxyServer server, final Logger logger, final @DataDirectory Path dataDirectory) {
        this.proxyServer = server;
        this.logger = logger;
        this.pluginDirectory = dataDirectory;
    }

    /**
     * The method is executed in the startup of velocity.
     *
     * @param event is the {@link ProxyInitializeEvent} from velocity.
     */
    @Subscribe
    public void onProxyInitialize(final ProxyInitializeEvent event) {
        // Empty
    }

    /**
     * The event when the proxy is shutdown.
     *
     * @param event is the {@link ProxyShutdownEvent} from Velocity.
     */
    @Subscribe
    public void onShutDown(final ProxyShutdownEvent event) {
        // Empty
    }


}
