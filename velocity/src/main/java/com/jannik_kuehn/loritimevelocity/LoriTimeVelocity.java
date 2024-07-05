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
import com.velocitypowered.api.plugin.PluginContainer;
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

    private final ArrayList<VelocityCommand> commands;

    private final Metrics.Factory metricsFactory;

    private LoriTimePlugin loriTimePlugin;

    @Inject
    public LoriTimeVelocity(final ProxyServer server, final Logger logger, @DataDirectory final Path dataDirectory, final Metrics.Factory metricsFactory) {
        this.proxyServer = server;
        this.logger = new VelocityLogger(logger);
        this.dataDirectory = dataDirectory;
        this.metricsFactory = metricsFactory;
        this.commands = new ArrayList<>();
    }

    @Subscribe
    public void onInitialize(final ProxyInitializeEvent event) {
        final PluginContainer container = proxyServer.getPluginManager().ensurePluginContainer(this);
        if (container.getDescription().getVersion().isEmpty()) {
            logger.severe("Could not get the version of the plugin! Pls report this to the dev!");
        }
        final String version = container.getDescription().getVersion().get();

        final VelocityServer velocityServer = new VelocityServer(version);
        this.loriTimePlugin = new LoriTimePlugin(logger, dataDirectory.toFile(), new VelocityScheduleAdapter(this, proxyServer.getScheduler()), velocityServer);
        velocityServer.enable(proxyServer);
        try {
            loriTimePlugin.enable();
            LoriTimeAPI.setPlugin(loriTimePlugin);
        } catch (final Exception e) {
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

        metricsFactory.make(this, 22484);
    }

    private void enableAsMaster() {
        final EventManager eventManager = proxyServer.getEventManager();
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
    public void onShutDown(final ProxyShutdownEvent event) {
        for (final VelocityCommand command : commands) {
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
