package dev.jvmguard.integration;

import dev.jvmguard.integration.util.SleepHelper;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class Util {
    public static final String RUNNO_PROP_NAME = "jvmguard.runNo";
    public static final String ATTACHABLE_PROP_NAME = "jvmguard.attachable";
    public static final String LIBRARYNO_PROP_NAME = "jvmguard.libraryNo";
    public static final String VMNO_PROP_NAME = "jvmguard.vmNo";
    public static final String VM_PROP_NAME = "jvmguard.vmName";
    public static final String DEFAULT_RUNCLASS_PROP_NAME = "jvmguard.defaultRunClass";
    public static final String TEST_CLASS_PROP_NAME = "jvmguard.testClass";
    public static final String RELOADING_PROP_NAME = "jvmguard.reloading";

    private static Boolean wsl2;

    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().startsWith("win");
    }

    public static boolean isMac() {
        return System.getProperty("os.name").toLowerCase().startsWith("mac");
    }

    public static boolean isLinux() {
        return System.getProperty("os.name").toLowerCase().startsWith("linux");
    }

    public static boolean isLinuxAarch64() {
        return isLinux() && isAarch64();
    }

    public static boolean isAarch64() {
        return "aarch64".equals(System.getProperty("os.arch"));
    }

    public static void waitForFile(File file, boolean additionalWait) {
        waitForFile(file, null, additionalWait);

    }

    public static void waitForFile(File file, File shutdownFile, boolean additionalWait) {
        long startTime = System.nanoTime();
        boolean isFile = file.isFile();
        while (!isFile && TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startTime) < 600) {
            LockSupport.parkNanos(1000L * 1000 * 100);
            if (shutdownFile != null && shutdownFile.isFile()) {
                throw new RuntimeException("shutdown");
            }
            isFile = file.isFile();
        }
        if (!isFile) {
            System.out.println(file + " not found");
            throw new RuntimeException(file + " not found");
        }
        if (additionalWait) {
            SleepHelper.sleep(3000);
        }
    }

    public static void createFile(File file) {
        try {
            RandomAccessFile raFile = new RandomAccessFile(file, "rw");
            raFile.write(0);
            raFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean isJava9Plus() {
        return !System.getProperty("java.version").startsWith("1.");
    }

    public static String getLibraryEnvVarName() {
        if (isWindows()) {
            return "PATH";
        } else if (isMac()) {
            return "DYLD_LIBRARY_PATH";
        } else {
            return "LD_LIBRARY_PATH";
        }
    }

    public static String getPlatformDescriptor() {
        if (isWindows()) {
            return isAarch64() ? "windows-aarch64" : "windows-x64";
        } else if (isMac()) {
            return "macos";
        } else {
            return isAarch64() ? "linux-aarch64" : "linux-x64";
        }
    }

    public static String removeCommonPrefix(String name) {
        String commonPrefix = "dev.jvmguard.integration.tests.";
        if (name.startsWith(commonPrefix)) {
            name = name.substring(commonPrefix.length());
        }
        String modulesPrefix = "modules.";
        if (name.startsWith(modulesPrefix)) {
            name = name.substring(modulesPrefix.length());
        }
        return name;
    }

    public static synchronized boolean isInWSL2() {
        if (wsl2 == null) {
            wsl2 = false;
            if (isLinux()) {
                try {
                    wsl2 = new String(Files.readAllBytes(Paths.get("/proc/version")), StandardCharsets.UTF_8).toLowerCase().contains("microsoft");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.out.println("WSL2: " + wsl2);
            }
        }
        return wsl2;
    }
}
