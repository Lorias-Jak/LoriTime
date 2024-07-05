package com.jannik_kuehn.loritimebukkit;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.LoriTimeAPI;
import com.jannik_kuehn.common.api.common.CommonLogger;
import com.jannik_kuehn.common.command.LoriTimeAdminCommand;
import com.jannik_kuehn.common.command.LoriTimeAfkCommand;
import com.jannik_kuehn.common.command.LoriTimeCommand;
import com.jannik_kuehn.common.command.LoriTimeInfoCommand;
import com.jannik_kuehn.common.command.LoriTimeTopCommand;
import com.jannik_kuehn.common.module.afk.MasteredAfkPlayerHandling;
import com.jannik_kuehn.loritimebukkit.afk.BukkitSlavedAfkHandling;
import com.jannik_kuehn.loritimebukkit.command.BukkitCommand;
import com.jannik_kuehn.loritimebukkit.listener.BukkitPlayerAfkListener;
import com.jannik_kuehn.loritimebukkit.listener.PlayerNameBukkitListener;
import com.jannik_kuehn.loritimebukkit.listener.TimeAccumulatorBukkitListener;
import com.jannik_kuehn.loritimebukkit.listener.UpdateNotificationBukkitListener;
import com.jannik_kuehn.loritimebukkit.schedule.BukkitScheduleAdapter;
import com.jannik_kuehn.loritimebukkit.util.BukkitLogger;
import com.jannik_kuehn.loritimebukkit.util.BukkitServer;
import com.jannik_kuehn.loritimebukkit.util.LoriTimePlaceholder;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class LoriTimeBukkit extends JavaPlugin {

    private LoriTimePlugin loriTimePlugin;

    private BukkitPluginMessanger pluginMessanger;

    @Override
    public void onEnable() {
        final CommonLogger logger = new BukkitLogger(getLogger());
        final BukkitScheduleAdapter scheduleAdapter = new BukkitScheduleAdapter(this, Bukkit.getScheduler());
        final BukkitServer bukkitServer = new BukkitServer(getDescription().getVersion());
        this.loriTimePlugin = new LoriTimePlugin(logger, this.getDataFolder(), scheduleAdapter, bukkitServer);
        bukkitServer.enable(this);
        try {
            loriTimePlugin.enable();
            LoriTimeAPI.setPlugin(loriTimePlugin);
        } catch (final Exception e) {
            loriTimePlugin.disable();
            logger.warning("Error while enabling the plugin! Disabling the plugin...", e);
            return;
        }

        if (bukkitServer.getServerMode().equalsIgnoreCase("master")) {
            enableAsMaster();
        } else if (bukkitServer.getServerMode().equalsIgnoreCase("slave")) {
            enableAsSlave();
        } else {
            logger.severe("Server mode is not set correctly! Please set the server mode to 'master' or 'slave' in the config.yml. Disabling the plugin...");
            loriTimePlugin.disable();
        }
        enableRemainingFeatures();
        new Metrics(this, 22500);
    }

    private void enableAsMaster() {
        Bukkit.getPluginManager().registerEvents(new PlayerNameBukkitListener(loriTimePlugin), this);
        Bukkit.getPluginManager().registerEvents(new TimeAccumulatorBukkitListener(loriTimePlugin), this);
        Bukkit.getPluginManager().registerEvents(new UpdateNotificationBukkitListener(loriTimePlugin), this);

        if (loriTimePlugin.isAfkEnabled()) {
            loriTimePlugin.enableAfkFeature(new MasteredAfkPlayerHandling(loriTimePlugin));
        }
        new BukkitCommand(this, new LoriTimeAdminCommand(loriTimePlugin, loriTimePlugin.getLocalization(),
                loriTimePlugin.getParser()));
        new BukkitCommand(this, new LoriTimeCommand(loriTimePlugin, loriTimePlugin.getLocalization()));
        new BukkitCommand(this, new LoriTimeInfoCommand(loriTimePlugin, loriTimePlugin.getLocalization()));
        new BukkitCommand(this, new LoriTimeTopCommand(loriTimePlugin, loriTimePlugin.getLocalization()));
    }

    private void enableAsSlave() {
        Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(this, "loritime:afk");
        pluginMessanger = new BukkitPluginMessanger(this);
        if (loriTimePlugin.isAfkEnabled()) {
            loriTimePlugin.enableAfkFeature(new BukkitSlavedAfkHandling(this));
        }
    }

    private void enableRemainingFeatures() {
        if (loriTimePlugin.isAfkEnabled()) {
            Bukkit.getPluginManager().registerEvents(new BukkitPlayerAfkListener(loriTimePlugin), this);
            new BukkitCommand(this, new LoriTimeAfkCommand(loriTimePlugin, loriTimePlugin.getLocalization()));
        }
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null && loriTimePlugin.getConfig().getBoolean("integrations.PlaceholderAPI", true)) {
            new LoriTimePlaceholder(loriTimePlugin).register();
        }
    }

    @Override
    public void onDisable() {
        loriTimePlugin.disable();
    }

    public BukkitPluginMessanger getPluginMessanger() {
        return pluginMessanger;
    }

    public LoriTimePlugin getPlugin() {
        return loriTimePlugin;
    }
}
