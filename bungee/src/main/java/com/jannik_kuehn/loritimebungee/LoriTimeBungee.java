package com.jannik_kuehn.loritimebungee;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.LoriTimeAPI;
import com.jannik_kuehn.common.api.common.CommonLogger;
import com.jannik_kuehn.common.command.LoriTimeAdminCommand;
import com.jannik_kuehn.common.command.LoriTimeCommand;
import com.jannik_kuehn.common.command.LoriTimeInfoCommand;
import com.jannik_kuehn.common.command.LoriTimeTopCommand;
import com.jannik_kuehn.common.module.afk.MasteredAfkPlayerHandling;
import com.jannik_kuehn.loritimebungee.command.BungeeCommand;
import com.jannik_kuehn.loritimebungee.listener.PlayerNameBungeeListener;
import com.jannik_kuehn.loritimebungee.listener.TimeAccumulatorBungeeListener;
import com.jannik_kuehn.loritimebungee.listener.UpdateNotificationBungeeListener;
import com.jannik_kuehn.loritimebungee.schedule.BungeeScheduleAdapter;
import com.jannik_kuehn.loritimebungee.util.BungeeLogger;
import com.jannik_kuehn.loritimebungee.util.BungeeMetrics;
import com.jannik_kuehn.loritimebungee.util.BungeeServer;
import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import org.bstats.bungeecord.Metrics;

public class LoriTimeBungee extends Plugin {

    private LoriTimePlugin loriTimePlugin;

    private BungeeAudiences audiences;

    @Override
    public void onEnable() {
        final CommonLogger logger = new BungeeLogger(getProxy().getLogger());
        final BungeeScheduleAdapter scheduleAdapter = new BungeeScheduleAdapter(this, getProxy().getScheduler());
        audiences = BungeeAudiences.create(this);
        final BungeeServer bungeeServer = new BungeeServer(getDescription().getVersion(), audiences);
        this.loriTimePlugin = new LoriTimePlugin(logger, this.getDataFolder(), scheduleAdapter, bungeeServer);
        bungeeServer.enable(getProxy());
        try {
            loriTimePlugin.enable();
            LoriTimeAPI.setPlugin(loriTimePlugin);
        } catch (final Exception e) {
            loriTimePlugin.disable();
            logger.warning("Error while enabling the plugin! Disabling the plugin...", e);
            return;
        }

        if (bungeeServer.getServerMode().equalsIgnoreCase("master")) {
            enableAsMaster();
        } else if (bungeeServer.getServerMode().equalsIgnoreCase("slave")) {
            enableAsSlave();
        } else {
            logger.severe("Server mode is not set correctly! Please set the server mode to 'master' or 'slave' in the config.yml. Disabling the plugin...");
            loriTimePlugin.disable();
        }
        enableRemainingFeatures();
        new BungeeMetrics(this, new Metrics(this, 22499));
    }

    private void enableAsMaster() {
        final PluginManager pluginManager = getProxy().getPluginManager();
        pluginManager.registerListener(this, new PlayerNameBungeeListener(loriTimePlugin));
        pluginManager.registerListener(this, new TimeAccumulatorBungeeListener(loriTimePlugin));
        pluginManager.registerListener(this, new UpdateNotificationBungeeListener(loriTimePlugin, audiences));

        new BungeeCommand(this, audiences, new LoriTimeAdminCommand(loriTimePlugin, loriTimePlugin.getLocalization(),
                loriTimePlugin.getParser()));
        new BungeeCommand(this, audiences, new LoriTimeCommand(loriTimePlugin, loriTimePlugin.getLocalization()));
        new BungeeCommand(this, audiences, new LoriTimeInfoCommand(loriTimePlugin, loriTimePlugin.getLocalization()));
        new BungeeCommand(this, audiences, new LoriTimeTopCommand(loriTimePlugin, loriTimePlugin.getLocalization()));
    }

    private void enableAsSlave() {
        getLogger().warning("Slave mode is not supported on Proxys! Disabling the plugin...");
        loriTimePlugin.disable();
    }

    private void enableRemainingFeatures() {
        if (loriTimePlugin.isAfkEnabled()) {
            getProxy().registerChannel("loritime:afk");
            getProxy().getPluginManager().registerListener(this, new BungeePluginMessenger(this));
            loriTimePlugin.enableAfkFeature(new MasteredAfkPlayerHandling(loriTimePlugin));
        }
    }

    @Override
    public void onDisable() {
        loriTimePlugin.disable();
        audiences.close();
        getProxy().getPluginManager().unregisterListeners(this);
        getProxy().getPluginManager().unregisterCommands(this);
    }

    public LoriTimePlugin getPlugin() {
        return loriTimePlugin;
    }
}
