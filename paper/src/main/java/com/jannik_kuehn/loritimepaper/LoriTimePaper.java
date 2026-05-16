package com.jannik_kuehn.loritimepaper;

import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.LoriTimeAPI;
import com.jannik_kuehn.common.api.storage.StorageMode;
import com.jannik_kuehn.common.command.LoriTimeAdminCommand;
import com.jannik_kuehn.common.command.LoriTimeAfkCommand;
import com.jannik_kuehn.common.command.LoriTimeCommand;
import com.jannik_kuehn.common.command.LoriTimeDebugCommand;
import com.jannik_kuehn.common.command.LoriTimeInfoCommand;
import com.jannik_kuehn.common.command.LoriTimeTopCommand;
import com.jannik_kuehn.common.module.afk.MasteredAfkPlayerHandling;
import com.jannik_kuehn.loritimepaper.afk.PaperSlavedAfkHandling;
import com.jannik_kuehn.loritimepaper.command.PaperCommand;
import com.jannik_kuehn.loritimepaper.listener.LoriTimeUpdatePaperListener;
import com.jannik_kuehn.loritimepaper.listener.PaperPlayerAfkListener;
import com.jannik_kuehn.loritimepaper.listener.PlayerNamePaperListener;
import com.jannik_kuehn.loritimepaper.listener.TimeAccumulatorPaperListener;
import com.jannik_kuehn.loritimepaper.messenger.PaperPluginMessenger;
import com.jannik_kuehn.loritimepaper.messenger.SlaveReadCache;
import com.jannik_kuehn.loritimepaper.messenger.SlaveSessionReporter;
import com.jannik_kuehn.loritimepaper.placeholder.LoriTimePlaceholder;
import com.jannik_kuehn.loritimepaper.schedule.PaperScheduleAdapter;
import com.jannik_kuehn.loritimepaper.util.PaperMetrics;
import com.jannik_kuehn.loritimepaper.util.PaperServer;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The {@code LoriTimePaper} class serves as the main plugin class for the LoriTime plugin
 * on Paper servers. It extends the {@code JavaPlugin} class to interface with Bukkit's
 * plugin lifecycle, including enabling, disabling, and registering commands and events.
 * <p>
 * This class is responsible for initializing and managing the various features, services,
 * and dependencies of the LoriTime plugin, depending on the configured server mode. It
 * supports three modes of operation: standalone, master, and slave.
 * <p>
 * Functionalities include:
 * - Registering server commands and event listeners.
 * - Initializing integration with third-party APIs like PlaceholderAPI.
 * - Handling different server modes and configurations.
 * - Providing communication channels for inter-server messaging in a networked environment.
 */
@SuppressWarnings({"PMD.AtLeastOneConstructor"})
public class LoriTimePaper extends JavaPlugin {

    /**
     * The {@link LoriTimePlugin} instance.
     */
    private LoriTimePlugin loriTimePlugin;

    /**
     * The {@link PaperPluginMessenger} instance.
     */
    private PaperPluginMessenger paperPluginMessenger;

    /**
     * The {@link SlaveSessionReporter} instance.
     */
    private SlaveSessionReporter slaveSessionReporter;

    @Override
    @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
    public void onEnable() {
        final PaperScheduleAdapter scheduleAdapter = new PaperScheduleAdapter(this,
                getServer().getAsyncScheduler(),
                getServer().getGlobalRegionScheduler());
        final PaperServer paperServer = new PaperServer(this, getDescription().getVersion());
        final LoggerFactory loggerFactory = new LoggerFactory(getSLF4JLogger());
        this.loriTimePlugin = new LoriTimePlugin(loggerFactory, this.getDataFolder(), scheduleAdapter, paperServer, null);
        final WrappedLogger log = loggerFactory.create(LoriTimePaper.class);

        loriTimePlugin.enable();
        LoriTimeAPI.setPlugin(loriTimePlugin);

        if (StorageMode.STANDALONE.configValue().equalsIgnoreCase(paperServer.getServerMode())
                || StorageMode.MASTER.configValue().equalsIgnoreCase(paperServer.getServerMode())) {
            enableAsCanonical();
        } else if ("slave".equalsIgnoreCase(paperServer.getServerMode())) {
            enableAsSlave();
        } else {
            log.error("Server mode is not set correctly! Please set the server mode to 'standalone', 'master' or 'slave' in the config.yml. Disabling the plugin...");
            loriTimePlugin.disable();
        }
        enableRemainingFeatures();
    }

    private void enableAsCanonical() {
        Bukkit.getPluginManager().registerEvents(new PlayerNamePaperListener(loriTimePlugin), this);
        Bukkit.getPluginManager().registerEvents(new TimeAccumulatorPaperListener(loriTimePlugin), this);

        if (loriTimePlugin.isAfkEnabled()) {
            loriTimePlugin.enableAfkFeature(new MasteredAfkPlayerHandling(loriTimePlugin));
        }
        new PaperCommand(this, new LoriTimeAdminCommand(loriTimePlugin, loriTimePlugin.getLocalization(),
                loriTimePlugin.getParser()));
        new PaperCommand(this, new LoriTimeCommand(loriTimePlugin, loriTimePlugin.getLocalization()));
        new PaperCommand(this, new LoriTimeInfoCommand(loriTimePlugin, loriTimePlugin.getLocalization()));
        new PaperCommand(this, new LoriTimeTopCommand(loriTimePlugin, loriTimePlugin.getLocalization()));

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null && loriTimePlugin.getConfig().getBoolean("integrations.PlaceholderAPI", true)) {
            new LoriTimePlaceholder(loriTimePlugin, loriTimePlugin.getStorage()).register();
        }
    }

    private void enableAsSlave() {
        Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(this, "loritime:afk");
        paperPluginMessenger = new PaperPluginMessenger(this);
        if (loriTimePlugin.isAfkEnabled()) {
            loriTimePlugin.enableAfkFeature(new PaperSlavedAfkHandling(this));
        }
        Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(this, "loritime:storage");
        final SlaveReadCache slaveReadCache = new SlaveReadCache(this, paperPluginMessenger);
        slaveSessionReporter = new SlaveSessionReporter(this, paperPluginMessenger,
                loriTimePlugin.getConfig().getInt("general.saveInterval"));
        Bukkit.getPluginManager().registerEvents(slaveReadCache, this);
        Bukkit.getPluginManager().registerEvents(slaveSessionReporter, this);
        Bukkit.getServer().getMessenger().registerIncomingPluginChannel(this, "loritime:storage", slaveReadCache);
        // TODO unify-storage-system: register PlaceholderAPI against the slave read cache.
        // TODO unify-storage-system: define deterministic placeholder cache-miss behavior while requesting master refreshes.
    }

    @SuppressWarnings("PMD.UseUnderscoresInNumericLiterals")
    private void enableRemainingFeatures() {
        Bukkit.getPluginManager().registerEvents(new LoriTimeUpdatePaperListener(loriTimePlugin), this);

        if (loriTimePlugin.isAfkEnabled()) {
            Bukkit.getPluginManager().registerEvents(new PaperPlayerAfkListener(loriTimePlugin), this);
            new PaperCommand(this, new LoriTimeAfkCommand(loriTimePlugin, loriTimePlugin.getLocalization()));
        }
        new PaperCommand(this, new LoriTimeDebugCommand(loriTimePlugin, loriTimePlugin.getLocalization()));
        new PaperMetrics(this, new Metrics(this, 22500));
    }

    @Override
    public void onDisable() {
        if (slaveSessionReporter != null) {
            slaveSessionReporter.close();
        }
        loriTimePlugin.disable();
    }

    /**
     * Gets the {@link PaperPluginMessenger} instance.
     *
     * @return the {@link PaperPluginMessenger} instance.
     */
    public PaperPluginMessenger getPaperPluginMessenger() {
        return paperPluginMessenger;
    }

    /**
     * Gets the {@link LoriTimePlugin} instance.
     *
     * @return the {@link LoriTimePlugin} instance.
     */
    public LoriTimePlugin getPlugin() {
        return loriTimePlugin;
    }
}
