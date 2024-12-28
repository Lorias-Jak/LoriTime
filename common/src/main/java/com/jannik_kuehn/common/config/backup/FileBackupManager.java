package com.jannik_kuehn.common.config.backup;

import com.jannik_kuehn.common.api.logger.LoriTimeLogger;
import com.jannik_kuehn.common.exception.ConfigurationException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * The {@link FileBackupManager} is responsible for creating backups of files.
 */
public class FileBackupManager {
    /**
     * {@code true} if backups are enabled, otherwise {@code false}.
     */
    private final boolean backupsEnabled;

    /**
     * The {@link LoriTimeLogger} instance.
     */
    private final LoriTimeLogger log;

    /**
     * The maximum number of backups.
     */
    private final int maxBackups;

    /**
     * The backup directory.
     */
    private final File backupDirectory;

    /**
     * The list of files to backup.
     */
    private final List<File> backupFiles;

    /**
     * Creates a new {@link FileBackupManager} instance.
     *
     * @param log             the {@link LoriTimeLogger} instance.
     * @param pluginDirectory the plugin directory.
     * @param backupsEnabled  {@code true} if backups are enabled, otherwise {@code false}.
     * @param maxBackups      the maximum number of backups. If set to 0 or lower, no backups will be deleted.
     */
    public FileBackupManager(final LoriTimeLogger log, final File pluginDirectory, final boolean backupsEnabled, final int maxBackups) {
        this.log = log;
        this.backupsEnabled = backupsEnabled;
        this.maxBackups = maxBackups;
        this.backupDirectory = new File(pluginDirectory, "backup");
        this.backupFiles = new ArrayList<>();

        if (backupsEnabled && backupDirectory.mkdirs()) {
            log.info("Created Backup Directory");
        }
    }

    /**
     * Adds a file to the backup list.
     *
     * @param fileToBackup the file to backup.
     * @throws ConfigurationException if an error occurs while copying the file to the backup directory.
     */
    public void addFileToBackup(final File fileToBackup) throws ConfigurationException {
        if (!fileToBackup.exists() || !backupsEnabled) {
            return;
        }
        final File backupFile = new File(backupDirectory, fileToBackup.getName());
        try {
            Files.copy(fileToBackup.toPath(), new File(backupDirectory, fileToBackup.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (final IOException e) {
            throw new ConfigurationException("Fehler beim Kopieren der Datei zum Backup-Verzeichnis.", e);
        }
        backupFiles.add(backupFile);
    }

    /**
     * Starts the backup process.
     */
    public void startBackup() {
        if (backupFiles.isEmpty() || !backupsEnabled) {
            log.warn("Keine Dateien zum Backup hinzugefügt.");
            return;
        }

        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss");
        final String currentDate = LocalDateTime.now().format(formatter);
        final File backupFile = new File(backupDirectory, currentDate + " Backup" + ".zip");

        try {
            zipFiles(backupFile);
            log.info("Backup erfolgreich erstellt: " + backupFile.getAbsolutePath());
        } catch (final IOException e) {
            log.error("Fehler beim Erstellen des Backups.", e);
            return;
        }
        backupFiles.forEach(File::delete);

        manageBackupRetention();
    }

    private void zipFiles(final File outputZipFile) throws IOException {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(outputZipFile.toPath()))) {
            for (final File file : backupFiles) {
                addToZip(file, zipOutputStream, "");
            }
        }
    }

    private void addToZip(final File file, final ZipOutputStream zipOutputStream, final String parentPath) throws IOException {
        final String zipEntryName = parentPath + file.getName();
        if (file.isDirectory()) {
            zipOutputStream.putNextEntry(new ZipEntry(zipEntryName + "/"));
            zipOutputStream.closeEntry();

            final File[] children = file.listFiles();
            if (children != null) {
                for (final File child : children) {
                    addToZip(child, zipOutputStream, zipEntryName + "/");
                }
            }
        } else {
            zipOutputStream.putNextEntry(new ZipEntry(zipEntryName));
            Files.copy(file.toPath(), zipOutputStream);
            zipOutputStream.closeEntry();
        }
    }

    /**
     * Manages the backup retention and deletes the oldest files if the maximum number of backups is exceeded.
     */
    public void manageBackupRetention() {
        if (!backupsEnabled || maxBackups <= 0) {
            return;
        }
        final File[] backupFiles = backupDirectory.listFiles((dir, name) -> name.endsWith("Backup.zip"));
        if (backupFiles == null || backupFiles.length <= maxBackups) {
            return;
        }

        Arrays.sort(backupFiles, Comparator.comparingLong(File::lastModified));

        for (int i = 0; i < backupFiles.length - maxBackups; i++) {
            if (backupFiles[i].delete()) {
                log.info("Backup-Datei gelöscht: " + backupFiles[i].getName());
            } else {
                log.error("Fehler beim Löschen der Backup-Datei: " + backupFiles[i].getName());
            }
        }
    }
}
