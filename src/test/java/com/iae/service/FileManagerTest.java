package com.iae.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileManagerTest {

    private final FileManager fileManager = new FileManager();

    @Test
    void discoverZipFilesReturnsEmptyListWhenFolderMissing() {
        List<File> zips = fileManager.discoverZipFiles(new File("/does/not/exist"));
        assertTrue(zips.isEmpty());
    }

    @Test
    void discoverZipFilesFiltersByExtension(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("a.zip"), "");
        Files.writeString(tempDir.resolve("B.ZIP"), "");
        Files.writeString(tempDir.resolve("readme.txt"), "");
        Files.createDirectory(tempDir.resolve("nested"));

        List<File> zips = fileManager.discoverZipFiles(tempDir.toFile());
        assertEquals(2, zips.size());
        zips.forEach(f -> assertTrue(f.getName().toLowerCase().endsWith(".zip")));
    }

    @Test
    void extractZipPreservesNestedDirectoryStructure(@TempDir Path tempDir) throws IOException {
        File zip = tempDir.resolve("student_42.zip").toFile();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip))) {
            zos.putNextEntry(new ZipEntry("src/Main.java"));
            zos.write("class Main {}".getBytes());
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry("README.txt"));
            zos.write("notes".getBytes());
            zos.closeEntry();
        }

        File target = tempDir.resolve("work").toFile();
        target.mkdirs();

        File studentFolder = fileManager.extractZip(zip, target);

        assertEquals("student_42", studentFolder.getName());
        assertTrue(new File(studentFolder, "src/Main.java").isFile());
        assertTrue(new File(studentFolder, "README.txt").isFile());
    }

    @Test
    void extractZipThrowsForCorruptedZip(@TempDir Path tempDir) throws IOException {
        File corrupted = tempDir.resolve("broken.zip").toFile();
        Files.writeString(corrupted.toPath(), "this is not a zip");
        File target = tempDir.resolve("work").toFile();
        target.mkdirs();

        assertThrows(IOException.class, () -> fileManager.extractZip(corrupted, target));
    }

    @Test
    void findSourceFileIsCaseInsensitiveAndRecursive(@TempDir Path tempDir) throws IOException {
        Path nested = tempDir.resolve("submission/src");
        Files.createDirectories(nested);
        Files.writeString(nested.resolve("Main.java"), "class Main {}");

        File found = fileManager.findSourceFile(tempDir.toFile(), "main.java");
        assertNotNull(found);
        assertEquals("Main.java", found.getName());
    }

    @Test
    void findSourceFileReturnsNullWhenMissing(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("notes.txt"), "");
        assertNull(fileManager.findSourceFile(tempDir.toFile(), "Main.java"));
    }
}
