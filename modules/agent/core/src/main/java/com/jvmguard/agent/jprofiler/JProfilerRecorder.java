package com.jvmguard.agent.jprofiler;

import com.jprofiler.api.controller.Controller;
import com.jvmguard.agent.JvmGuardAgent;
import com.jvmguard.agent.artifact.ArtifactHandler;
import com.jvmguard.agent.artifact.ArtifactHandlers;
import com.jvmguard.agent.artifact.ArtifactKind;
import com.jvmguard.agent.util.JvmGuardUtil;
import com.jvmguard.agent.util.ProcessHelper;
import com.jvmguard.agent.util.Util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Records a JProfiler CPU snapshot from inside the monitored JVM
 * - loads the JProfiler agent from the cached package with jpenable
 * - drives CPU recording and snapshot saving through the Controller API
 */
public class JProfilerRecorder {

    //TODO remove offline config once jpenable inline config is available
    private static final String OFFLINE_CONFIG =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config version=\"10.0\">\n" +
            "  <licenseKey key=\"\"/>\n" +
            "  <sessions>\n" +
            "    <session id=\"1\" name=\"jvmguard\"/>\n" +
            "  </sessions>\n" +
            "</config>\n";
    private static final int OFFLINE_SESSION_ID = 1;
    private static final long JPENABLE_TIMEOUT_SECONDS = 120;

    private static volatile String enabledArtifactKey = null;

    public static File record(String artifactKey, int seconds) throws Exception {
        ArtifactHandler handler = ArtifactHandlers.get(ArtifactKind.JPROFILER);
        File packageDir = handler.getInstallDir(artifactKey);
        if (!handler.isReady(packageDir)) {
            throw new IOException("JProfiler agent package is not available at " + packageDir);
        }

        ensureProfilingEnabled(packageDir, artifactKey);

        Controller.startCPURecording(true);
        try {
            Thread.sleep(Math.max(0L, seconds) * 1000L);
            File snapshot = File.createTempFile("jvmguard_jprofiler", ".jps");
            Controller.saveSnapshot(snapshot);
            return snapshot;
        } finally {
            Controller.stopCPURecording();
        }
    }

    private static void ensureProfilingEnabled(File packageDir, String artifactKey) throws IOException, InterruptedException {
        String current = enabledArtifactKey;
        if (current != null) {
            if (!current.equals(artifactKey)) {
                JvmGuardAgent.log("JProfiler agent already loaded as " + current
                    + "; recording with it instead of " + artifactKey);
            }
            return;
        }
        synchronized (JProfilerRecorder.class) {
            if (enabledArtifactKey != null) {
                return;
            }
            File configFile = writeOfflineConfig();
            try {
                enableProfiling(packageDir, currentPid(), configFile);
                enabledArtifactKey = artifactKey;
            } finally {
                if (!configFile.delete()) {
                    configFile.deleteOnExit();
                }
            }
        }
    }

    private static void enableProfiling(File packageDir, long pid, File configFile) throws IOException, InterruptedException {
        File home = JProfilerLayout.resolveHome(packageDir);
        if (home == null) {
            throw new IOException("JProfiler package is incomplete at " + packageDir);
        }
        File jpenable = JProfilerLayout.jpenable(home);
        if (!jpenable.isFile()) {
            throw new IOException("jpenable not found at " + jpenable);
        }
        jpenable.setExecutable(true);

        ProcessBuilder processBuilder = new ProcessBuilder(
            jpenable.getAbsolutePath(),
            "--offline",
            "--pid=" + pid,
            "--config=" + configFile.getAbsolutePath(),
            "--id=" + OFFLINE_SESSION_ID,
            "--noinput"
        );
        processBuilder.environment().put("INSTALL4J_JAVA_HOME_OVERRIDE", System.getProperty("java.home"));
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        final StringBuilder output = new StringBuilder();
        Thread drain = new Thread(JvmGuardAgent.AGENT_THREAD_GROUP, () -> {
            try {
                output.append(JvmGuardUtil.readToString(process.getInputStream()));
            } catch (IOException ignored) {
            }
        }, "_jvmguard_jpenable");
        drain.setDaemon(true);
        drain.start();

        boolean finished = process.waitFor(JPENABLE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IOException("jpenable timed out after " + JPENABLE_TIMEOUT_SECONDS + "s");
        }
        drain.join(1000);

        int exitValue = process.exitValue();
        if (exitValue != 0) {
            throw new IOException("jpenable failed (exit " + exitValue + "): " + output);
        }
        JvmGuardAgent.log("jpenable loaded JProfiler agent: " + output);
    }

    private static File writeOfflineConfig() throws IOException {
        File configFile = File.createTempFile("jprofiler_config", ".xml");
        try (Writer out = new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8)) {
            out.write(OFFLINE_CONFIG);
        }
        return configFile;
    }

    private static long currentPid() {
        if (Util.JAVA_MAJOR_VERSION >= 11) {
            return ProcessHelper.currentPid();
        } else {
            // Java 8 fallback: the RuntimeMXBean name is "<pid>@<host>".
            String name = ManagementFactory.getRuntimeMXBean().getName();
            int at = name.indexOf('@');
            return Long.parseLong(at > 0 ? name.substring(0, at) : name);
        }
    }
}
