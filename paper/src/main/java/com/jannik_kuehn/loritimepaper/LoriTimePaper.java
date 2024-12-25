package com.jannik_kuehn.loritimepaper;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.LoriTimeAPI;
import com.jannik_kuehn.common.api.logger.LoriTimeLogger;
import com.jannik_kuehn.common.api.storage.TimeStorage;
import com.jannik_kuehn.common.command.LoriTimeAdminCommand;
import com.jannik_kuehn.common.command.LoriTimeAfkCommand;
import com.jannik_kuehn.common.command.LoriTimeCommand;
import com.jannik_kuehn.common.command.LoriTimeDebugCommand;
import com.jannik_kuehn.common.command.LoriTimeInfoCommand;
import com.jannik_kuehn.common.command.LoriTimeTopCommand;
import com.jannik_kuehn.common.module.afk.MasteredAfkPlayerHandling;
import com.jannik_kuehn.loritimepaper.afk.PaperSlavedAfkHandling;
import com.jannik_kuehn.loritimepaper.command.PaperCommand;
import com.jannik_kuehn.loritimepaper.listener.PaperPlayerAfkListener;
import com.jannik_kuehn.loritimepaper.listener.PlayerNamePaperListener;
import com.jannik_kuehn.loritimepaper.listener.TimeAccumulatorPaperListener;
import com.jannik_kuehn.loritimepaper.listener.UpdateNotificationPaperListener;
import com.jannik_kuehn.loritimepaper.messenger.PaperPluginMessenger;
import com.jannik_kuehn.loritimepaper.messenger.SlavedTimeStorageCache;
import com.jannik_kuehn.loritimepaper.placeholder.LoriTimePlaceholder;
import com.jannik_kuehn.loritimepaper.schedule.PaperScheduleAdapter;
import com.jannik_kuehn.loritimepaper.util.PaperMetrics;
import com.jannik_kuehn.loritimepaper.util.PaperServer;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

@SuppressWarnings({"PMD.CommentRequired", "PMD.AtLeastOneConstructor"})
public class LoriTimePaper extends JavaPlugin {

    private LoriTimePlugin loriTimePlugin;

    private PaperPluginMessenger paperPluginMessenger;

    @Override
    @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
    public void onEnable() {
        final PaperScheduleAdapter scheduleAdapter = new PaperScheduleAdapter(this,
                getServer().getAsyncScheduler(),
                getServer().getGlobalRegionScheduler());
        final PaperServer paperServer = new PaperServer(this, getDescription().getVersion());
        this.loriTimePlugin = new LoriTimePlugin(this.getDataFolder(), scheduleAdapter, paperServer, null);
        final LoriTimeLogger log = loriTimePlugin.getLoggerFactory().create(LoriTimePaper.class);

        loriTimePlugin.enable();
        LoriTimeAPI.setPlugin(loriTimePlugin);

        if ("master".equalsIgnoreCase(paperServer.getServerMode())) {
            enableAsMaster();
        } else if ("slave".equalsIgnoreCase(paperServer.getServerMode())) {
            enableAsSlave();
        } else {
            log.error("Server mode is not set correctly! Please set the server mode to 'master' or 'slave' in the config.yml. Disabling the plugin...");
            loriTimePlugin.disable();
        }
        enableRemainingFeatures();
    }

    private void enableAsMaster() {
        Bukkit.getPluginManager().registerEvents(new PlayerNamePaperListener(loriTimePlugin), this);
        Bukkit.getPluginManager().registerEvents(new TimeAccumulatorPaperListener(loriTimePlugin), this);
        Bukkit.getPluginManager().registerEvents(new UpdateNotificationPaperListener(loriTimePlugin), this);

        if (loriTimePlugin.isAfkEnabled()) {
            loriTimePlugin.enableAfkFeature(new MasteredAfkPlayerHandling(loriTimePlugin));
        }
        new PaperCommand(this, new LoriTimeAdminCommand(loriTimePlugin, loriTimePlugin.getLocalization(),
                loriTimePlugin.getParser()));
        new PaperCommand(this, new LoriTimeCommand(loriTimePlugin, loriTimePlugin.getLocalization()));
        new PaperCommand(this, new LoriTimeInfoCommand(loriTimePlugin, loriTimePlugin.getLocalization()));
        new PaperCommand(this, new LoriTimeTopCommand(loriTimePlugin, loriTimePlugin.getLocalization()));

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null && loriTimePlugin.getConfig().getBoolean("integrations.PlaceholderAPI", true)) {
            new LoriTimePlaceholder(loriTimePlugin, loriTimePlugin.getTimeStorage()).register();
        }
    }

    @SuppressWarnings("PMD.CloseResource")
    private void enableAsSlave() {
        Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(this, "loritime:afk");
        paperPluginMessenger = new PaperPluginMessenger(this);
        if (loriTimePlugin.isAfkEnabled()) {
            loriTimePlugin.enableAfkFeature(new PaperSlavedAfkHandling(this));
        }
        final TimeStorage slavedTimeStorage = new SlavedTimeStorageCache(this, paperPluginMessenger,
                loriTimePlugin.getConfig().getInt("general.saveInterval"));
        Bukkit.getPluginManager().registerEvents((Listener) slavedTimeStorage, this);
        Bukkit.getServer().getMessenger().registerIncomingPluginChannel(this, "loritime:storage", (PluginMessageListener) slavedTimeStorage);
        Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(this, "loritime:storage");
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null && loriTimePlugin.getConfig().getBoolean("integrations.PlaceholderAPI", true)) {
            new LoriTimePlaceholder(loriTimePlugin, slavedTimeStorage).register();
        }
    }

    @SuppressWarnings("PMD.UseUnderscoresInNumericLiterals")
    private void enableRemainingFeatures() {
        if (loriTimePlugin.isAfkEnabled()) {
            Bukkit.getPluginManager().registerEvents(new PaperPlayerAfkListener(loriTimePlugin), this);
            new PaperCommand(this, new LoriTimeAfkCommand(loriTimePlugin, loriTimePlugin.getLocalization()));
        }
        new PaperCommand(this, new LoriTimeDebugCommand(loriTimePlugin, loriTimePlugin.getLocalization()));
        new PaperMetrics(this, new Metrics(this, 22500));
    }

    @Override
    public void onDisable() {
        loriTimePlugin.disable();
    }

    public PaperPluginMessenger getBukkitPluginMessenger() {
        return paperPluginMessenger;
    }

    public LoriTimePlugin getPlugin() {
        return loriTimePlugin;
    }
}
