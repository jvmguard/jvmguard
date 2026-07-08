package com.jvmguard.agent.artifact;

import com.jvmguard.agent.util.JvmGuardUtil;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ArchiveExtractor {

    public static void extract(File archive, File targetDir) throws IOException {
        byte[] magic = readMagic(archive);
        if (isZip(magic)) {
            extractZip(archive, targetDir);
        } else if (isGzip(magic)) {
            TarExtractor.extractTarGz(archive, targetDir);
        } else {
            throw new IOException("Unrecognized archive format: " + archive);
        }
    }

    private static void extractZip(File archive, File targetDir) throws IOException {
        targetDir = targetDir.getCanonicalFile();
        ZipInputStream in = new ZipInputStream(new BufferedInputStream(new FileInputStream(archive)));
        ZipEntry entry = in.getNextEntry();
        while (entry != null) {
            File file = new File(targetDir, entry.getName()).getCanonicalFile();
            if (file.getPath().startsWith(targetDir.getPath() + File.separator)) {
                if (entry.isDirectory()) {
                    file.mkdirs();
                } else {
                    OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
                    JvmGuardUtil.pumpStream(in, out, Long.MAX_VALUE);
                    out.close();
                }
            }
            entry = in.getNextEntry();
        }
        in.close();
    }

    private static byte[] readMagic(File file) throws IOException {
        byte[] buffer = new byte[4];
        try (InputStream in = new FileInputStream(file)) {
            int read = 0;
            while (read < buffer.length) {
                int n = in.read(buffer, read, buffer.length - read);
                if (n < 0) {
                    break;
                }
                read += n;
            }
        }
        return buffer;
    }

    private static boolean isZip(byte[] m) {
        return (m[0] & 0xff) == 0x50 && (m[1] & 0xff) == 0x4B && (m[2] & 0xff) == 0x03 && (m[3] & 0xff) == 0x04;
    }

    private static boolean isGzip(byte[] m) {
        return (m[0] & 0xff) == 0x1f && (m[1] & 0xff) == 0x8b;
    }
}
