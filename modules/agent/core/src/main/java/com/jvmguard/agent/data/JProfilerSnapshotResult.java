package com.jvmguard.agent.data;

import com.jvmguard.agent.comm.CommunicationContext;
import com.jvmguard.agent.comm.ProtocolRequirement;
import com.jvmguard.agent.helper.SnapshotRecorder;
import com.jvmguard.agent.jprofiler.JProfilerRecorder;
import com.jvmguard.agent.parameter.JProfilerRecordParameters;

import java.io.File;
import java.util.concurrent.Future;

public class JProfilerSnapshotResult extends OrderedSnapshotTransferResult {

    @Override
    protected Future<File> getFuture(CommunicationContext context) {
        final JProfilerRecordParameters parameters =
            (JProfilerRecordParameters)context.getProperty(CommunicationContext.PROPERTY_PARAMETER);
        return SnapshotRecorder.execute(() ->
            JProfilerRecorder.record(
                parameters.getArtifactKey(), parameters.getSeconds(), parameters.getSubsystems(),
                parameters.isHeapDump(), parameters.isHeapDumpFullGc(),
                parameters.isMbeanSnapshot(), parameters.isMonitorDump()));
    }

    @Override
    protected String getNullErrorMessage() {
        return "could not record JProfiler snapshot";
    }

    @Override
    protected boolean isCompress() {
        return false;
    }

    @Override
    public ProtocolRequirement getProtocolRequirement() {
        return ProtocolRequirement.V8;
    }
}
