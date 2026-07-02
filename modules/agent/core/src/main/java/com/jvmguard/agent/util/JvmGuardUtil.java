package com.jvmguard.agent.util;

import java.io.*;
import java.util.Collections;
import java.util.Set;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class JvmGuardUtil {
    @SuppressWarnings("DuplicatedCode")
    public static long pumpStream(InputStream is, OutputStream os, long maxLength) throws IOException {
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

    public static void copyFile(File sourceFile, File targetFile, boolean compress) throws IOException {
        InputStream fis = null;
        OutputStream fos = null;
        try {
            fis = new BufferedInputStream(new FileInputStream(sourceFile));
            FileOutputStream fileOutputStream = new FileOutputStream(targetFile);
            fos = new BufferedOutputStream(compress ? new GZIPOutputStream(new BufferedOutputStream(fileOutputStream)) : fileOutputStream);
            pumpStream(fis, fos, sourceFile.length());
        } finally {
            if (fis != null) {
                fis.close();
            }
            if (fos != null) {
                fos.close();
            }
        }
    }

    public static void writeCappedUTF(DataOutputStream out, String value) throws IOException {
        if (value.length() > 10000) {
            out.writeUTF(value.substring(0, 10000));
        } else {
            out.writeUTF(value);
        }
    }

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

    public static void unpack(File zipFile, File dir) throws IOException {
        dir = dir.getCanonicalFile();
        ZipInputStream in = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)));
        ZipEntry entry = in.getNextEntry();
        while (entry != null) {
            File file = new File(dir, entry.getName()).getCanonicalFile();
            if (file.getPath().startsWith(dir.getPath() + File.separator)) {
                if (entry.isDirectory()) {
                    file.mkdirs();
                } else {
                    OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
                    pumpStream(in, out, Long.MAX_VALUE);
                    out.close();
                }
            }
            entry = in.getNextEntry();
        }
        in.close();
    }

    public static String getCommandLineName(Enum<?> enumConstant, boolean capitalizeFirst) {
        String constantName = enumConstant.name();
        StringBuilder usedName = new StringBuilder(constantName.length());
        for (int i = 0; i < constantName.length(); i++) {
            if (constantName.charAt(i) == '_') {
                usedName.append(constantName.charAt(++i));
            } else if (capitalizeFirst && i == 0) {
                usedName.append(constantName.charAt(i));
            } else {
                usedName.append(Character.toLowerCase(constantName.charAt(i)));
            }
        }
        return usedName.toString();
    }

    public static <T extends Comparable<? super T>> int compareNullable(T o1, T o2) {
        if (o1 == null && o2 == null) {
            return 0;
        } else if (o1 != null && o2 != null) {
            return o1.compareTo(o2);
        } else {
            return o1 == null ? -1 : 1;
        }
    }
}
