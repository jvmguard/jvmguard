package dev.jvmguard.agent.parameter;

import dev.jvmguard.agent.comm.CommunicationContext;
import dev.jvmguard.agent.config.base.DefaultConstructor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class HeapDumpParameters extends CaptureContextParameters {

    @DefaultConstructor
    public HeapDumpParameters() {
    }

    public HeapDumpParameters(CaptureContext captureContext) {
        super(captureContext);
    }

    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws IOException {
        writeCaptureContext(context, out);
    }

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws IOException {
        readCaptureContext(context, in);
    }
}
