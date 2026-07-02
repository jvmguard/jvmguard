package com.jvmguard.agent.telemetry;

import com.jvmguard.agent.comm.CommunicationContext;
import com.jvmguard.agent.comm.ProtocolRequirement;
import com.jvmguard.agent.data.BaseResult;
import com.jvmguard.agent.data.Message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TelemetryResult extends BaseResult {

    private long totalMemory;
    private long freeMemory;
    private long maxMemory;
    private long cpuLoad;
    private long gcActivity;
    private long threadCount;

    private List<AdditionalData> additionalData = new ArrayList<>();
    private final List<Message> messages = new ArrayList<>();

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws IOException {
        while (in.readBoolean()) {
            AdditionalData data = new AdditionalData(in, context);
            additionalData.add(data);
        }

        totalMemory = in.readLong();
        freeMemory = in.readLong();
        if (context.satisfies(ProtocolRequirement.V6)) {
            maxMemory = in.readLong();
        } else {
            maxMemory = Long.MAX_VALUE;
        }
        cpuLoad = in.readLong();
        gcActivity = in.readLong();
        threadCount = in.readLong();

        while (in.readBoolean()) {
            Message message = new Message();
            message.read(context, in);
            messages.add(message);
        }
    }

    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws IOException {
        TelemetryCollector.getInstance().write(context, out);
    }

    public List<Message> getMessages() {
        return messages;
    }

    public long getTotalMemory() {
        return totalMemory;
    }

    public long getFreeMemory() {
        return freeMemory;
    }

    public long getMaxMemory() {
        return maxMemory;
    }

    public long getCpuLoad() {
        return cpuLoad;
    }

    public long getGcActivity() {
        return gcActivity;
    }

    public long getThreadCount() {
        return threadCount;
    }

    public List<AdditionalData> getAdditionalData() {
        return additionalData;
    }

}
