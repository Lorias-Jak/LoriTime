package com.jannik_kuehn.common.module.updater;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.common.CommonLogger;
import com.jannik_kuehn.common.api.common.CommonSender;
import com.jannik_kuehn.common.api.scheduler.PluginTask;
import com.jannik_kuehn.common.config.localization.Localization;
import net.kyori.adventure.text.TextComponent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

/**
 * This class is responsible for checking for updates of the plugin.
 */
public class UpdateCheck {
    /**
     * The URL to the Modrinth API to get the latest version of the plugin.
     */
    private static final String MODRINTH_URL = "https://api.modrinth.com/v2/project/loritime/version";

    /**
     * The main plugin class created from the modules.
     */
    private final LoriTimePlugin loriTime;

    /**
     * The logger to log messages.
     */
    private final CommonLogger log;

    /**
     * The localization class to get messages.
     */
    private final Localization localization;

    /**
     * The current version of the plugin.
     */
    private final String currentVersion;

    /**
     * The task to check for updates.
     */
    private PluginTask repeatedUpdateCheck;

    /**
     * The new version of the plugin.
     */
    private String newVersion;

    /**
     * Is the newest Version a release version.
     */
    private boolean isRelease;

    /**
     * A boolean to check if the update check is enabled.
     */
    private boolean isUpdateCheckEnabled;

    /**
     * Constructor for the UpdateCheck class.
     *
     * @param loriTime The main plugin class created from the modules.
     */
    public UpdateCheck(final LoriTimePlugin loriTime) {
        this.loriTime = loriTime;
        this.log = loriTime.getLogger();
        this.localization = loriTime.getLocalization();
        this.currentVersion = loriTime.getServer().getPluginVersion();
    }

    /**
     * Starts the update check task.
     */
    public void startCheck() {
        if (!loriTime.getConfig().getBoolean("general.checkForUpdates", true)) {
            isUpdateCheckEnabled = false;
            return;
        }
        isUpdateCheckEnabled = true;
        repeatedUpdateCheck = loriTime.getScheduler().scheduleAsync(1L, 360 * 20L, this::checkForUpdate);
    }

    /**
     * Stops the update check task.
     */
    public void stopCheck() {
        if (repeatedUpdateCheck != null) {
            repeatedUpdateCheck.cancel();
            log.info("Stopping Update-Check Task!");
        }
    }

    /**
     * Sends an update notification if an update is available, the sender has the permission,
     * and the update check is enabled.
     *
     * @param sender The {@link CommonSender} to send the update notification.
     */
    public void sendUpdateNotification(final CommonSender sender) {
        if (hasUpdate(newVersion) && hasPermission(sender) && isUpdateCheckEnabled) {
            sender.sendMessage(getMessage());
        }
    }

    private void checkForUpdate() {
        log.info("Checking for updates...");
        final JsonArray jsonArray = getLatestVersion();
        if (jsonArray == null || jsonArray.isEmpty()) {
            log.warning("Could not check for updates, got an empty result from Web-API!");
            return;
        }
        final JsonObject latestVersionInfo = jsonArray.get(0).getAsJsonObject();
        newVersion = latestVersionInfo.get("version_number").getAsString();
        isRelease = latestVersionInfo.get("version_type").getAsString().equalsIgnoreCase("release");

        if (!hasUpdate(newVersion)) {
            log.info("You are using the latest version of LoriTime!");
            return;
        }

        sendUpdateNotificationToConsole(localization.getRawMessage("message.update.available")
                .replace("[currentVersion]", currentVersion)
                .replace("[newVersion]", newVersion)
                .replace("[url]", "https://modrinth.com/plugin/loritime/changelog"));
    }

    private JsonArray getLatestVersion() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new URI(MODRINTH_URL).toURL().openStream(), StandardCharsets.UTF_8))) {
            final StringBuilder buffer = new StringBuilder();
            int read;
            final char[] chars = new char[1024];
            while ((read = reader.read(chars)) != -1) {
                buffer.append(chars, 0, read);
            }
            return new Gson().fromJson(buffer.toString(), JsonArray.class);
        } catch (IOException | URISyntaxException e) {
            log.warning("Could not check for updates: " + e.getMessage());
            return null;
        }
    }

    private void sendUpdateNotificationToConsole(final String message) {
        loriTime.getServer().sendMessageToConsole(localization.formatTextComponent(message));
    }

    private TextComponent getMessage() {
        return localization.formatTextComponent(
                localization.getRawMessage("message.update.available")
                        .replace("[currentVersion]", currentVersion)
                        .replace("[newVersion]", newVersion)
                        .replace("[url]", "https://modrinth.com/plugin/loritime/changelog")
        );
    }

    private boolean hasUpdate(final String newVersion) {
        return isRelease && VersionUtil.isNewerVersion(currentVersion, newVersion);
    }

    private boolean hasPermission(final CommonSender sender) {
        return sender.hasPermission("loritime.update");
    }
}
