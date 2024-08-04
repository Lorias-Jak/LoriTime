package com.jannik_kuehn.loritimebukkit;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.LoriTimeAPI;
import com.jannik_kuehn.common.api.common.CommonLogger;
import com.jannik_kuehn.common.api.storage.TimeStorage;
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
import com.jannik_kuehn.loritimebukkit.messenger.BukkitPluginMessenger;
import com.jannik_kuehn.loritimebukkit.messenger.SlavedTimeStorageCache;
import com.jannik_kuehn.loritimebukkit.placeholder.LoriTimePlaceholder;
import com.jannik_kuehn.loritimebukkit.schedule.BukkitScheduleAdapter;
import com.jannik_kuehn.loritimebukkit.util.BukkitLogger;
import com.jannik_kuehn.loritimebukkit.util.BukkitMetrics;
import com.jannik_kuehn.loritimebukkit.util.BukkitServer;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

public class LoriTimeBukkit extends JavaPlugin {

    private LoriTimePlugin loriTimePlugin;

    private BukkitPluginMessenger bukkitPluginMessenger;

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

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null && loriTimePlugin.getConfig().getBoolean("integrations.PlaceholderAPI", true)) {
            new LoriTimePlaceholder(loriTimePlugin, loriTimePlugin.getTimeStorage()).register();
        }
    }

    private void enableAsSlave() {
        Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(this, "loritime:afk");
        bukkitPluginMessenger = new BukkitPluginMessenger(this);
        if (loriTimePlugin.isAfkEnabled()) {
            loriTimePlugin.enableAfkFeature(new BukkitSlavedAfkHandling(this));
        }
        final TimeStorage slavedTimeStorage = new SlavedTimeStorageCache(this, bukkitPluginMessenger,
                loriTimePlugin.getConfig().getInt("general.saveInterval"));
        Bukkit.getPluginManager().registerEvents((Listener) slavedTimeStorage, this);
        Bukkit.getServer().getMessenger().registerIncomingPluginChannel(this, "loritime:storage", (PluginMessageListener) slavedTimeStorage);
        Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(this, "loritime:storage");
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null && loriTimePlugin.getConfig().getBoolean("integrations.PlaceholderAPI", true)) {
            new LoriTimePlaceholder(loriTimePlugin, slavedTimeStorage).register();
        }
    }

    private void enableRemainingFeatures() {
        if (loriTimePlugin.isAfkEnabled()) {
            Bukkit.getPluginManager().registerEvents(new BukkitPlayerAfkListener(loriTimePlugin), this);
            new BukkitCommand(this, new LoriTimeAfkCommand(loriTimePlugin, loriTimePlugin.getLocalization()));
        }
        new BukkitMetrics(this, new Metrics(this, 22500));
    }

    @Override
    public void onDisable() {
        loriTimePlugin.disable();
    }

    public BukkitPluginMessenger getBukkitPluginMessenger() {
        return bukkitPluginMessenger;
    }

    public LoriTimePlugin getPlugin() {
        return loriTimePlugin;
    }
}
