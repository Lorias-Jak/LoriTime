package com.jannik_kuehn.loritime.bukkit;

import com.jannik_kuehn.loritime.api.CommonLogger;
import com.jannik_kuehn.loritime.bukkit.afk.BukkitSlavedAfkHandling;
import com.jannik_kuehn.loritime.bukkit.command.BukkitCommand;
import com.jannik_kuehn.loritime.bukkit.listener.BukkitPlayerAfkListener;
import com.jannik_kuehn.loritime.bukkit.listener.PlayerNameBukkitListener;
import com.jannik_kuehn.loritime.bukkit.listener.TimeAccumulatorBukkitListener;
import com.jannik_kuehn.loritime.bukkit.schedule.BukkitScheduleAdapter;
import com.jannik_kuehn.loritime.bukkit.util.BukkitLogger;
import com.jannik_kuehn.loritime.bukkit.util.BukkitServer;
import com.jannik_kuehn.loritime.common.LoriTimePlugin;
import com.jannik_kuehn.loritime.common.command.LoriTimeAdminCommand;
import com.jannik_kuehn.loritime.common.command.LoriTimeAfkCommand;
import com.jannik_kuehn.loritime.common.command.LoriTimeCommand;
import com.jannik_kuehn.loritime.common.command.LoriTimeInfoCommand;
import com.jannik_kuehn.loritime.common.command.LoriTimeTopCommand;
import com.jannik_kuehn.loritime.common.module.afk.MasteredAfkPlayerHandling;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;

public class LoriTimeBukkit extends JavaPlugin {

    private LoriTimePlugin loriTimePlugin;
    private final ArrayList<BukkitCommand> commands = new ArrayList<>();
    private BukkitPluginMessanger pluginMessanger;

    @Override
    public void onEnable() {
        CommonLogger logger = new BukkitLogger(Bukkit.getLogger());
        BukkitScheduleAdapter scheduleAdapter = new BukkitScheduleAdapter(this, Bukkit.getScheduler());
        BukkitServer bukkitServer = new BukkitServer();
        this.loriTimePlugin = new LoriTimePlugin(logger, this.getDataFolder(), scheduleAdapter, bukkitServer);
        bukkitServer.enable(this);
        try {
            loriTimePlugin.enable();
        } catch (Exception e) {
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
    }

    private void enableAsMaster() {
        Bukkit.getPluginManager().registerEvents(new PlayerNameBukkitListener(loriTimePlugin), this);
        Bukkit.getPluginManager().registerEvents(new TimeAccumulatorBukkitListener(loriTimePlugin), this);

        if (loriTimePlugin.isAfkEnabled()) {
            loriTimePlugin.enableAfkFeature(new MasteredAfkPlayerHandling(loriTimePlugin));
        }
        commands.add(new BukkitCommand(this, new LoriTimeAdminCommand(loriTimePlugin, loriTimePlugin.getLocalization(),
                loriTimePlugin.getParser())));
        commands.add(new BukkitCommand(this, new LoriTimeCommand(loriTimePlugin, loriTimePlugin.getLocalization())));
        commands.add(new BukkitCommand(this, new LoriTimeInfoCommand(loriTimePlugin, loriTimePlugin.getLocalization())));
        commands.add(new BukkitCommand(this, new LoriTimeTopCommand(loriTimePlugin, loriTimePlugin.getLocalization())));
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
            commands.add(new BukkitCommand(this, new LoriTimeAfkCommand(loriTimePlugin, loriTimePlugin.getLocalization())));
        }
    }


    @Override
    public void onDisable() {
        loriTimePlugin.disable();
    }

    public LoriTimePlugin getLoriTimePlugin() {
        return loriTimePlugin;
    }

    public BukkitPluginMessanger getPluginMessanger() {
        return pluginMessanger;
    }
}
