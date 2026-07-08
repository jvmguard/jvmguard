package com.jvmguard.agent.artifact;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

public final class TarExtractor {

    private static final int BLOCK = 512;

    private TarExtractor() {
    }

    public static void extractTarGz(File archive, File dest) throws IOException {
        try (InputStream in = new GZIPInputStream(new BufferedInputStream(new FileInputStream(archive)))) {
            extract(in, dest.getCanonicalFile());
        }
    }

    private static void extract(InputStream in, File dest) throws IOException {
        byte[] header = new byte[BLOCK];
        while (true) {
            if (!readFully(in, header)) {
                break; // EOF at block boundary
            }
            if (isAllZero(header)) {
                break; // end-of-archive marker
            }

            String name = cString(header, 0, 100);
            long mode = octal(header, 100, 8);
            long size = octal(header, 124, 12);
            char type = (char)(header[156] & 0xFF);

            File out = new File(dest, name).getCanonicalFile();
            boolean inside = out.getPath().equals(dest.getPath())
                || out.getPath().startsWith(dest.getPath() + File.separator); // zip-slip guard

            if (type == '5') { // directory
                if (inside) {
                    out.mkdirs();
                }
            } else if (type == '0' || type == '\0') { // regular file
                if (inside) {
                    File parent = out.getParentFile();
                    if (parent != null) {
                        parent.mkdirs();
                    }
                    try (OutputStream os = new BufferedOutputStream(new FileOutputStream(out))) {
                        copy(in, os, size);
                    }
                    if ((mode & 0111) != 0) {
                        out.setExecutable(true, false);
                    }
                } else {
                    skip(in, size);
                }
            } else { // link / long name / PAX / sparse, not used here
                skip(in, size);
            }
            skip(in, (BLOCK - (size % BLOCK)) % BLOCK); // padding to block boundary
        }
    }

    private static void copy(InputStream in, OutputStream out, long n) throws IOException {
        byte[] buf = new byte[8192];
        long left = n;
        while (left > 0) {
            int r = in.read(buf, 0, (int)Math.min(buf.length, left));
            if (r < 0) {
                break;
            }
            out.write(buf, 0, r);
            left -= r;
        }
    }

    private static void skip(InputStream in, long n) throws IOException {
        long left = n;
        while (left > 0) {
            long s = in.skip(left);
            if (s <= 0) {
                if (in.read() < 0) {
                    break;
                }
                left--;
            } else {
                left -= s;
            }
        }
    }

    private static boolean readFully(InputStream in, byte[] buf) throws IOException {
        int off = 0;
        while (off < buf.length) {
            int r = in.read(buf, off, buf.length - off);
            if (r < 0) {
                return off != 0; // false only on EOF at block start
            }
            off += r;
        }
        return true;
    }

    private static boolean isAllZero(byte[] b) {
        for (byte v : b) {
            if (v != 0) {
                return false;
            }
        }
        return true;
    }

    private static String cString(byte[] b, int off, int len) {
        int end = off;
        int max = off + len;
        while (end < max && b[end] != 0) {
            end++;
        }
        return new String(b, off, end - off, StandardCharsets.UTF_8);
    }

    private static long octal(byte[] b, int off, int len) {
        int i = off;
        int end = off + len;
        long v = 0;
        while (i < end && (b[i] == ' ' || b[i] == 0)) {
            i++;
        }
        while (i < end && b[i] >= '0' && b[i] <= '7') {
            v = v * 8 + (b[i] - '0');
            i++;
        }
        return v;
    }
}
