package dev.jvmguard.agent.data;

import dev.jvmguard.agent.JvmGuardAgent;
import dev.jvmguard.agent.base.logging.Subsystem;
import dev.jvmguard.agent.comm.CommunicationContext;
import dev.jvmguard.agent.helper.SnapshotRecorder;
import dev.jvmguard.agent.util.Logger;
import com.sun.management.HotSpotDiagnosticMXBean;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.util.concurrent.Future;

public class HeapDumpResult extends OrderedSnapshotTransferResult {

    @Override
    protected Future<File> getFuture(CommunicationContext context) {
        return SnapshotRecorder.execute(HeapDumpResult::dumpHprof);
    }

    private static File dumpHprof() {
        try {
            File file = Files.createTempFile(JvmGuardAgent.getAgentUserDir().toPath(), "jdm", ".hprof").toFile();
            if (!file.delete()) { // dumpHeap requires that the file does not exist yet
                Logger.log(Subsystem.COMMON, 1, true, "could not prepare heap dump file " + file);
                return null;
            }
            HotSpotDiagnosticMXBean diagnosticBean = ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);
            if (diagnosticBean == null) {
                return null;
            }
            diagnosticBean.dumpHeap(file.getAbsolutePath(), true);
            return file;
        } catch (Throwable t) {
            Logger.log(Subsystem.COMMON, 1, true, t);
            return null;
        }
    }

    @Override
    protected String getNullErrorMessage() {
        return "could not trigger heap dump";
    }
}
