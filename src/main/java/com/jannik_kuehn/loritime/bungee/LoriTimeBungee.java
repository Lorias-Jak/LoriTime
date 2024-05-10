package com.jannik_kuehn.loritime.bungee;

import com.jannik_kuehn.loritime.api.CommonLogger;
import com.jannik_kuehn.loritime.bungee.command.BungeeCommand;
import com.jannik_kuehn.loritime.bungee.listener.PlayerNameBungeeListener;
import com.jannik_kuehn.loritime.bungee.listener.TimeAccumulatorBungeeListener;
import com.jannik_kuehn.loritime.bungee.schedule.BungeeScheduleAdapter;
import com.jannik_kuehn.loritime.bungee.util.BungeeLogger;
import com.jannik_kuehn.loritime.bungee.util.BungeeServer;
import com.jannik_kuehn.loritime.common.LoriTimePlugin;
import com.jannik_kuehn.loritime.common.command.LoriTimeAdminCommand;
import com.jannik_kuehn.loritime.common.command.LoriTimeCommand;
import com.jannik_kuehn.loritime.common.command.LoriTimeInfoCommand;
import com.jannik_kuehn.loritime.common.command.LoriTimeTopCommand;
import com.jannik_kuehn.loritime.common.module.afk.MasteredAfkPlayerHandling;
import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;

import java.util.ArrayList;

public class LoriTimeBungee extends Plugin {

    private LoriTimePlugin loriTimePlugin;
    private BungeeAudiences audiences;
    private final ArrayList<BungeeCommand> commands = new ArrayList<>();

    @Override
    public void onEnable() {
        CommonLogger logger = new BungeeLogger(getProxy().getLogger());
        BungeeScheduleAdapter scheduleAdapter = new BungeeScheduleAdapter(this, getProxy().getScheduler());
        BungeeServer bungeeServer = new BungeeServer();
        this.loriTimePlugin = new LoriTimePlugin(logger, this.getDataFolder(), scheduleAdapter, bungeeServer);
        bungeeServer.enable(loriTimePlugin, getProxy());
        audiences = BungeeAudiences.create(this);
        try {
            loriTimePlugin.enable();
        } catch (Exception e) {
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
    }

    private void enableAsMaster() {
        PluginManager pluginManager = getProxy().getPluginManager();
        pluginManager.registerListener(this, new PlayerNameBungeeListener(loriTimePlugin));
        pluginManager.registerListener(this, new TimeAccumulatorBungeeListener(loriTimePlugin));


        commands.add(new BungeeCommand(this, audiences, new LoriTimeAdminCommand(loriTimePlugin, loriTimePlugin.getLocalization(),
                loriTimePlugin.getParser())));
        commands.add(new BungeeCommand(this, audiences, new LoriTimeCommand(loriTimePlugin, loriTimePlugin.getLocalization())));
        commands.add(new BungeeCommand(this, audiences, new LoriTimeInfoCommand(loriTimePlugin, loriTimePlugin.getLocalization())));
        commands.add(new BungeeCommand(this, audiences, new LoriTimeTopCommand(loriTimePlugin, loriTimePlugin.getLocalization())));
    }

    private void enableAsSlave() {
        getLogger().warning("Slave mode is not supported on Proxys! Disabling the plugin...");
        loriTimePlugin.disable();
    }

    private void enableRemainingFeatures() {
        if (loriTimePlugin.isAfkEnabled()) {
            getProxy().registerChannel("loritime:afk");
            getProxy().getPluginManager().registerListener(this, new BungeePluginMessanger(this));
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

    public LoriTimePlugin getLoriTimePlugin() {
        return loriTimePlugin;
    }
}
