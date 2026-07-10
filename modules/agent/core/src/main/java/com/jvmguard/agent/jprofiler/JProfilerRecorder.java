package com.jvmguard.agent.jprofiler;

import com.jprofiler.api.controller.Controller;
import com.jprofiler.api.controller.HeapDumpOptions;
import com.jprofiler.api.controller.TrackingOptions;
import com.jvmguard.agent.JvmGuardAgent;
import com.jvmguard.agent.artifact.ArtifactHandler;
import com.jvmguard.agent.artifact.ArtifactHandlers;
import com.jvmguard.agent.artifact.ArtifactKind;
import com.jvmguard.agent.util.JvmGuardUtil;
import com.jvmguard.agent.util.ProcessHelper;
import com.jvmguard.agent.util.Util;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.concurrent.TimeUnit;

/**
 * Records a JProfiler snapshot from inside the monitored JVM:
 * - loads the JProfiler agent from the cached package with jpenable (config-less offline mode)
 * - drives recording of the selected subsystems and saves the snapshot through the Controller API
 */
public class JProfilerRecorder {

    // System property read by the JProfiler native agent to locate the shadowed controller API
    private static final String PROPNAME_CONTROLLER_PACKAGE = "jprofiler.controllerPackage";

    private static final long JPENABLE_TIMEOUT_SECONDS = 120;

    private static volatile String enabledArtifactKey = null;

    public static File record(String artifactKey, int seconds, String[] subsystems,
                              boolean heapDump, boolean heapDumpFullGc,
                              boolean mbeanSnapshot, boolean monitorDump) throws Exception {
        ArtifactHandler handler = ArtifactHandlers.get(ArtifactKind.JPROFILER);
        File packageDir = handler.getInstallDir(artifactKey);
        if (!handler.isReady(packageDir)) {
            throw new IOException("JProfiler agent package is not available at " + packageDir);
        }

        ensureProfilingEnabled(packageDir, artifactKey);

        // The snapshot should not be empty
        String[] effective = subsystems.length == 0
            ? new String[] {JProfilerRecordingNames.SUBSYSTEM_CPU} : subsystems;

        try {
            applyRecordings(effective, true);
            Thread.sleep(Math.max(0L, seconds) * 1000L);
            // Point-in-time captures at the end of the recording window, folded into the same snapshot.
            // Each is independent so one failure cannot abort the snapshot.
            applyDumps(heapDump, heapDumpFullGc, mbeanSnapshot, monitorDump);
            File snapshot = File.createTempFile("jvmguard_jprofiler", ".jps");
            Controller.saveSnapshot(snapshot);
            JvmGuardAgent.log("JProfiler snapshot saved: " + snapshot.length() + " bytes");
            return snapshot;
        } finally {
            applyRecordings(effective, false);
        }
    }

    /**
     * Triggers the optional point-in-time dumps that are stored alongside the windowed recording. The heap
     * dump is the expensive one (full-JVM pause plus a large in-snapshot payload); the MBean and monitor
     * dumps are cheap. Each is guarded independently so an unavailable capture cannot lose the snapshot.
     */
    private static void applyDumps(boolean heapDump, boolean heapDumpFullGc,
                                   boolean mbeanSnapshot, boolean monitorDump) {
        if (heapDump) {
            try {
                Controller.triggerHeapDump(heapDumpFullGc ? HeapDumpOptions.DEFAULT : HeapDumpOptions.NO_FULL_GC);
            } catch (Throwable t) {
                JvmGuardAgent.log(t);
            }
        }
        if (mbeanSnapshot) {
            try {
                Controller.triggerMBeanSnapshot(null, null);
            } catch (Throwable t) {
                JvmGuardAgent.log(t);
            }
        }
        if (monitorDump) {
            try {
                Controller.triggerMonitorDump();
            } catch (Throwable t) {
                JvmGuardAgent.log(t);
            }
        }
    }

    private static void applyRecordings(String[] subsystems, boolean start) {
        for (String subsystem : subsystems) {
            try {
                if (JProfilerRecordingNames.SUBSYSTEM_CPU.equals(subsystem)) {
                    if (start) {
                        Controller.startCPURecording(true);
                    } else {
                        Controller.stopCPURecording();
                    }
                } else if (JProfilerRecordingNames.SUBSYSTEM_ALLOCATION.equals(subsystem)) {
                    if (start) {
                        Controller.startAllocRecording(true);
                    } else {
                        Controller.stopAllocRecording();
                    }
                } else if (JProfilerRecordingNames.SUBSYSTEM_MONITORS.equals(subsystem)) {
                    if (start) {
                        Controller.startMonitorRecording();
                    } else {
                        Controller.stopMonitorRecording();
                    }
                } else {
                    String probeName = JProfilerRecordingNames.probeName(subsystem);
                    if (probeName != null) {
                        if (start) {
                            Controller.startProbeRecording(probeName, false);
                        } else {
                            Controller.stopProbeRecording(probeName);
                        }
                    }
                }
            } catch (Throwable t) {
                JvmGuardAgent.log(t);
            }
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
            enableProfiling(packageDir, currentPid());
            enabledArtifactKey = artifactKey;
        }
    }

    private static void enableProfiling(File packageDir, long pid) throws IOException, InterruptedException {
        File home = JProfilerLayout.resolveHome(packageDir);
        if (home == null) {
            throw new IOException("JProfiler package is incomplete at " + packageDir);
        }
        File jpenable = JProfilerLayout.jpenable(home);
        if (!jpenable.isFile()) {
            throw new IOException("jpenable not found at " + jpenable);
        }
        jpenable.setExecutable(true);

        System.setProperty(PROPNAME_CONTROLLER_PACKAGE, controllerPackage());

        ProcessBuilder processBuilder = new ProcessBuilder(
            jpenable.getAbsolutePath(),
            "--offline",
            "--pid=" + pid,
            "--call-tree-mode=sampling",
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

    private static String controllerPackage() {
        // Use a class that does not initialize the Controller class
        String name = TrackingOptions.class.getName();
        int lastDot = name.lastIndexOf('.');
        return name.substring(0, lastDot);
    }

    private static long currentPid() {
        if (Util.JAVA_MAJOR_VERSION >= 11) {
            return ProcessHelper.currentPid();
        }
        // Java 8 fallback: the RuntimeMXBean name is "<pid>@<host>".
        String name = ManagementFactory.getRuntimeMXBean().getName();
        int at = name.indexOf('@');
        return Long.parseLong(at > 0 ? name.substring(0, at) : name);
    }
}
