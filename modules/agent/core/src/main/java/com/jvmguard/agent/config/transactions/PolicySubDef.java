package com.jvmguard.agent.config.transactions;

import com.jvmguard.agent.comm.*;
import com.jvmguard.agent.config.base.AbstractEntity;
import com.jvmguard.agent.config.base.CheckedString;
import com.jvmguard.agent.config.base.DefaultConstructor;
import com.jvmguard.agent.config.base.EntityChangeListener;
import com.jvmguard.agent.helper.matcher.PatternMatcher;
import com.jvmguard.agent.policy.PolicyHandler;

import java.io.DataInputStream;
import java.io.DataOutputStream;

public class PolicySubDef extends AbstractEntity implements PolicyDef, AgentSerializable, CodecEntity {

    private boolean discard = false;
    private Policy policy;

    private String filter = "";
    private ComparisonType comparisonType = ComparisonType.WILDCARD;
    private boolean wildcardCommaSeparated = true;
    private CheckedString description = new CheckedString(false, "");

    private transient PolicyHandler policyHandler;
    private transient PatternMatcher patternMatcher;

    public PolicySubDef(TransactionDef transactionDef) {
        policy = transactionDef == null ? new Policy() : transactionDef.createPolicy();
    }

    @DefaultConstructor
    public PolicySubDef() {
        this(null);
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        String oldValue = this.filter;
        this.filter = filter;
        fireChanged(oldValue, filter);
    }

    public ComparisonType getComparisonType() {
        return comparisonType;
    }

    public void setComparisonType(ComparisonType comparisonType) {
        ComparisonType oldValue = this.comparisonType;
        this.comparisonType = comparisonType;
        fireChanged(oldValue, comparisonType);
    }

    public boolean isWildcardCommaSeparated() {
        return wildcardCommaSeparated;
    }

    public void setWildcardCommaSeparated(boolean wildcardCommaSeparated) {
        boolean oldValue = this.wildcardCommaSeparated;
        this.wildcardCommaSeparated = wildcardCommaSeparated;
        fireChanged(oldValue, wildcardCommaSeparated);
    }

    public CheckedString getDescription() {
        return description;
    }

    public void setDescription(CheckedString description) {
        CheckedString oldValue = this.description;
        this.description = description;
        fireChanged(oldValue, description);
    }

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

    @Override
    public Policy getPolicy() {
        return policy;
    }

    @Override
    public void setPolicy(Policy policy) {
        this.policy = policy;
    }

    @Override
    public PolicyHandler getPolicyHandler() {
        return policyHandler;
    }

    @Override
    public PolicyHandler initPolicyHandler() {
        if (policy != null && policy.isActive()) {
            policyHandler = new PolicyHandler(policy);
        }
        return policyHandler;
    }

    public boolean matches(String name) {
        if (patternMatcher == null) {
            prepareForUsage();
        }
        return patternMatcher.matches(name);
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
        return "PolicySubDef";
    }

    @Override
    public void readState(AgentReader reader) throws Exception {
        setId(reader.readLong("id"));
        discard = reader.readBoolean("discard");
        policy = reader.readObject("policy");

        filter = reader.readString("filter");
        comparisonType = reader.readEnum("comparisonType", ComparisonType.class);
        wildcardCommaSeparated = reader.readBoolean("wildcardCommaSeparated");
    }

    @Override
    public void writeState(AgentWriter writer) throws Exception {
        writer.writeLong("id", getId() == null ? 0 : getId());
        writer.writeBoolean("discard", discard);
        writer.writeObject("policy", policy);

        writer.writeString("filter", filter);
        writer.writeEnum("comparisonType", comparisonType);
        writer.writeBoolean("wildcardCommaSeparated", wildcardCommaSeparated);
    }

    @Override
    public void addChangeListener(EntityChangeListener listener) {
        super.addChangeListener(listener);
        policy.addChangeListener(listener);
    }

    @Override
    public void removeChangeListener(EntityChangeListener listener) {
        super.removeChangeListener(listener);
        policy.removeChangeListener(listener);
    }

    @Override
    public ProtocolRequirement getProtocolRequirement() {
        return ProtocolRequirement.V1;
    }

    public void prepareForUsage() {
        patternMatcher = PatternMatcher.create(filter, comparisonType, wildcardCommaSeparated, false, false);
    }

}
