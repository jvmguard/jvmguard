package com.jvmguard.agent.config.transactions;

import com.jvmguard.agent.comm.*;
import com.jvmguard.agent.config.base.AbstractEntity;
import com.jvmguard.agent.config.base.CheckedString;
import com.jvmguard.agent.config.base.ConfigDoc;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;

public class TransactionNaming extends AbstractEntity implements AgentSerializable, CodecEntity {

    private static final List<List<NamingElement>> uniqueNamings = new ArrayList<>();

    @ConfigDoc("Whether custom transaction naming is active (else an automatic name is used).")
    private boolean active = true;
    @ConfigDoc("On recursive re-entry into instrumented code, which further entries are suppressed.")
    private ReentryInhibition reentryInhibition = ReentryInhibition.NAME;
    @ConfigDoc("Optional transaction group name for this definition, provided as an object with a boolean " +
            "'checked' and a string 'value', where the value applies only when checked is true.")
    private CheckedString group = new CheckedString();
    @ConfigDoc("Ordered list of naming elements concatenated to build each transaction's name.")
    private List<NamingElement> namingElements = new ArrayList<>();

    private int namingIdentifier;

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        boolean oldValue = this.active;
        this.active = active;
        fireChanged(oldValue, active);
    }


    public ReentryInhibition getReentryInhibition() {
        return reentryInhibition;
    }


    public void setReentryInhibition(ReentryInhibition reentryInhibition) {
        ReentryInhibition oldValue = this.reentryInhibition;
        this.reentryInhibition = reentryInhibition;
        fireChanged(oldValue, reentryInhibition);
    }

    public CheckedString getGroup() {
        return group;
    }

    public void setGroup(CheckedString group) {
        CheckedString oldName = this.group;
        this.group = group;
        fireChanged(oldName, group);
    }

    public List<NamingElement> getNamingElements() {
        return namingElements;
    }

    public void setNamingElements(List<NamingElement> namingElements) {
        this.namingElements = namingElements;
        fireChanged(false, true); // always fire, so only call setter if changed
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
        return "TransactionNaming";
    }

    @Override
    public void readState(AgentReader reader) throws Exception {
        active = reader.readBoolean("active");
        reentryInhibition = reader.readEnum("reentryInhibition", ReentryInhibition.class);
        group = reader.readCheckedString("group");
        reader.readList("namingElements", namingElements);
    }

    @Override
    public void writeState(AgentWriter writer) throws Exception {
        writer.writeBoolean("active", active);
        writer.writeEnum("reentryInhibition", reentryInhibition);
        writer.writeCheckedString("group", group);
        writer.writeList("namingElements", namingElements);
    }

    @Override
    public ProtocolRequirement getProtocolRequirement() {
        return ProtocolRequirement.V1;
    }

    private boolean canBeStatic() {
        for (NamingElement namingElement : namingElements) {
            if (!namingElement.canBeStatic()) {
                return false;
            }
        }
        return true;
    }

    public int namingIdentifier() {
        return namingIdentifier;
    }

    public void prepareForUsage() {
        if (canBeStatic()) {
            namingIdentifier = calculateNamingIdentifier(namingElements);
        } else {
            namingIdentifier = 0;
        }
    }

    private static synchronized int calculateNamingIdentifier(List<NamingElement> namingElements) {
        for (int uniqueIndex = 0; uniqueIndex < uniqueNamings.size(); uniqueIndex++) {
            List<NamingElement> previousElements = uniqueNamings.get(uniqueIndex);
            if (isIdentical(previousElements, namingElements)) {
                return uniqueIndex + 1;
            }
        }
        uniqueNamings.add(namingElements);
        return uniqueNamings.size();
    }

    private static boolean isIdentical(List<NamingElement> list1, List<NamingElement> list2) {
        if (list1.size() != list2.size()) {
            return false;
        }
        for (int i = 0; i < list1.size(); i++) {
            if (!list1.get(i).isIdentical(list2.get(i))) {
                return false;
            }
        }
        return true;
    }
}
