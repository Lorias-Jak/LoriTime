package com.jannik_kuehn.loritimevelocity;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.LoriTimeAPI;
import com.jannik_kuehn.common.api.logger.LoriTimeLogger;
import com.jannik_kuehn.common.command.LoriTimeAdminCommand;
import com.jannik_kuehn.common.command.LoriTimeCommand;
import com.jannik_kuehn.common.command.LoriTimeDebugCommand;
import com.jannik_kuehn.common.command.LoriTimeInfoCommand;
import com.jannik_kuehn.common.command.LoriTimeTopCommand;
import com.jannik_kuehn.common.module.afk.MasteredAfkPlayerHandling;
import com.jannik_kuehn.loritimevelocity.command.VelocityCommand;
import com.jannik_kuehn.loritimevelocity.listener.LoriTimeUpdateVelocityListener;
import com.jannik_kuehn.loritimevelocity.listener.PlayerNameVelocityListener;
import com.jannik_kuehn.loritimevelocity.listener.TimeAccumulatorVelocityListener;
import com.jannik_kuehn.loritimevelocity.schedule.VelocityScheduleAdapter;
import com.jannik_kuehn.loritimevelocity.util.VelocityMetrics;
import com.jannik_kuehn.loritimevelocity.util.VelocityServer;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import jakarta.inject.Inject;
import org.bstats.velocity.Metrics;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"PMD.CommentRequired"})
public class LoriTimeVelocity {
    private final Path dataDirectory;

    private final ProxyServer proxyServer;

    private final List<VelocityCommand> commands;

    private final Metrics.Factory metricsFactory;

    private final Logger pluginLogger;

    private LoriTimePlugin loriTimePlugin;

    private LoriTimeLogger log;

    @Inject
    public LoriTimeVelocity(final ProxyServer server, final Logger logger, @DataDirectory final Path dataDirectory, final Metrics.Factory metricsFactory) {
        this.proxyServer = server;
        this.pluginLogger = logger;
        this.dataDirectory = dataDirectory;
        this.metricsFactory = metricsFactory;
        this.commands = new ArrayList<>();
    }

    @Subscribe
    @SuppressWarnings({"PMD.UseUnderscoresInNumericLiterals", "PMD.AvoidLiteralsInIfCondition"})
    public void onInitialize(final ProxyInitializeEvent event) {
        final VelocityServer velocityServer = new VelocityServer(pluginLogger);
        this.loriTimePlugin = new LoriTimePlugin(dataDirectory.toFile(), new VelocityScheduleAdapter(this,
                proxyServer.getScheduler()), velocityServer, null);
        this.log = loriTimePlugin.getLoggerFactory().create(LoriTimeVelocity.class);

        final PluginContainer container = proxyServer.getPluginManager().ensurePluginContainer(this);
        if (container.getDescription().getVersion().isEmpty()) {
            log.error("Could not get the version of the plugin! Pls report this to the dev!");
        }
        final String version = container.getDescription().getVersion().get();
        velocityServer.enable(proxyServer, version);

        loriTimePlugin.enable();
        LoriTimeAPI.setPlugin(loriTimePlugin);

        if ("master".equalsIgnoreCase(velocityServer.getServerMode())) {
            enableAsMaster();
        } else if ("slave".equalsIgnoreCase(velocityServer.getServerMode())) {
            enableAsSlave();
        } else {
            log.error("Server mode is not set correctly! Please set the server mode to 'master' or 'slave' in the config.yml. Disabling the plugin...");
            loriTimePlugin.disable();
        }
        enableRemainingFeatures();

        new VelocityMetrics(this, metricsFactory.make(this, 22484));
        log.debug("LoriTime enabled to its complete!");
    }

    private void enableAsMaster() {
        final EventManager eventManager = proxyServer.getEventManager();
        eventManager.register(this, new PlayerNameVelocityListener(loriTimePlugin));
        eventManager.register(this, new TimeAccumulatorVelocityListener(loriTimePlugin));
        eventManager.register(this, new LoriTimeUpdateVelocityListener(loriTimePlugin));

        commands.add(new VelocityCommand(this, new LoriTimeAdminCommand(loriTimePlugin, loriTimePlugin.getLocalization(),
                loriTimePlugin.getParser())));
        commands.add(new VelocityCommand(this, new LoriTimeCommand(loriTimePlugin, loriTimePlugin.getLocalization())));
        commands.add(new VelocityCommand(this, new LoriTimeInfoCommand(loriTimePlugin, loriTimePlugin.getLocalization())));
        commands.add(new VelocityCommand(this, new LoriTimeTopCommand(loriTimePlugin, loriTimePlugin.getLocalization())));
        commands.add(new VelocityCommand(this, new LoriTimeDebugCommand(loriTimePlugin, loriTimePlugin.getLocalization())));
    }

    private void enableAsSlave() {
        log.warn("Slave mode is not supported on Proxys! Disabling the plugin...");
        loriTimePlugin.disable();
    }

    private void enableRemainingFeatures() {
        if (loriTimePlugin.isAfkEnabled()) {
            proxyServer.getChannelRegistrar().register(MinecraftChannelIdentifier.from("loritime:afk"));
            proxyServer.getChannelRegistrar().register(MinecraftChannelIdentifier.from("loritime:storage"));
            proxyServer.getEventManager().register(this, new VelocityPluginMessenger(this));
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
