package com.jannik_kuehn.common.module.updater.download;

import com.jannik_kuehn.common.exception.UpdateException;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Responsible for downloading files from a given URL.
 */
public class Downloader {

    /**
     * The folder where the downloaded files should be stored.
     */
    private final File targetFolder;

    /**
     * Indicates if a download is currently in progress.
     */
    private final AtomicBoolean isDownloading;

    /**
     * The file that should be downloaded.
     */
    private File targetFile;

    /**
     * Creates a new instance of the {@link Downloader}.
     *
     * @param targetFolder the folder where the downloaded files should be stored
     */
    public Downloader(final File targetFolder) {
        this.targetFolder = targetFolder;
        this.isDownloading = new AtomicBoolean(false);
    }

    /**
     * Downloads a file from the given URL and stores it in the target folder.
     *
     * @param downloadUrl the URL where the file should be downloaded from
     * @param targetFile  the file that should be downloaded
     */
    public void downloadFile(final URL downloadUrl, final File targetFile) {
        createFolderIfNotExists();

        try {
            final boolean runningDownload = isDownloading.compareAndSet(false, true);
            if (!runningDownload) {
                throw new UpdateException("An update is already in progress. Please wait until it is finished.");
            }
            final File tempFile = File.createTempFile(targetFile.getName() + "-", ".tmp");
            tempFile.deleteOnExit();
            FileUtils.copyURLToFile(downloadUrl, tempFile, 5000, 5000);

            try {
                final File detFile = new File(targetFolder.getPath() + "/" + targetFile.getName());
                this.targetFile = detFile;
                Files.move(tempFile.toPath(), detFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (final IOException e) {
                throw new UpdateException("Could not move downloaded file to target location: " + targetFile.getAbsolutePath(), e);
            }
        } catch (final IOException e) {
            throw new UpdateException("Could not download file from URL: " + downloadUrl, e);
        } finally {
            isDownloading.set(false);
        }
    }

    /**
     * Checks if the file is already downloaded.
     *
     * @return true if the file is already downloaded, otherwise false
     */
    public boolean fileAlreadyDownloaded() {
        return targetFile.exists();
    }

    private void createFolderIfNotExists() {
        if (!targetFolder.exists() && !targetFolder.mkdir()) {
            throw new UpdateException("Could not create folder for download on path: " + targetFolder.getAbsolutePath());
        }
    }
}
