package com.jvmguard.agent.config.transactions;

import com.jvmguard.agent.base.logging.Subsystem;
import com.jvmguard.agent.comm.*;
import com.jvmguard.agent.config.base.AbstractEntity;
import com.jvmguard.agent.config.base.CheckedString;
import com.jvmguard.agent.config.base.EntityChangeListener;
import com.jvmguard.agent.config.base.Hierarchical;
import com.jvmguard.agent.config.transactions.naming.MethodParameterElement;
import com.jvmguard.agent.config.transactions.naming.TextElement;
import com.jvmguard.agent.policy.PolicyHandler;
import com.jvmguard.agent.util.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

public abstract class TransactionDef extends AbstractEntity implements Hierarchical, AgentSerializable, PolicyDef, CodecEntity {

    public static final PolicyDef DISCARD_POLICY_DEF = new PolicySubDef(null);

    protected static void commonAppendTransactionName(NamingElement namingElement, StringBuilder buffer) {
        if (namingElement instanceof TextElement) {
            ((TextElement)namingElement).appendName(buffer);
        }
    }

    private CheckedString description;

    private boolean discard = false;

    private Policy policy;
    private List<PolicySubDef> policySubDefs = new ArrayList<>();

    private TransactionNaming naming;

    private transient PolicyHandler policyHandler;
    private transient boolean namingChecked = false;
    private transient boolean needsArguments = false;

    protected TransactionDef() {
        policy = createPolicy();
        naming = new TransactionNaming();
        naming.setActive(isDefaultNamingActive());
        description = new CheckedString(false, "");
    }

    protected Policy createPolicy() {
        return new Policy();
    }

    protected boolean isDefaultNamingActive() {
        return true;
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
    public void readState(AgentReader reader) throws Exception {
        setId(reader.readLong("id"));
        description = reader.readCheckedString("description");

        discard = reader.readBoolean("discard");

        policy = reader.readObject("policy");
        reader.readList("policySubDefs", policySubDefs);

        naming = reader.readObject("naming");
    }

    @Override
    public void writeState(AgentWriter writer) throws Exception {
        writer.writeLong("id", getId() == null ? 0 : getId());
        writer.writeCheckedString("description", description);

        writer.writeBoolean("discard", discard);

        writer.writeObject("policy", policy);
        writer.writeList("policySubDefs", policySubDefs);

        writer.writeObject("naming", naming);
    }

    public List<PolicySubDef> getPolicySubDefs() {
        return policySubDefs;
    }


    @Override
    public ProtocolRequirement getProtocolRequirement() {
        return ProtocolRequirement.V1;
    }

    public abstract void initDefault();
    public abstract TransactionType getTransactionType();
    public abstract String getAutomaticName();

    @Override

    public boolean isDiscard() {
        return discard;
    }

    @Override

    public void setDiscard(boolean discard) {
        boolean oldValue = this.discard;
        this.discard = discard;
        fireChanged(oldValue, discard);
    }


    public CheckedString getDescription() {
        return description;
    }


    public void setDescription(CheckedString description) {
        CheckedString oldValue = this.description;
        this.description = description;
        fireChanged(oldValue, description);
    }

    public boolean isNamingActive() {
        return naming.isActive();
    }

    public boolean isPolicyActive() {
        return policy.isActive();
    }

    @Override

    public Policy getPolicy() {
        return policy;
    }

    @Override

    public void setPolicy(Policy policy) {
        this.policy = policy;
    }


    public TransactionNaming getNaming() {
        return naming;
    }


    public void setNaming(TransactionNaming naming) {
        this.naming = naming;
    }

    @Override
    public char getHierarchySeparatorChar() {
        return '/';
    }

    @Override
    public void setHierarchyPath(String hierarchyPath) {
        // cannot be changed
    }

    @Override
    public String getHierarchyPath() {
        return getTransactionType().name() + "/" + getDisplayName().replace('/', '_');
    }

    public String getDisplayName() {
        StringBuilder buffer = new StringBuilder();
        if (isDiscard()) {
            buffer.append("[Discard] ");
        }
        CheckedString description = getDescription();
        if (description.isChecked()) {
            buffer.append(description.getValue());
        } else {
            buffer.append(getAutomaticName());
        }
        return buffer.toString();
    }

    @Override
    public void addChangeListener(EntityChangeListener listener) {
        super.addChangeListener(listener);
        policy.addChangeListener(listener);
        naming.addChangeListener(listener);
    }

    @Override
    public void removeChangeListener(EntityChangeListener listener) {
        super.removeChangeListener(listener);
        policy.removeChangeListener(listener);
        naming.removeChangeListener(listener);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        initListeners();
    }

    private void initListeners() {
        policy.addChangeListener(this);
        naming.addChangeListener(this);
    }

    @Override
    public String toString() {
        return getDisplayName();
    }

    protected static void handleEnvironmentException(EnvironmentException e, Object environment, int elementNumber) {
        if (Boolean.getBoolean("jvmguard.debug")) {
            e.printStackTrace();
        }
        try {
            if (Logger.isEnabled(Subsystem.USER, 10)) {
                Logger.log(Subsystem.USER, 10, true, "error in transaction name for element %d on %s: %s\n", elementNumber, environment, e.getMessage());
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public PolicyHandler initPolicyHandler() {
        if (policy != null && policy.isActive()) {
            policyHandler = new PolicyHandler(policy);
        }
        return policyHandler;
    }

    @Override
    public PolicyHandler getPolicyHandler() {
        return policyHandler;
    }

    public boolean needsArguments() {
        if (!namingChecked) {
            needsArguments = needsArguments(this);
            namingChecked = true;
        }
        return needsArguments;
    }

    private static boolean needsArguments(TransactionDef transactionDef) {
        for (NamingElement namingElement : transactionDef.getNaming().getNamingElements()) {
            if (namingElement instanceof MethodParameterElement) {
                return true;
            }
        }
        return false;
    }

    public String getUsedGroup() {
        return getNaming().getGroup().getUsedValue();
    }

    public PolicyDef findPolicyDef(String transactionName) {
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < policySubDefs.size(); i++) {
            PolicySubDef policySubDef = policySubDefs.get(i);
            if (policySubDef.matches(transactionName)) {
                if (policySubDef.isDiscard()) {
                    return DISCARD_POLICY_DEF;
                } else {
                    return policySubDef;
                }
            }
        }
        return this;
    }

    public void prepareForUsage() {
        if (isNamingActive()) {
            naming.prepareForUsage();
        }
    }
}
