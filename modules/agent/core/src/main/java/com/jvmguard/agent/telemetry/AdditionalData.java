package com.jvmguard.agent.telemetry;

import com.jvmguard.agent.comm.CommunicationContext;
import com.jvmguard.agent.comm.ProtocolRequirement;
import com.jvmguard.annotation.TelemetryFormat;

import java.io.DataInput;
import java.io.IOException;

public class AdditionalData {
    private int type;
    private String name;
    private long value;

    private TelemetryFormat format;

    public AdditionalData(DataInput in, CommunicationContext context) throws IOException {
        type = in.readInt();
        name = in.readUTF();
        value = in.readLong();
        if (context.satisfies(ProtocolRequirement.V2)) {
            if (in.readBoolean()) {
                format = new TelemetryFormatImpl(in, context);
            }
        }
    }

    public int getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public long getValue() {
        return value;
    }

    public TelemetryFormat getFormat() {
        return format;
    }

    @Override
    public String toString() {
        return "AdditionalData{" +
            "type=" + type +
            ", name='" + name + '\'' +
            ", value=" + value +
            ", format=" + format +
            '}';
    }
}
