package com.jvmguard.agent.config.transactions;

import com.jvmguard.agent.comm.*;
import com.jvmguard.agent.config.base.CheckedString;
import com.jvmguard.agent.config.base.ConfigDoc;
import com.jvmguard.agent.instrument.transaction.annotation.AnnotationDefinition;
import com.jvmguard.agent.instrument.transaction.annotation.DeclaredAnnotationDefinition;

import java.io.DataInputStream;
import java.io.DataOutputStream;

@ConfigDoc("Transactions declared in code via jvmguard @MethodTransaction/@ClassTransaction annotations.")
public class DeclaredTransactionDef extends AnnotatedTransactionDef {

    public static String getDefaultName(CheckedString group, String className) {
        if (group.isChecked()) {
            return "Group \"" + group.getValue() + "\" [" + className + "]";
        } else {
            return className;
        }
    }

    @ConfigDoc("Optional transaction group name for these declared transactions, provided as an object with a " +
            "boolean 'checked' and a string 'value', where the value applies only when checked is true.")
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
            new DeclaredAnnotationDefinition(DeclaredAnnotationDefinition.CLASS_TRANSACTION_DESCRIPTOR, false, group.getUsedValue()),
            new DeclaredAnnotationDefinition(DeclaredAnnotationDefinition.METHOD_TRANSACTION_DESCRIPTOR, true, group.getUsedValue())
        };
    }

    @Override
    protected boolean isDefaultNamingActive() {
        return false;
    }

    @Override
    public TransactionType getTransactionType() {
        return TransactionType.DECLARED;
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
        return "DeclaredTransactionDef";
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
