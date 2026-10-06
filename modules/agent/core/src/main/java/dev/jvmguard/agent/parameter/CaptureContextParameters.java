package dev.jvmguard.agent.parameter;

import dev.jvmguard.agent.comm.CommunicationContext;
import dev.jvmguard.agent.comm.ProtocolRequirement;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class CaptureContextParameters extends BaseParameter implements CaptureContextProvider {

    private CaptureContext captureContext;

    protected CaptureContextParameters() {
    }

    protected CaptureContextParameters(CaptureContext captureContext) {
        this.captureContext = captureContext;
    }

    @Override
    public CaptureContext getCaptureContext() {
        return captureContext;
    }

    protected void writeCaptureContext(CommunicationContext context, DataOutputStream out) throws IOException {
        if (context.satisfies(ProtocolRequirement.V2)) {
            out.writeBoolean(captureContext != null);
            if (captureContext != null) {
                captureContext.write(context, out);
            }
        }
    }

    protected void readCaptureContext(CommunicationContext context, DataInputStream in) throws IOException {
        if (context.satisfies(ProtocolRequirement.V2) && in.readBoolean()) {
            captureContext = new CaptureContext();
            captureContext.read(context, in);
        }
    }
}
