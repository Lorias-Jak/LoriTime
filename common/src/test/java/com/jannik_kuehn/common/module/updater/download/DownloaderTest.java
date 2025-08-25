package com.jannik_kuehn.common.module.updater.download;

import com.jannik_kuehn.common.exception.UpdateException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Downloader}.
 */
class DownloaderTest {

    private File tempDir;

    @AfterEach
    void tearDown() throws IOException {
        if (tempDir != null && tempDir.exists()) {
            Files.walk(tempDir.toPath())
                .map(java.nio.file.Path::toFile)
                .sorted((a, b) -> -a.compareTo(b))
                .forEach(File::delete);
        }
    }

    @Test
    void testFileAlreadyDownloaded() {
        tempDir = new File(System.getProperty("java.io.tmpdir"), "downloader-test-" + System.nanoTime());
        final Downloader downloader = new Downloader(tempDir);
        assertDoesNotThrow(downloader::fileAlreadyDownloaded);
        assertFalse(downloader.fileAlreadyDownloaded(), "No file was downloaded, should report false");
    }

    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    void testCopyDownloadedFile() throws Exception {
        final File sourceFile = File.createTempFile("source-", ".txt");
        try (BufferedWriter writer = Files.newBufferedWriter(sourceFile.toPath())) {
            writer.write("hello-updater");
        }
        final URL sourceUrl = sourceFile.toURI().toURL();

        tempDir = new File(System.getProperty("java.io.tmpdir"), "downloader-test-" + System.nanoTime());
        final Downloader downloader = new Downloader(tempDir);
        final File target = new File("LoriTime.jar");

        downloader.downloadFile(sourceUrl, target);

        final File expectedDownloaded = new File(tempDir, target.getName());
        assertTrue(expectedDownloaded.exists(), "Downloaded file should exist in target folder");
        assertTrue(downloader.fileAlreadyDownloaded(), "Downloader should remember that a file was downloaded");

        final String content = Files.readString(expectedDownloaded.toPath());
        assertEquals("hello-updater", content, "Downloaded file should have the same content as the source file");

        assertTrue(sourceFile.delete(), "Source file should be deleted after copying it to the target folder");
    }

    @Test
    void testDownloadFileThrowsException() {
        tempDir = new File(System.getProperty("java.io.tmpdir"), "downloader-test-" + System.nanoTime());
        final Downloader downloader = new Downloader(tempDir);
        final File target = new File("LoriTime.jar");

        assertThrows(UpdateException.class, () ->
                downloader.downloadFile(new URL("file:///this/does/not/exist/nope.zip"), target),
                "Expected an UpdateException while downloading a non-existing file");
    }
}
