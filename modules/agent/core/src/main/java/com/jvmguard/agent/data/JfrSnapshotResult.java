package com.jvmguard.agent.data;

import com.jvmguard.agent.util.Util;
import com.jvmguard.agent.comm.CommunicationContext;
import com.jvmguard.agent.helper.SnapshotRecorder;
import com.jvmguard.agent.jfr.JfrRecorder;
import com.jvmguard.agent.parameter.JfrRecordParameters;

import java.io.File;
import java.util.concurrent.Future;

public class JfrSnapshotResult extends OrderedSnapshotTransferResult {
    @Override
    protected Future<File> getFuture(CommunicationContext context) {
        final JfrRecordParameters recordParameters = (JfrRecordParameters)context.getProperty(CommunicationContext.PROPERTY_PARAMETER);
        return SnapshotRecorder.execute(() -> {
            if (Util.JAVA_MAJOR_VERSION < 11) {
                throw new RuntimeException("JFR recording is only supported for Java 11+");
            }
            return JfrRecorder.record(
                recordParameters.getRecordingName(),
                recordParameters.getSeconds(),
                recordParameters.isPredefined(),
                recordParameters.getProfileNameOrSettings()
            );
        });
    }

    @Override
    protected String getNullErrorMessage() {
        return "could not record JFR snapshot";
    }

    @Override
    protected boolean isCompress() {
        return false;
    }
}
