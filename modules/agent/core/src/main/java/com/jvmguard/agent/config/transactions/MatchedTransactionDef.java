package com.jvmguard.agent.config.transactions;

import com.jvmguard.agent.comm.*;
import com.jvmguard.agent.instrument.transaction.matched.MatchedDefinition;

import java.io.DataInputStream;
import java.io.DataOutputStream;

public class MatchedTransactionDef extends MethodInterceptionTransactionDef {

    public static String getDefaultName(String className, String methodName) {
        return className + "#" + methodName;
    }

    private String declaringClassName = "";

    private InterceptionTarget interceptionTarget = InterceptionTarget.CLASS;
    private boolean interceptSubclasses = false;
    private boolean staticMethods = false;
    private MethodInterceptionMode methodInterceptionMode = MethodInterceptionMode.IMPLEMENTING_PUBLIC;

    private String methodName = "";
    private String methodSignature = "";

    public MatchedTransactionDef() {
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
        return "MatchedTransactionDef";
    }

    @Override
    public void readState(AgentReader reader) throws Exception {
        super.readState(reader);
        declaringClassName = reader.readString("declaringClassName");
        interceptionTarget = reader.readEnum("interceptionTarget", InterceptionTarget.class);
        interceptSubclasses = reader.readBoolean("interceptSubclasses");
        staticMethods = reader.readBoolean("staticMethods");
        methodInterceptionMode = reader.readEnum("methodInterceptionMode", MethodInterceptionMode.class);
        methodName = reader.readString("methodName");
        methodSignature = reader.readString("methodSignature");
    }

    @Override
    public void writeState(AgentWriter writer) throws Exception {
        super.writeState(writer);
        writer.writeString("declaringClassName", declaringClassName);
        writer.writeEnum("interceptionTarget", interceptionTarget);
        writer.writeBoolean("interceptSubclasses", interceptSubclasses);
        writer.writeBoolean("staticMethods", staticMethods);
        writer.writeEnum("methodInterceptionMode", methodInterceptionMode);
        writer.writeString("methodName", methodName);
        writer.writeString("methodSignature", methodSignature);
    }

    @Override
    public TransactionType getTransactionType() {
        return TransactionType.MATCHED;
    }

    @Override
    public String getAutomaticName() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(getDefaultName(declaringClassName, interceptionTarget == InterceptionTarget.METHOD ? methodName : "*"));
        if (interceptSubclasses) {
            buffer.append(" [subclasses: ");
            buffer.append(getClassName());
            buffer.append("]");
        }
        return buffer.toString();
    }

    public String getDeclaringClassName() {
        return declaringClassName;
    }

    public void setDeclaringClassName(String declaringClassName) {
        String oldValue = this.declaringClassName;
        this.declaringClassName = declaringClassName;
        fireChanged(oldValue, declaringClassName);
    }

    public InterceptionTarget getInterceptionTarget() {
        return interceptionTarget;
    }

    public void setInterceptionTarget(InterceptionTarget interceptionTarget) {
        InterceptionTarget oldValue = this.interceptionTarget;
        this.interceptionTarget = interceptionTarget;
        fireChanged(oldValue, interceptionTarget);
    }

    public boolean isInterceptSubclasses() {
        return interceptSubclasses;
    }

    public void setInterceptSubclasses(boolean interceptSubclasses) {
        boolean oldValue = this.interceptSubclasses;
        this.interceptSubclasses = interceptSubclasses;
        fireChanged(oldValue, interceptSubclasses);
    }

    public MethodInterceptionMode getMethodInterceptionMode() {
        return methodInterceptionMode;
    }

    public void setMethodInterceptionMode(MethodInterceptionMode methodInterceptionMode) {
        MethodInterceptionMode oldValue = this.methodInterceptionMode;
        this.methodInterceptionMode = methodInterceptionMode;
        fireChanged(oldValue, methodInterceptionMode);
    }

    public String getMethodSignature() {
        return methodSignature;
    }

    public void setMethodSignature(String methodSignature) {
        String oldSignature = this.methodSignature;
        this.methodSignature = methodSignature;
        fireChanged(oldSignature, methodSignature);
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        String oldName = this.methodName;
        this.methodName = methodName;
        fireChanged(oldName, methodName);
    }

    public boolean isStaticMethods() {
        return staticMethods;
    }

    public void setStaticMethods(boolean staticMethods) {
        boolean oldValue = this.staticMethods;
        this.staticMethods = staticMethods;
        fireChanged(oldValue, staticMethods);
    }

    public MatchedDefinition createMatchedDefinition() {
        boolean singleMethod = interceptionTarget == InterceptionTarget.METHOD;
        return new MatchedDefinition(declaringClassName, interceptSubclasses, singleMethod || methodInterceptionMode == MethodInterceptionMode.IMPLEMENTING_PUBLIC, singleMethod || staticMethods, singleMethod ? methodName : null, singleMethod ? methodSignature : null);
    }

    public enum InterceptionTarget {
        CLASS("Class or interface"),
        METHOD("Single method of a class or interface");

        private final String verbose;

        InterceptionTarget(String verbose) {
            this.verbose = verbose;
        }

        @Override
        public String toString() {
            return verbose;
        }
    }

    public enum MethodInterceptionMode {
        IMPLEMENTING_PUBLIC("Implementing or overriding public methods"),
        ALL_PUBLIC("All public methods of implementing or derived classes");

        private final String verbose;

        MethodInterceptionMode(String verbose) {
            this.verbose = verbose;
        }

        @Override
        public String toString() {
            return verbose;
        }
    }
}
