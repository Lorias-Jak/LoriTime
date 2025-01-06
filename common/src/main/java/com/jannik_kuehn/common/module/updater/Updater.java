package com.jannik_kuehn.common.module.updater;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.common.CommonSender;
import com.jannik_kuehn.common.api.logger.LoriTimeLogger;
import com.jannik_kuehn.common.exception.UpdateException;
import com.jannik_kuehn.common.module.updater.version.Strategy;
import com.jannik_kuehn.common.module.updater.version.Version;
import org.apache.commons.lang3.tuple.Pair;

public class Updater {

//    private static final TemporalAmount UPDATE_CHECK_DELAY = Duration.ofMinutes(30);
//
//    private static final TemporalAmount ANNOUNCE_PLAYER_DELAY = Duration.ofHours(60);

    private final LoriTimeLogger log;

    private final LoriTimePlugin loriTime;

    private final UpdateSourceHandler updateSourceHandler;

    private Pair<Version, String> latestVersion;

    public Updater(final LoriTimeLogger log, final UpdateSourceHandler updateSourceHandler, final LoriTimePlugin loriTime) {
        this.log = log;
        this.updateSourceHandler = updateSourceHandler;
        this.loriTime = loriTime;
    }

    public void search() {
        loriTime.getScheduler().runAsyncOnce(() -> {
            if (searchForUpdate()) {
                if (loriTime.getConfig().getBoolean("general.checkForUpdates", true)) {
                    // ToDo Automatisch Updaten!
                }
            }
        });
    }

    private boolean searchForUpdate() {
        // ToDo UpdateStrategy anpassen!
        // ToDo Indicator auslesen. ggf leer
        final Pair<Version, String> newLatest = updateSourceHandler.searchUpdate(Strategy.MAJOR, "DEV", latestVersion.getKey());
        if (newLatest.getValue() == null) {
            return false;
        }
        latest = newLatest;
        return true;
    }

    public boolean isUpdateAvailable() {
        return latestVersion.getValue() != null;
    }

    public String getUpdateVersion() {
        if (latestVersion.getValue() != null) {
            return latestVersion.getKey().toString();
        }
        return null;
    }

    public void sendUpdateNotification(final CommonSender sender) {
        // ToDo Ingame Notification?
        // ToDo Darf er UpdateNachricht erhalten?
    }

    public void update(CommonSender sender) {
        // ToDo Update check wird gestartet
        loriTime.getScheduler().runAsyncOnce(() -> {
            try {
                doesUpdateRequirementsMeet();

            } catch (UpdateException e) {
                //ToDo Exception
                throw new UpdateException(e);
            }
            // Checken ob updaten darf (Version da, Gibts nicht doch noch eine neuere?
            // ToDo Update herunterladen und in Ordner laden (Schauen welcher)
            // ToDo Update Notification das erfolgreich war
        });
    }

    private void doesUpdateRequirementsMeet() {
        // ToDo Trhows richtig schreiben!
        if (loriTime.getConfig().getBoolean("general.checkForUpdates", true)) {
            throw new UpdateException("Update check is disabled!");
        }
        // ToDo Checken ob updaten darf (Version da, Gibts nicht doch noch eine neuere?
    }

    private void executeUpdate() {
        // ToDo Datei runterladen als file und ablegen
        latestVersion = Pair.of(latestVersion.getKey(), null);
    }
}
