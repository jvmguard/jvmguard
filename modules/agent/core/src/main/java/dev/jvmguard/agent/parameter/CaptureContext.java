package dev.jvmguard.agent.parameter;

import dev.jvmguard.agent.comm.AgentSerializable;
import dev.jvmguard.agent.comm.CommunicationContext;
import dev.jvmguard.agent.config.base.DefaultConstructor;
import dev.jvmguard.agent.config.transactions.PolicyEventType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class CaptureContext implements AgentSerializable {

    public enum Origin {
        TRIGGER,
        MANUAL
    }

    private Origin origin = Origin.MANUAL;
    private String triggerDescription = "";
    private String transactionName = "";
    private PolicyEventType policyEventType;

    @DefaultConstructor
    public CaptureContext() {
    }

    public CaptureContext(Origin origin, String triggerDescription, String transactionName, PolicyEventType policyEventType) {
        this.origin = origin;
        this.triggerDescription = triggerDescription == null ? "" : triggerDescription;
        this.transactionName = transactionName == null ? "" : transactionName;
        this.policyEventType = policyEventType;
    }

    public static CaptureContext manual() {
        return new CaptureContext(Origin.MANUAL, null, null, null);
    }

    public Origin getOrigin() {
        return origin;
    }

    public String getTriggerDescription() {
        return triggerDescription;
    }

    public String getTransactionName() {
        return transactionName;
    }

    public PolicyEventType getPolicyEventType() {
        return policyEventType;
    }

    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws IOException {
        out.writeUTF(origin.name());
        out.writeUTF(triggerDescription);
        out.writeUTF(transactionName);
        out.writeUTF(policyEventType == null ? "" : policyEventType.name());
    }

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws IOException {
        origin = Origin.valueOf(in.readUTF());
        triggerDescription = in.readUTF();
        transactionName = in.readUTF();
        String policyEventTypeName = in.readUTF();
        policyEventType = policyEventTypeName.isEmpty() ? null : PolicyEventType.valueOf(policyEventTypeName);
    }
}
