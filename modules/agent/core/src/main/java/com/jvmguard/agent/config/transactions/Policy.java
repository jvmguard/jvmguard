package com.jvmguard.agent.config.transactions;

import com.jvmguard.agent.comm.*;
import com.jvmguard.agent.config.base.AbstractEntity;
import com.jvmguard.agent.config.base.ConfigDoc;

import java.io.DataInputStream;
import java.io.DataOutputStream;

public class Policy extends AbstractEntity implements AgentSerializable, CodecEntity {

    @ConfigDoc("Whether this policy is active (health evaluation / thresholds applied to matching transactions).")
    private boolean active = true;

    @ConfigDoc("Interpretation of slowValue: PERCENT (percent slower than the running average) or MILLIS " +
            "(absolute ms).")
    private DurationType slowDurationType = DurationType.PERCENT;
    @ConfigDoc("Threshold above which a call is classified slow (unit per slowDurationType).")
    private int slowValue = 500; // percent or ms

    @ConfigDoc("Interpretation of verySlowValue (percent-over-average vs absolute ms).")
    private DurationType verySlowDurationType = DurationType.PERCENT;
    @ConfigDoc("Threshold above which a call is classified very slow.")
    private int verySlowValue = 1000; // percent or ms

    @ConfigDoc("Interpretation of overdueValue (percent vs ms) for a still-running call.")
    private DurationType overdueDurationType = DurationType.MILLIS;
    @ConfigDoc("Duration after which an in-progress call is flagged overdue.")
    private int overdueValue = 500000; // percent or ms

    @ConfigDoc("Treat thrown checked exceptions as transaction errors.")
    private boolean checkedExceptionAsError = false;
    @ConfigDoc("Treat thrown runtime exceptions as transaction errors.")
    private boolean runtimeExceptionAsError = true;
    @ConfigDoc("Treat thrown java.lang.Error throwables as transaction errors.")
    private boolean errorThrowableAsError = true;

    @ConfigDoc("Treat logged WARN-level messages during the call as transaction errors.")
    private boolean loggedWarningAsError = false;
    @ConfigDoc("Treat logged ERROR-level messages during the call as transaction errors.")
    private boolean loggedErrorAsError = true;

    @ConfigDoc("Whether to split the call tree at this transaction boundary.")
    private boolean splitTree = true;

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        boolean oldValue = this.active;
        this.active = active;
        fireChanged(oldValue, active);
    }

    public DurationType getSlowDurationType() {
        return slowDurationType;
    }

    public void setSlowDurationType(DurationType slowDurationType) {
        DurationType oldValue = this.slowDurationType;
        this.slowDurationType = slowDurationType;
        fireChanged(oldValue, slowDurationType);
    }

    public int getSlowValue() {
        return slowValue;
    }

    public void setSlowValue(int slowValue) {
        int oldValue = this.slowValue;
        this.slowValue = slowValue;
        fireChanged(oldValue, slowValue);
    }

    public DurationType getVerySlowDurationType() {
        return verySlowDurationType;
    }

    public void setVerySlowDurationType(DurationType verySlowDurationType) {
        DurationType oldValue = this.verySlowDurationType;
        this.verySlowDurationType = verySlowDurationType;
        fireChanged(oldValue, verySlowDurationType);
    }

    public int getVerySlowValue() {
        return verySlowValue;
    }

    public void setVerySlowValue(int verySlowValue) {
        int oldValue = this.verySlowValue;
        this.verySlowValue = verySlowValue;
        fireChanged(oldValue, verySlowValue);
    }

    public DurationType getOverdueDurationType() {
        return overdueDurationType;
    }

    public void setOverdueDurationType(DurationType overdueDurationType) {
        DurationType oldValue = this.overdueDurationType;
        this.overdueDurationType = overdueDurationType;
        fireChanged(oldValue, overdueDurationType);
    }

    public int getOverdueValue() {
        return overdueValue;
    }

    public void setOverdueValue(int overdueValue) {
        int oldValue = this.overdueValue;
        this.overdueValue = overdueValue;
        fireChanged(oldValue, overdueValue);
    }

    public boolean isCheckedExceptionAsError() {
        return checkedExceptionAsError;
    }

    public void setCheckedExceptionAsError(boolean checkedExceptionAsError) {
        boolean oldValue = this.checkedExceptionAsError;
        this.checkedExceptionAsError = checkedExceptionAsError;
        fireChanged(oldValue, checkedExceptionAsError);
    }

    public boolean isRuntimeExceptionAsError() {
        return runtimeExceptionAsError;
    }

    public void setRuntimeExceptionAsError(boolean runtimeExceptionAsError) {
        boolean oldValue = this.runtimeExceptionAsError;
        this.runtimeExceptionAsError = runtimeExceptionAsError;
        fireChanged(oldValue, runtimeExceptionAsError);
    }

    public boolean isErrorThrowableAsError() {
        return errorThrowableAsError;
    }

    public void setErrorThrowableAsError(boolean errorThrowableAsError) {
        boolean oldValue = this.errorThrowableAsError;
        this.errorThrowableAsError = errorThrowableAsError;
        fireChanged(oldValue, errorThrowableAsError);
    }

    public boolean isLoggedWarningAsError() {
        return loggedWarningAsError;
    }

    public void setLoggedWarningAsError(boolean loggedWarningAsError) {
        boolean oldValue = this.loggedWarningAsError;
        this.loggedWarningAsError = loggedWarningAsError;
        fireChanged(oldValue, loggedWarningAsError);
    }

    public boolean isLoggedErrorAsError() {
        return loggedErrorAsError;
    }

    public void setLoggedErrorAsError(boolean loggedErrorAsError) {
        boolean oldValue = this.loggedErrorAsError;
        this.loggedErrorAsError = loggedErrorAsError;
        fireChanged(oldValue, loggedErrorAsError);
    }

    public boolean isSplitTree() {
        return splitTree;
    }

    public void setSplitTree(boolean splitTree) {
        boolean oldValue = this.splitTree;
        this.splitTree = splitTree;
        fireChanged(oldValue, splitTree);
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
        return "Policy";
    }

    @Override
    public void readState(AgentReader reader) throws Exception {
        active = reader.readBoolean("active");

        slowDurationType = reader.readEnum("slowDurationType", DurationType.class);
        slowValue = reader.readInt("slowValue");

        verySlowDurationType = reader.readEnum("verySlowDurationType", DurationType.class);
        verySlowValue = reader.readInt("verySlowValue");

        overdueDurationType = reader.readEnum("overdueDurationType", DurationType.class);
        overdueValue = reader.readInt("overdueValue");

        splitTree = reader.readBoolean("splitTree");

        checkedExceptionAsError = reader.readBoolean("checkedExceptionAsError");
        runtimeExceptionAsError = reader.readBoolean("runtimeExceptionAsError");
        errorThrowableAsError = reader.readBoolean("errorThrowableAsError");

        loggedWarningAsError = reader.readBoolean("loggedWarningAsError");
        loggedErrorAsError = reader.readBoolean("loggedErrorAsError");
    }

    @Override
    public void writeState(AgentWriter writer) throws Exception {
        writer.writeBoolean("active", active);

        writer.writeEnum("slowDurationType", slowDurationType);
        writer.writeInt("slowValue", slowValue);

        writer.writeEnum("verySlowDurationType", verySlowDurationType);
        writer.writeInt("verySlowValue", verySlowValue);

        writer.writeEnum("overdueDurationType", overdueDurationType);
        writer.writeInt("overdueValue", overdueValue);

        writer.writeBoolean("splitTree", splitTree);

        writer.writeBoolean("checkedExceptionAsError", checkedExceptionAsError);
        writer.writeBoolean("runtimeExceptionAsError", runtimeExceptionAsError);
        writer.writeBoolean("errorThrowableAsError", errorThrowableAsError);

        writer.writeBoolean("loggedWarningAsError", loggedWarningAsError);
        writer.writeBoolean("loggedErrorAsError", loggedErrorAsError);
    }

    @Override
    public ProtocolRequirement getProtocolRequirement() {
        return ProtocolRequirement.V1;
    }

    protected String getTransactionTypeSpecificUsedError(Object errorObject) {
        return errorObject.toString();
    }

    public final String getUsedError(Object errorObject) {
        if (errorObject == null) {
            return null;
        } else if (errorObject instanceof Throwable) {
            if (errorObject instanceof RuntimeException) {
                return runtimeExceptionAsError ? errorObject.getClass().getName() : null;
            } else if (errorObject instanceof Error) {
                return errorThrowableAsError ? errorObject.getClass().getName() : null;
            } else {
                return checkedExceptionAsError ? errorObject.getClass().getName() : null;
            }
        }
        return getTransactionTypeSpecificUsedError(errorObject);
    }
}
