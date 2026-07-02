package com.jvmguard.agent.bootstrap;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;

public class BootstrapFileUtil {
    public static void deleteDirectory(File dir) {
        Set<File> emptySet = Collections.emptySet();
        deleteDirectory(dir, emptySet);
    }

    @SuppressWarnings("DuplicatedCode")
    public static void emptyDirectory(File dir, Set<File> excludedFiles) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (!excludedFiles.contains(file.getAbsoluteFile())) {
                if (file.isDirectory()) {
                    deleteDirectory(file, excludedFiles);
                    file.delete();
                } else {
                    file.delete();
                }
            }
        }
    }

    public static void deleteDirectory(File dir, Set<File> excludedFiles) {
        emptyDirectory(dir, excludedFiles);
        dir.delete();
    }


    @SuppressWarnings("DuplicatedCode")
    private static long pumpStream(InputStream is, OutputStream os, long maxLength) throws IOException {
        byte[] buf = new byte[4096 * 10];

        long count = 0;
        int currentRead;
        while (count < maxLength && (currentRead = is.read(buf)) != -1) {
            int toBeWritten = (int)Math.min(currentRead, maxLength - count);
            os.write(buf, 0, toBeWritten);
            count += toBeWritten;
        }
        return count;
    }

    static void copyFile(File sourceFile, File targetFile) throws IOException {
        try (InputStream fis = new BufferedInputStream(new FileInputStream(sourceFile)); OutputStream fos = new BufferedOutputStream(new FileOutputStream(targetFile))) {
            pumpStream(fis, fos, sourceFile.length());
        }
    }

    public static void duplicateDirectory(File sourceDir, File targetBaseDir) throws IOException, InterruptedException {
        File targetDir = new File(targetBaseDir, sourceDir.getName());
        targetDir.mkdirs();
        if (!targetDir.isDirectory()) {
            throw new IOException("Cannot create " + targetDir.getPath());
        }
        copyDirectory(sourceDir, targetDir);
    }

    private static void copyDirectory(File sourceDir, File targetDir) throws IOException, InterruptedException {
        File[] files = sourceDir.listFiles();
        if (files != null) {
            for (File file : files) {
                File sourceFile = new File(sourceDir, file.getName()).getCanonicalFile();

                File targetFile = new File(targetDir, sourceFile.getName());
                if (sourceFile.isDirectory()) {
                    if (!targetFile.isDirectory() && !targetFile.mkdirs()) {
                        throw new IOException("Cannot create " + targetDir.getPath());
                    }
                    copyDirectory(sourceFile, targetFile);
                } else {
                    copyFile(sourceFile, targetFile);
                }
            }
        }
    }

    public static String getHashedPath(String path) {
        CRC64 crc64 = new CRC64();
        crc64.update(path.getBytes(StandardCharsets.UTF_8));
        return Long.toString(crc64.getValue(), Character.MAX_RADIX);
    }
}
