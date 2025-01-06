package com.jannik_kuehn.common.module.updater.download;

import com.jannik_kuehn.common.exception.UpdateException;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicBoolean;

public class Downloader {

    private final File targetFolder;

    private final AtomicBoolean isDownloading;

    private File targetFile;

    public Downloader(final File targetFolder) {
        this.targetFolder = targetFolder;
        this.isDownloading = new AtomicBoolean(false);
    }

    public void downloadFile(final URL downloadUrl, final File targetFile) {
        createFolderIfNotExists();
        this.targetFile = targetFile;

        try {
            final boolean runningDownload = isDownloading.compareAndSet(false, true);
            if (!runningDownload) {
                throw new UpdateException("An update is already in progress. Please wait until it is finished.");
            }
            final File tempFile = File.createTempFile(targetFile.getName() + "-", ".tmp");
            tempFile.deleteOnExit();
            FileUtils.copyURLToFile(downloadUrl, tempFile, 5000, 5000);

            try {
                File detFile = new File(targetFolder.getPath() + "/" + targetFile.getName());
                Files.move(tempFile.toPath(), detFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new UpdateException("Could not move downloaded file to target location: " + targetFile.getAbsolutePath(), e);
            }
        } catch (IOException e) {
            throw new UpdateException("Could not download file from URL: " + downloadUrl, e);
        } finally {
            isDownloading.set(false);
        }
    }

    public boolean fileAlreadyDownloaded() {
        return targetFile.exists();
    }

    private void createFolderIfNotExists() {
        if (!targetFolder.exists() && !targetFolder.mkdir()) {
            throw new UpdateException("Could not create folder for download on path: " + targetFolder.getAbsolutePath());
        }
    }
}
