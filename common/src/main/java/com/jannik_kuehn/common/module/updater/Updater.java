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
import java.util.Map;
import java.util.UUID;

public class Updater {

    private static final TemporalAmount UPDATE_CHECK_DELAY = Duration.ofMinutes(30);

    private static final TemporalAmount ANNOUNCE_PLAYER_DELAY = Duration.ofHours(24);

    private final LoriTimeLogger log;

    private final LoriTimePlugin loriTime;

    private final UpdateSourceHandler updateSourceHandler;

    private final InstantSource instantSource;

    private final Downloader downloader;

    private Pair<Version, String> latestVersion;

    private Instant lastUpdateCheck;

    private Map<UUID, Instant> lastAnnounce;

    public Updater(final LoriTimeLogger log, Version currentVersion, final UpdateSourceHandler updateSourceHandler, final LoriTimePlugin loriTime,
                   InstantSource instantSource, Downloader downloader) {
        this.log = log;
        this.updateSourceHandler = updateSourceHandler;
        this.loriTime = loriTime;
        this.instantSource = instantSource;
        this.downloader = downloader;
        this.latestVersion = Pair.of(currentVersion, null);
    }

    public void search() {
        Configuration config = loriTime.getConfig();
        boolean updatesActive = config.getBoolean("updater.checkForUpdates", true);
        if (!updatesActive || !isTimeToCheckForUpdate()) {
            return;
        }

        loriTime.getScheduler().runAsyncOnce(() -> {
            if (!searchForUpdate()) {
                return;
            }

            log.info("An update has been found. Current Version: " + loriTime.getServer().getPluginVersion() + ", New Version: " + latestVersion.getKey());
            if (config.getBoolean("updater.autoUpdate", false)) {
                update(null);
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
        String configStrategy = loriTime.getConfig().getString("updater.updateStrategy", Strategy.MINOR.name());
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

    public boolean isUpdateAvailable() {
        return latestVersion.getValue() != null;
    }

    public Version getUpdateVersion() {
        if (latestVersion.getValue() != null) {
            return latestVersion.getKey();
        }
        return null;
    }

    public void sendPlayerUpdateNotification(final CommonSender sender) {
        if (loriTime.getConfig().getBoolean("updater.inGameNotification", true) && !sender.isConsole()) {
            Instant current = instantSource.instant();
            if (lastAnnounce.containsKey(sender.getUniqueId()) && lastAnnounce.get(sender.getUniqueId()).plus(ANNOUNCE_PLAYER_DELAY).isAfter(current)) {
                return;
            }
            lastAnnounce.put(sender.getUniqueId(), current);

            if (isUpdateAvailable()) {
                sender.sendMessage(loriTime.getLocalization().formatTextComponent(loriTime.getLocalization().getRawMessage("message.updater.available")));
            } else {
                sender.sendMessage(loriTime.getLocalization().formatTextComponent(loriTime.getLocalization().getRawMessage("message.updater.notFound")));
            }
        }
    }

    public void update(CommonSender sender) {
        sender.sendMessage(loriTime.getLocalization().formatTextComponent(loriTime.getLocalization().getRawMessage("message.updater.startUpdate")));
        loriTime.getScheduler().runAsyncOnce(() -> {
            try {
                doesUpdateRequirementsMeet();

                executeUpdate();
                sender.sendMessage(loriTime.getLocalization().formatTextComponent(loriTime.getLocalization().getRawMessage("message.updater.updateSuccess")));
            } catch (UpdateException e) {
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
                    .replace("[newVersion]", getUpdateVersion().toString()));
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
