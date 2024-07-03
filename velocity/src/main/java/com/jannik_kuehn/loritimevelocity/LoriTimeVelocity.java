package com.jannik_kuehn.loritimevelocity;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.LoriTimeAPI;
import com.jannik_kuehn.common.api.common.CommonLogger;
import com.jannik_kuehn.common.command.LoriTimeAdminCommand;
import com.jannik_kuehn.common.command.LoriTimeCommand;
import com.jannik_kuehn.common.command.LoriTimeInfoCommand;
import com.jannik_kuehn.common.command.LoriTimeTopCommand;
import com.jannik_kuehn.common.module.afk.MasteredAfkPlayerHandling;
import com.jannik_kuehn.loritimevelocity.command.VelocityCommand;
import com.jannik_kuehn.loritimevelocity.listener.PlayerNameVelocityListener;
import com.jannik_kuehn.loritimevelocity.listener.TimeAccumulatorVelocityListener;
import com.jannik_kuehn.loritimevelocity.listener.UpdateNotificationVelocityListener;
import com.jannik_kuehn.loritimevelocity.schedule.VelocityScheduleAdapter;
import com.jannik_kuehn.loritimevelocity.util.VelocityLogger;
import com.jannik_kuehn.loritimevelocity.util.VelocityServer;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import org.bstats.velocity.Metrics;
import org.slf4j.Logger;

import javax.inject.Inject;

import java.nio.file.Path;
import java.util.ArrayList;

public class LoriTimeVelocity {
    private final Path dataDirectory;

    private final CommonLogger logger;

    private final ProxyServer proxyServer;

    private LoriTimePlugin loriTimePlugin;

    private final ArrayList<VelocityCommand> commands;

    private final Metrics.Factory metricsFactory;

    @Inject
    public LoriTimeVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory, Metrics.Factory metricsFactory) {
        this.dataDirectory = dataDirectory;
        this.logger = new VelocityLogger(logger);
        this.metricsFactory = metricsFactory;
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
            LoriTimeAPI.setPlugin(loriTimePlugin);
        } catch (Exception e) {
            loriTimePlugin.disable();
            logger.error("Error while enabling the plugin! Disabling the plugin...", e);
        }

        if (velocityServer.getServerMode().equalsIgnoreCase("master")) {
            enableAsMaster();
        } else if (velocityServer.getServerMode().equalsIgnoreCase("slave")) {
            enableAsSlave();
        } else {
            logger.severe("Server mode is not set correctly! Please set the server mode to 'master' or 'slave' in the config.yml. Disabling the plugin...");
            loriTimePlugin.disable();
        }
        enableRemainingFeatures();

//        new VelocityMetrics(loriTimePlugin, metricsFactory.make(this, 22484));
    }

    private void enableAsMaster() {
        EventManager eventManager = proxyServer.getEventManager();
        eventManager.register(this, new PlayerNameVelocityListener(loriTimePlugin));
        eventManager.register(this, new TimeAccumulatorVelocityListener(loriTimePlugin));
        eventManager.register(this, new UpdateNotificationVelocityListener(loriTimePlugin));

        commands.add(new VelocityCommand(this, new LoriTimeAdminCommand(loriTimePlugin, loriTimePlugin.getLocalization(),
                loriTimePlugin.getParser())));
        commands.add(new VelocityCommand(this, new LoriTimeCommand(loriTimePlugin, loriTimePlugin.getLocalization())));
        commands.add(new VelocityCommand(this, new LoriTimeInfoCommand(loriTimePlugin, loriTimePlugin.getLocalization())));
        commands.add(new VelocityCommand(this, new LoriTimeTopCommand(loriTimePlugin, loriTimePlugin.getLocalization())));
    }

    private void enableAsSlave() {
        logger.warning("Slave mode is not supported on Proxys! Disabling the plugin...");
        loriTimePlugin.disable();
    }

    private void enableRemainingFeatures() {
        if (loriTimePlugin.isAfkEnabled()) {
            proxyServer.getChannelRegistrar().register(MinecraftChannelIdentifier.from("loritime:afk"));
            proxyServer.getEventManager().register(this, new VelocityPluginMessage(this));
            loriTimePlugin.enableAfkFeature(new MasteredAfkPlayerHandling(loriTimePlugin));
        }
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

    public LoriTimePlugin getPlugin() {
        return loriTimePlugin;
    }
}
