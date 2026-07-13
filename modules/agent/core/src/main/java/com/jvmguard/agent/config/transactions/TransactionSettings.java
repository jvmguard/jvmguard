package com.jvmguard.agent.config.transactions;

import com.jvmguard.agent.comm.*;
import com.jvmguard.agent.config.base.ConfigDoc;
import com.jvmguard.agent.config.base.OptionalConfig;
import com.jvmguard.agent.config.recording.RetransformationType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;

public class TransactionSettings extends OptionalConfig implements TransactionDefProvider, AgentSerializable, CodecEntity {

    public static void assignIds(List<TransactionDef> transactionDefs) {
        long lastTransactionId = 0;
        for (TransactionDef transactionDef : transactionDefs) {
            Long id = transactionDef.getId();
            if (id != null) {
                try {
                    lastTransactionId = Math.max(lastTransactionId, id);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }

        for (TransactionDef transactionDef : transactionDefs) {
            if (transactionDef.getId() == null) {
                transactionDef.setId(++lastTransactionId);
            }
        }
    }

    @ConfigDoc("The ordered list of transaction definitions (MATCHED/MAPPED/DECLARED) that select which " +
            "methods/classes are instrumented as transactions.")
    private List<TransactionDef> transactionDefs = new ArrayList<>();

    @ConfigDoc("When the agent re-instruments already-loaded classes after this transaction config changes.")
    private RetransformationType retransformationType = RetransformationType.ALWAYS;

    public TransactionSettings() {
    }

    @Override
    public List<TransactionDef> getTransactionDefs() {
        return transactionDefs;
    }

    public void setTransactionDefs(List<TransactionDef> transactionDefs) {
        this.transactionDefs = transactionDefs;
        assignIds(transactionDefs);
        fireChanged(); // always fire, so only call setter if changed
    }

    public RetransformationType getRetransformationType() {
        return retransformationType;
    }

    public void setRetransformationType(RetransformationType retransformationType) {
        RetransformationType oldValue = this.retransformationType;
        this.retransformationType = retransformationType;
        fireChanged(oldValue, retransformationType);
    }

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws Exception {
        readState(new BinaryAgentReader(in));
    }

    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws Exception {
        writeState(new BinaryAgentWriter(out));
    }

    @Override
    public String codecType() {
        return "TransactionSettings";
    }

    @Override
    public void readState(AgentReader reader) throws Exception {
        reader.readList("transactionDefs", transactionDefs);
        retransformationType = reader.readEnum("retransformationType", RetransformationType.class);
    }

    @Override
    public void writeState(AgentWriter writer) throws Exception {
        writer.writeList("transactionDefs", transactionDefs);
        writer.writeEnum("retransformationType", retransformationType);
    }

    @Override
    public String toString() {
        return "TransactionSettings{" +
            "transactionDefs=" + transactionDefs +
            '}';
    }
}
