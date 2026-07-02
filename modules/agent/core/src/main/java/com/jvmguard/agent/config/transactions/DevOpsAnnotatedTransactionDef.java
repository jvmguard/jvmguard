package com.jvmguard.agent.config.transactions;

import com.jvmguard.agent.comm.*;
import com.jvmguard.agent.config.base.CheckedString;
import com.jvmguard.agent.instrument.transaction.annotation.AnnotationDefinition;
import com.jvmguard.agent.instrument.transaction.annotation.DevOpsAnnotationDefinition;

import java.io.DataInputStream;
import java.io.DataOutputStream;

public class DevOpsAnnotatedTransactionDef extends AnnotatedTransactionDef {

    public static String getDefaultName(CheckedString group, String className) {
        if (group.isChecked()) {
            return "Group \"" + group.getValue() + "\" [" + className + "]";
        } else {
            return className;
        }
    }

    private CheckedString group = new CheckedString();

    public CheckedString getGroup() {
        return group;
    }

    public void setGroup(CheckedString group) {
        CheckedString oldName = this.group;
        this.group = group;
        fireChanged(oldName, group);
    }

    @Override
    public AnnotationDefinition[] getAnnotationDefinitions() {
        return new AnnotationDefinition[] {
            new DevOpsAnnotationDefinition(DevOpsAnnotationDefinition.CLASS_TRANSACTION_DESCRIPTOR, false, group.getUsedValue()),
            new DevOpsAnnotationDefinition(DevOpsAnnotationDefinition.METHOD_TRANSACTION_DESCRIPTOR, true, group.getUsedValue())
        };
    }

    @Override
    protected boolean isDefaultNamingActive() {
        return false;
    }

    @Override
    public TransactionType getTransactionType() {
        return TransactionType.DEVOPS;
    }

    @Override
    public String getAutomaticName() {
        return getDefaultName(group, getClassName());
    }

    @Override
    public String getUsedGroup() {
        return group.getUsedValue();
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
        return "DevOpsAnnotatedTransactionDef";
    }

    @Override
    public void readState(AgentReader reader) throws Exception {
        super.readState(reader);
        group = reader.readCheckedString("group");
    }

    @Override
    public void writeState(AgentWriter writer) throws Exception {
        super.writeState(writer);
        writer.writeCheckedString("group", group);
    }
}
