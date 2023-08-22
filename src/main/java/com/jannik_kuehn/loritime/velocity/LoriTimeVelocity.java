package com.jannik_kuehn.loritime.velocity;

import com.jannik_kuehn.loritime.common.LoriTimePlugin;
import com.jannik_kuehn.loritime.common.command.LoriTimeAdminCommand;
import com.jannik_kuehn.loritime.common.command.LoriTimeCommand;
import com.jannik_kuehn.loritime.common.command.LoriTimeInfoCommand;
import com.jannik_kuehn.loritime.common.command.LoriTimeTopCommand;
import com.jannik_kuehn.loritime.velocity.module.command.VelocityCommand;
import com.jannik_kuehn.loritime.velocity.module.listener.TimeAccumulatorVelocityListener;
import com.jannik_kuehn.loritime.velocity.module.listener.PlayerNameVelocityListener;
import com.jannik_kuehn.loritime.velocity.module.schedule.VelocityScheduleAdapter;
import com.jannik_kuehn.loritime.velocity.util.VelocityLogger;
import com.jannik_kuehn.loritime.velocity.util.VelocityServer;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.ArrayList;

public class LoriTimeVelocity {
    private final Path dataDirectory;
    private final VelocityLogger logger;
    private final ProxyServer proxyServer;
    private LoriTimePlugin loriTimePlugin;
    private final ArrayList<VelocityCommand> commands;

    @Inject
    public LoriTimeVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.logger = new VelocityLogger(logger);
        this.proxyServer = server;
        this.commands = new ArrayList<>();
    }

    @Subscribe
    public void onInitialize(ProxyInitializeEvent event) {
        VelocityServer velocityServer = new VelocityServer();
        this.loriTimePlugin = new LoriTimePlugin(logger, dataDirectory.toFile(), new VelocityScheduleAdapter(this, proxyServer.getScheduler()), velocityServer);
        velocityServer.enable(loriTimePlugin, proxyServer);

        try {
            loriTimePlugin.enable();
        } catch (Exception e) {
            loriTimePlugin.disable();
            throw new RuntimeException(e);
        }

        enableListener();
        enableCommands();
    }

    private void enableListener() {
        EventManager eventManager = proxyServer.getEventManager();
        eventManager.register(this, new PlayerNameVelocityListener(loriTimePlugin, loriTimePlugin.getNameStorage()));
        eventManager.register(this, new TimeAccumulatorVelocityListener(loriTimePlugin, loriTimePlugin.getTimeStorage()));
    }

    private void enableCommands() {
        commands.add(new VelocityCommand(this, new LoriTimeAdminCommand(loriTimePlugin, loriTimePlugin.getLocalization(),
                loriTimePlugin.getParser())));
        commands.add(new VelocityCommand(this, new LoriTimeCommand(loriTimePlugin, loriTimePlugin.getLocalization())));
        commands.add(new VelocityCommand(this, new LoriTimeInfoCommand(loriTimePlugin, loriTimePlugin.getLocalization())));
        commands.add(new VelocityCommand(this, new LoriTimeTopCommand(loriTimePlugin, loriTimePlugin.getLocalization())));
    }

    @Subscribe
    public void onShutDown(ProxyShutdownEvent event) {
        for (VelocityCommand command : commands) {
            command.unregisterCommand();
        }
        proxyServer.getEventManager().unregisterListeners(this);
        loriTimePlugin.disable();
    }

    public ProxyServer getProxyServer() {
        return proxyServer;
    }

    public LoriTimePlugin getLoriTimePlugin() {
        return loriTimePlugin;
    }

}
