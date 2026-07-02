package com.jvmguard.agent.config.transactions;

import com.jvmguard.agent.comm.*;
import com.jvmguard.agent.helper.matcher.PatternMatcher;

import java.io.DataInputStream;
import java.io.DataOutputStream;

public abstract class WildcardTransactionDef extends TransactionDef implements ComparisonTypeContainer {

    private ComparisonType comparisonType = ComparisonType.WILDCARD;

    private transient PatternMatcher patternMatcher;

    protected abstract String getFilter();

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
        super.readState(reader);
        comparisonType = reader.readEnum("comparisonType", ComparisonType.class);
    }

    @Override
    public void writeState(AgentWriter writer) throws Exception {
        super.writeState(writer);
        writer.writeEnum("comparisonType", comparisonType);
    }

    @Override
    public ComparisonType getComparisonType() {
        return comparisonType;
    }

    @Override
    public void setComparisonType(ComparisonType comparisonType) {
        ComparisonType oldValue = this.comparisonType;
        this.comparisonType = comparisonType;
        fireChanged(oldValue, comparisonType);
    }

    protected boolean isWildcardCommaSeparated() {
        return false;
    }

    protected boolean isUrlMatcher() {
        return false;
    }

    public boolean matches(String name) {
        ensureMatcher();
        return patternMatcher.matches(name);
    }

    private void ensureMatcher() {
        if (patternMatcher == null) {
            prepareForUsage();
        }
    }

    @Override
    public void prepareForUsage() {
        super.prepareForUsage();
        patternMatcher = PatternMatcher.create(getFilter(), comparisonType, isWildcardCommaSeparated(), isUrlMatcher(), needsRegexGroup());
    }

    protected boolean needsRegexGroup() {
        return false;
    }
}
