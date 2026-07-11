package com.jvmguard.agent.config.transactions;

import com.jvmguard.agent.comm.*;
import com.jvmguard.agent.config.base.ConfigDoc;
import com.jvmguard.agent.instrument.transaction.DefinitionSite;
import com.jvmguard.agent.instrument.transaction.annotation.AnnotationDefinition;
import com.jvmguard.agent.instrument.transaction.annotation.MappedAnnotationDefinition;

import java.io.DataInputStream;
import java.io.DataOutputStream;

@ConfigDoc("Marks classes/methods as transactions by the presence of a given annotation type.")
public class MappedTransactionDef extends AnnotatedTransactionDef {

    @ConfigDoc("Fully-qualified annotation type name whose presence marks classes/methods as transactions.")
    private String annotationName = "";
    @ConfigDoc("Whether the annotation is expected on classes or on methods.")
    private AnnotatedTarget annotatedTarget = AnnotatedTarget.CLASS;

    @ConfigDoc("If true, the annotation is inherited by subclasses/implementations.")
    private boolean interceptSubclasses = false;

    @ConfigDoc("For class-level annotations, which methods to instrument (implementing/overriding public, or " +
            "all public).")
    private MethodInterceptionMode methodInterceptionMode = MethodInterceptionMode.ALL_PUBLIC;
    @ConfigDoc("If true, match against the declaring class rather than the concrete/runtime class.")
    private boolean useDeclaringClassName = false;

    @Override
    public String getAutomaticName() {
        return annotationName + " [" + super.getAutomaticName() + "]";
    }

    public String getAnnotationName() {
        return annotationName;
    }

    public void setAnnotationName(String annotationName) {
        String oldValue = this.annotationName;
        this.annotationName = annotationName;
        fireChanged(oldValue, annotationName);
    }

    public AnnotatedTarget getAnnotatedTarget() {
        return annotatedTarget;
    }

    public void setAnnotatedTarget(AnnotatedTarget annotatedTarget) {
        AnnotatedTarget oldValue = this.annotatedTarget;
        this.annotatedTarget = annotatedTarget;
        fireChanged(oldValue, annotatedTarget);
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

    public boolean isUseDeclaringClassName() {
        return useDeclaringClassName;
    }

    public void setUseDeclaringClassName(boolean useDeclaringClassName) {
        boolean oldValue = this.useDeclaringClassName;
        this.useDeclaringClassName = useDeclaringClassName;
        fireChanged(oldValue, useDeclaringClassName);
    }

    @Override
    public boolean matches(DefinitionSite definitionSite) {
        return matches(useDeclaringClassName ? definitionSite.getDefinedBy() : definitionSite.getDefinedFor());
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
        return "MappedTransactionDef";
    }

    @Override
    public void readState(AgentReader reader) throws Exception {
        super.readState(reader);
        annotationName = reader.readString("annotationName");
        annotatedTarget = reader.readEnum("annotatedTarget", AnnotatedTarget.class);
        methodInterceptionMode = reader.readEnum("methodInterceptionMode", MethodInterceptionMode.class);
        useDeclaringClassName = reader.readBoolean("useDeclaringClassName");
        interceptSubclasses = reader.readBoolean("interceptSubclasses");
    }

    @Override
    public void writeState(AgentWriter writer) throws Exception {
        super.writeState(writer);
        writer.writeString("annotationName", annotationName);
        writer.writeEnum("annotatedTarget", annotatedTarget);
        writer.writeEnum("methodInterceptionMode", methodInterceptionMode);
        writer.writeBoolean("useDeclaringClassName", useDeclaringClassName);
        writer.writeBoolean("interceptSubclasses", interceptSubclasses);
    }

    @Override
    public TransactionType getTransactionType() {
        return TransactionType.MAPPED;
    }

    @Override
    public AnnotationDefinition[] getAnnotationDefinitions() {
        boolean methodAnnotation = annotatedTarget == AnnotatedTarget.METHOD;
        return new AnnotationDefinition[] {
            new MappedAnnotationDefinition(getAnnotationName(), methodAnnotation, methodAnnotation, getTransactionType())
                .inheritable(interceptSubclasses)
                .implementingOnly(methodAnnotation || methodInterceptionMode == MethodInterceptionMode.IMPLEMENTING_PUBLIC)
                .useDeclaringClassName(useDeclaringClassName)
        };
    }

    public enum AnnotatedTarget {
        @ConfigDoc("The annotation is on classes.")
        CLASS("Annotated classes"),
        @ConfigDoc("The annotation is on methods.")
        METHOD("Annotated methods");

        private final String verbose;

        AnnotatedTarget(String verbose) {
            this.verbose = verbose;
        }

        @Override
        public String toString() {
            return verbose;
        }
    }

    public enum MethodInterceptionMode {
        @ConfigDoc("Only implementing/overriding public methods.")
        IMPLEMENTING_PUBLIC("Implementing or overriding public methods"),
        @ConfigDoc("All public methods of implementing/derived classes.")
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
