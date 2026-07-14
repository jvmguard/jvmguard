package dev.jvmguard.agent.data;

import dev.jvmguard.agent.comm.AgentSerializable;
import dev.jvmguard.agent.comm.CommunicationContext;
import dev.jvmguard.agent.config.base.LogCategory;
import dev.jvmguard.agent.util.JvmGuardUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class Message implements AgentSerializable {
    private String content;
    private LogCategory logCategory;
    private boolean inbox;
    private Map<String, String> additionalInfo;

    public Map<String, String> getAdditionalInfo() {
        return additionalInfo == null ? Collections.emptyMap() : additionalInfo;
    }

    public String getContent() {
        return content;
    }

    public LogCategory getLogCategory() {
        return logCategory;
    }

    public boolean isInbox() {
        return inbox;
    }

    @Override
    public String toString() {
        return "Message{" +
            "additionalInfo=" + additionalInfo +
            '}';
    }

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws IOException {
        content = in.readUTF();
        inbox = in.readBoolean();
        logCategory = LogCategory.valueOf(in.readUTF());

        int additionalSize = in.readInt();
        if (additionalSize > 0) {
            additionalInfo = new HashMap<>();
            for (int i = 0; i < additionalSize; i++) {
                String key = in.readUTF();
                String value = in.readUTF();
                additionalInfo.put(key, value);
            }
        }
    }

    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws IOException {
        JvmGuardUtil.writeCappedUTF(out, content);
        out.writeBoolean(inbox);
        out.writeUTF(logCategory.name());

        out.writeInt(getAdditionalInfo().size());
        for (Entry<String, String> entry : getAdditionalInfo().entrySet()) {
            out.writeUTF(entry.getKey());
            try {
                String value = entry.getValue();
                if (value.length() > 50000) {
                    value = value.substring(0, 50000);
                }
                out.writeUTF(value);
            } catch (Throwable t) {
                out.writeUTF("");
            }
        }
    }
}
