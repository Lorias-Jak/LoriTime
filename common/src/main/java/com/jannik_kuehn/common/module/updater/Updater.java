package com.jannik_kuehn.common.module.updater;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.common.CommonSender;
import com.jannik_kuehn.common.api.logger.LoriTimeLogger;
import com.jannik_kuehn.common.config.Configuration;
import com.jannik_kuehn.common.exception.UpdateException;
import com.jannik_kuehn.common.module.updater.download.Downloader;
import com.jannik_kuehn.common.module.updater.version.Strategy;
import com.jannik_kuehn.common.module.updater.version.Version;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.time.temporal.TemporalAmount;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Responsible for checking for updates and downloading them if necessary.
 */
public class Updater {

    /**
     * The delay between two update checks.
     */
    private static final TemporalAmount UPDATE_CHECK_DELAY = Duration.ofMinutes(30);

    /**
     * The delay between two update announcements to a player.
     */
    private static final TemporalAmount ANNOUNCE_PLAYER_DELAY = Duration.ofHours(24);

    /**
     * The {@link LoriTimeLogger} that should be used for logging.
     */
    private final LoriTimeLogger log;

    /**
     * The {@link LoriTimePlugin} that should be updated.
     */
    private final LoriTimePlugin loriTime;

    /**
     * The {@link UpdateSourceHandler} that should be used for searching for updates.
     */
    private final UpdateSourceHandler updateSourceHandler;

    /**
     * The {@link InstantSource} that should be used for getting the current time.
     */
    private final InstantSource instantSource;

    /**
     * The {@link Downloader} that should be used for downloading the update.
     */
    private final Downloader downloader;

    /**
     * The last time an update was announced to a player.
     */
    private final Map<UUID, Instant> lastAnnounce;

    /**
     * The latest version that was found with a download link.
     */
    private Pair<Version, String> latestVersion;

    /**
     * The last time an update check was performed.
     */
    private Instant lastUpdateCheck;

    /**
     * Creates a new instance of the {@link Updater}.
     *
     * @param log                 the {@link LoriTimeLogger} that should be used for logging
     * @param currentVersion      the current {@link Version} of the plugin
     * @param updateSourceHandler the {@link UpdateSourceHandler} that should be used for searching for updates
     * @param loriTime            the {@link LoriTimePlugin} that should be updated
     * @param instantSource       the {@link InstantSource} that should be used for getting the current time
     * @param downloader          the {@link Downloader} that should be used for downloading the update
     */
    public Updater(final LoriTimeLogger log, final Version currentVersion, final UpdateSourceHandler updateSourceHandler, final LoriTimePlugin loriTime,
                   final InstantSource instantSource, final Downloader downloader) {
        this.log = log;
        this.updateSourceHandler = updateSourceHandler;
        this.loriTime = loriTime;
        this.instantSource = instantSource;
        this.downloader = downloader;
        this.latestVersion = Pair.of(currentVersion, null);
        this.lastAnnounce = new HashMap<>();
    }

    /**
     * Searches for an update and downloads it if it is available and the configuration allows it.
     */
    public void search() {
        final Configuration config = loriTime.getConfig();
        final boolean updatesActive = config.getBoolean("updater.checkForUpdates", true);
        if (!updatesActive || !isTimeToCheckForUpdate()) {
            return;
        }

        loriTime.getScheduler().runAsyncOnce(() -> {
            if (!searchForUpdate()) {
                return;
            }

            log.info("An update has been found. Current Version: " + loriTime.getServer().getPluginVersion() + ", New Version: " + latestVersion.getKey());
            if (config.getBoolean("updater.autoUpdate", false)) {
                update((CommonSender) loriTime.getServer());
            }
        });
    }

    private boolean isTimeToCheckForUpdate() {
        final Instant current = instantSource.instant();
        if (lastUpdateCheck != null && lastUpdateCheck.plus(UPDATE_CHECK_DELAY).isAfter(current)) {
            return false;
        }
        lastUpdateCheck = current;
        return true;
    }

    private boolean searchForUpdate() {
        final String configStrategy = loriTime.getConfig().getString("updater.updateStrategy", Strategy.MINOR.name());
        if (!Strategy.doesStrategyExists(configStrategy)) {
            log.error("The update strategy " + configStrategy + " is not valid.");
            return false;
        }

        final Pair<Version, String> newLatest = updateSourceHandler
                .searchUpdate(Strategy.getFromString(configStrategy), "", latestVersion.getKey());
        if (newLatest.getValue() == null) {
            return false;
        }

        latestVersion = newLatest;
        return true;
    }

    /**
     * Checks if an update is available.
     *
     * @return {@code true} if an update is available, otherwise {@code false}
     */
    public boolean isUpdateAvailable() {
        return latestVersion.getValue() != null;
    }

    /**
     * Returns the plugin version of the update if one was found.
     *
     * @return the {@link Version} of the update. If no update was found, it returns null.
     */
    public Version getUpdateVersion() {
        if (latestVersion.getValue() != null) {
            return latestVersion.getKey();
        }
        return null;
    }

    /**
     * Sends a notification to the player if an update is available.
     *
     * @param sender the {@link CommonSender} that should receive the update information
     */
    public void sendPlayerUpdateNotification(final CommonSender sender) {
        if (loriTime.getConfig().getBoolean("updater.inGameNotification", true) && !sender.isConsole()) {
            final Instant current = instantSource.instant();
            if (lastAnnounce.containsKey(sender.getUniqueId()) && lastAnnounce.get(sender.getUniqueId()).plus(ANNOUNCE_PLAYER_DELAY).isAfter(current)) {
                return;
            }
            lastAnnounce.put(sender.getUniqueId(), current);

            if (isUpdateAvailable()) {
                sender.sendMessage(loriTime.getLocalization().formatTextComponent(loriTime.getLocalization().getRawMessage("message.updater.available")));
            }
        }
    }

    /**
     * Checks if an update is available and downloads it if it is.
     *
     * @param sender the {@link CommonSender} that should receive the update information
     */
    public void update(final CommonSender sender) {
        sender.sendMessage(loriTime.getLocalization().formatTextComponent(loriTime.getLocalization().getRawMessage("message.updater.startUpdate")));
        loriTime.getScheduler().runAsyncOnce(() -> {
            try {
                doesUpdateRequirementsMeet();

                executeUpdate();
                sender.sendMessage(loriTime.getLocalization().formatTextComponent(
                        loriTime.getLocalization().getRawMessage("message.updater.updateSuccess")
                                .replace("[newVersion]", latestVersion.getKey().getVersionString())));
            } catch (final UpdateException e) {
                sender.sendMessage(loriTime.getLocalization().formatTextComponent(e.getMessage()));
                log.debug("An error occurred while updating the plugin", e);
            }
        });
    }

    private void doesUpdateRequirementsMeet() {
        if (!isUpdateCheckEnabled()) {
            throw new UpdateException(loriTime.getLocalization().getRawMessage("message.updater.disabled"));
        }
        if (searchForUpdate()) {
            throw new UpdateException(loriTime.getLocalization().getRawMessage("message.updater.newerVersionFound")
                    .replace("[newVersion]", getUpdateVersion().getVersionString()));
        }
        if (latestVersion.getValue() == null) {
            if (downloader.fileAlreadyDownloaded()) {
                throw new UpdateException(loriTime.getLocalization().getRawMessage("message.updater.alreadyDownloaded"));
            }
            throw new UpdateException(loriTime.getLocalization().getRawMessage("message.updater.notFound"));
        }
    }

    private void executeUpdate() {
        try {
            downloader.downloadFile(new URI(latestVersion.getValue()).toURL(), new File(loriTime.getServer().getPluginJarName()));
        } catch (MalformedURLException | URISyntaxException e) {
            throw new UpdateException("An error occurred while executing the update. Downloading the file failed", e);
        }
        latestVersion = Pair.of(latestVersion.getKey(), null);
    }

    private boolean isUpdateCheckEnabled() {
        return loriTime.getConfig().getBoolean("updater.checkForUpdates", true);
    }
}
