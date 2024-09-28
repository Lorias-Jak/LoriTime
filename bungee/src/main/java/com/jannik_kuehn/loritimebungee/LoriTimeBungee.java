package com.jannik_kuehn.loritimebungee;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.LoriTimeAPI;
import com.jannik_kuehn.common.api.logger.LoriTimeLogger;
import com.jannik_kuehn.common.command.LoriTimeAdminCommand;
import com.jannik_kuehn.common.command.LoriTimeCommand;
import com.jannik_kuehn.common.command.LoriTimeDebugCommand;
import com.jannik_kuehn.common.command.LoriTimeInfoCommand;
import com.jannik_kuehn.common.command.LoriTimeTopCommand;
import com.jannik_kuehn.common.module.afk.MasteredAfkPlayerHandling;
import com.jannik_kuehn.loritimebungee.command.BungeeCommand;
import com.jannik_kuehn.loritimebungee.listener.PlayerNameBungeeListener;
import com.jannik_kuehn.loritimebungee.listener.TimeAccumulatorBungeeListener;
import com.jannik_kuehn.loritimebungee.listener.UpdateNotificationBungeeListener;
import com.jannik_kuehn.loritimebungee.schedule.BungeeScheduleAdapter;
import com.jannik_kuehn.loritimebungee.util.BungeeMetrics;
import com.jannik_kuehn.loritimebungee.util.BungeeServer;
import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import org.bstats.bungeecord.Metrics;

@SuppressWarnings({"PMD.CommentRequired", "PMD.AvoidCatchingGenericException", "PMD.AtLeastOneConstructor"})
public class LoriTimeBungee extends Plugin {

    private LoriTimePlugin loriTimePlugin;

    private BungeeAudiences audiences;

    @Override
    @SuppressWarnings({"PMD.UseUnderscoresInNumericLiterals", "PMD.AvoidLiteralsInIfCondition"})
    public void onEnable() {
        audiences = BungeeAudiences.create(this);
        final BungeeScheduleAdapter scheduleAdapter = new BungeeScheduleAdapter(this, getProxy().getScheduler());
        final BungeeServer bungeeServer = new BungeeServer(getLogger(), getProxy(), getDescription().getVersion(), audiences);
        final String loggerTopic = "LoriTimeBungee";
        this.loriTimePlugin = new LoriTimePlugin(this.getDataFolder(), scheduleAdapter, bungeeServer, loggerTopic);

        final LoriTimeLogger log = loriTimePlugin.getLoggerFactory().create(LoriTimeBungee.class, loggerTopic);

        loriTimePlugin.enable();
        LoriTimeAPI.setPlugin(loriTimePlugin);

        if ("master".equalsIgnoreCase(bungeeServer.getServerMode())) {
            enableAsMaster();
        } else if ("slave".equalsIgnoreCase(bungeeServer.getServerMode())) {
            enableAsSlave();
        } else {
            log.error("Server mode is not set correctly! Please set the server mode to 'master' or 'slave' in the config.yml. Disabling the plugin...");
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
        new BungeeCommand(this, audiences, new LoriTimeDebugCommand(loriTimePlugin, loriTimePlugin.getLocalization()));
    }

    private void enableAsSlave() {
        getLogger().warning("Slave mode is not supported on Proxys! Disabling the plugin...");
        loriTimePlugin.disable();
    }

    private void enableRemainingFeatures() {
        if (loriTimePlugin.isAfkEnabled()) {
            getProxy().registerChannel("loritime:afk");
            getProxy().registerChannel("loritime:storage");
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
