package com.jvmguard.agent.config.transactions;

import com.jvmguard.agent.comm.*;
import com.jvmguard.agent.config.base.ConfigDoc;
import com.jvmguard.agent.config.transactions.naming.ClassNameElement;
import com.jvmguard.agent.config.transactions.naming.InstanceElement;
import com.jvmguard.agent.config.transactions.naming.MethodParameterElement;
import com.jvmguard.agent.instrument.NameTransformation;
import com.jvmguard.agent.instrument.transaction.DefinitionSite;

import java.io.DataInputStream;
import java.io.DataOutputStream;

public abstract class ClassFilterTransactionDef extends WildcardTransactionDef {

    @ConfigDoc("Class or interface name filter selecting which classes this definition applies to. It is " +
            "matched as a wildcard or regular expression per comparisonType, and a wildcard may be a " +
            "comma-separated list.")
    private String className = "*";

    @Override
    public void initDefault() {
        getNaming().getNamingElements().add(new ClassNameElement());
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
        super.readState(reader);
        className = reader.readString("className");
    }

    @Override
    public void writeState(AgentWriter writer) throws Exception {
        super.writeState(writer);
        writer.writeString("className", className);
    }

    @Override
    protected String getFilter() {
        return className;
        // could be used if classes are passed with slashes
/*
        if (getComparisonType() == ComparisonType.WILDCARD) {
            return className.replace('.', '/');
        } else {
            return className.replaceAll("\\\\\\.", "\\\\\\/");
        }
*/
    }

    @Override
    protected boolean isWildcardCommaSeparated() {
        return true;
    }

    public boolean matches(DefinitionSite definitionSite) {
        return matches(definitionSite.getDefinedFor());
    }

    @Override
    public String getAutomaticName() {
        return className;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        String oldValue = this.className;
        this.className = className;
        fireChanged(oldValue, className);
    }

    public static void classInterceptionAppendTransactionName(NamingElement namingElement, ClassInterceptionTransactionEnvironment environment, StringBuilder buffer) throws EnvironmentException {
        if (namingElement instanceof ClassNameElement) {
            ((ClassNameElement)namingElement).appendName(buffer, environment);
        } else if (namingElement instanceof MethodParameterElement) {
            ((MethodParameterElement)namingElement).appendName(buffer, environment);
        } else if (namingElement instanceof InstanceElement) {
            ((InstanceElement)namingElement).appendName(buffer, environment);
        } else {
            commonAppendTransactionName(namingElement, buffer);
        }
    }

    public static class ClassInterceptionTransactionEnvironment implements
        InstanceElement.TransactionEnvironment,
        MethodParameterElement.TransactionEnvironment {

        private String className;
        private Object[] parameterObjects;
        private Object instance;

        public ClassInterceptionTransactionEnvironment(String className, Object instance, Object[] parameterObjects) {
            this.className = className;
            this.instance = instance;
            this.parameterObjects = parameterObjects;
        }

        @Override
        public String getClassName() {
            return className;
        }

        @Override
        public Object[] getParameterObjects() {
            return parameterObjects;
        }

        @Override
        public String getInstanceClassName() {
            return instance != null ? NameTransformation.transformClass(instance.getClass().getName()) : className;
        }

        @Override
        public Object getInstance() {
            return instance;
        }

        @Override
        public String toString() {
            return className;
        }
    }

}
