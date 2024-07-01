package com.jannik_kuehn.loritime.common.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.jannik_kuehn.loritime.api.CommonLogger;
import com.jannik_kuehn.loritime.api.PluginTask;
import com.jannik_kuehn.loritime.common.LoriTimePlugin;
import com.jannik_kuehn.loritime.common.config.localization.Localization;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

public class UpdateCheck {

    private static final String MODRINTH_URL = "https://api.modrinth.com/v2/project/loritime/version";

    private final LoriTimePlugin loriTime;

    private final CommonLogger log;

    private final Localization localization;

    private PluginTask repeatedUpdateCheck;

    private final String currentVersion;

    private String newVersion;

    public UpdateCheck(LoriTimePlugin loriTime) {
        this.loriTime = loriTime;
        this.log = loriTime.getLogger();
        this.localization = loriTime.getLocalization();
        this.currentVersion = loriTime.getPluginVersion();
    }

    public void startCheck() {
        repeatedUpdateCheck = loriTime.getScheduler().scheduleAsync(1L, 360 * 20L, this::checkForUpdate);
    }

    public void stopCheck() {
        if (repeatedUpdateCheck != null) {
            repeatedUpdateCheck.cancel();
            log.info("Stopping Update-Check Task!");
            return;
        }
        log.info("Update-Check Task was not running. Nothing to stop here.");
    }

    private void checkForUpdate() {
        log.info("Checking for updates...");
        JsonArray jsonArray = getLatestVersion();
        if (jsonArray == null || jsonArray.isEmpty()) {
            log.warning("Could not check for updates, got an empty result from Web-API!");
            return;
        }
        JsonObject latestVersionInfo = jsonArray.get(0).getAsJsonObject();
        newVersion = latestVersionInfo.get("version_number").getAsString();

        if (currentVersion.equalsIgnoreCase(newVersion)) {
            log.info("You are using the latest version of LoriTime!");
            return;
        } else {
            log.info("New version of LoriTime available: " + newVersion);
        }

        Arrays.stream(loriTime.getServer().getOnlinePlayers())
                .filter(player -> player.hasPermission("loritime.update"))
                .forEach(player -> player.sendMessage(localization.formatTextComponent(
                        localization.getRawMessage("message.update.available")
                                .replace("%currentVersion%", currentVersion)
                                .replace("%newVersion%", newVersion)
                                .replace("%DestinationLink%", "https://modrinth.com/plugin/loritime/changelog")
                )));
    }

    private JsonArray getLatestVersion() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new URI(MODRINTH_URL).toURL().openStream()))) {
            StringBuilder buffer = new StringBuilder();
            int read;
            char[] chars = new char[1024];
            while ((read = reader.read(chars)) != -1) {
                buffer.append(chars, 0, read);
            }
            return new Gson().fromJson(buffer.toString(), JsonArray.class);
        } catch (IOException | URISyntaxException e) {
            log.warning("Could not check for updates: " + e.getMessage());
            return null;
        }
    }
}
