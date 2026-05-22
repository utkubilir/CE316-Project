package com.iae.service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileManager {

    public List<File> discoverZipFiles(File folder) {

        List<File> zipFiles = new ArrayList<>();

        if (folder == null || !folder.exists()) {
            return zipFiles;
        }

        File[] files = folder.listFiles();

        if (files == null) {
            return zipFiles;
        }

        for (File file : files) {

            if (file.isFile()
                    && file.getName().toLowerCase().endsWith(".zip")) {

                zipFiles.add(file);
            }
        }

        return zipFiles;
    }
    public File extractZip(File zipFile,
                       File targetDirectory) throws java.io.IOException {

    String fileName = zipFile.getName();

    String studentId =
            fileName.substring(0, fileName.lastIndexOf("."));

    File studentFolder =
            new File(targetDirectory, studentId);

    if (!studentFolder.exists()) {
        studentFolder.mkdirs();
    }

    // Validate the zip file structure. ZipInputStream is lenient and might
    // just return null on getNextEntry() for corrupted zips, avoiding an Exception.
    // java.util.zip.ZipFile enforces the Central Directory structure and throws.
    try (java.util.zip.ZipFile zf = new java.util.zip.ZipFile(zipFile)) {
        // Just checking if it's a valid zip.
    }

    try (ZipInputStream zis =
                 new ZipInputStream(
                         new FileInputStream(zipFile))) {

        ZipEntry entry;

        while ((entry = zis.getNextEntry()) != null) {

            File outFile =
                    new File(studentFolder, entry.getName());

            if (entry.isDirectory()) {

                outFile.mkdirs();

            } else {

                File parent = outFile.getParentFile();

                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }

                FileOutputStream fos =
                        new FileOutputStream(outFile);

                byte[] buffer = new byte[1024];

                int len;

                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }

                fos.close();
            }

            zis.closeEntry();
        }

    } // removed catch block to let IOException propagate

    return studentFolder;
    }
    public File findSourceFile(File folder,
                           String sourceFileName) {

    if (folder == null || !folder.exists()) {
        return null;
    }

    File[] files = folder.listFiles();

    if (files == null) {
        return null;
    }

    for (File file : files) {

        if (file.isDirectory()) {

            File found =
                    findSourceFile(file, sourceFileName);

            if (found != null) {
                return found;
            }

        } else if (file.getName()
                .equalsIgnoreCase(sourceFileName)) {

            return file;
        }
    }

    return null;
    }
}