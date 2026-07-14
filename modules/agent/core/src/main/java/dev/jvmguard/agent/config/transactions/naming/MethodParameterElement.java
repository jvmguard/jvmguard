package dev.jvmguard.agent.config.transactions.naming;

import dev.jvmguard.agent.comm.*;
import dev.jvmguard.agent.config.base.ConfigDoc;
import dev.jvmguard.agent.config.base.DefaultConstructor;
import dev.jvmguard.agent.config.transactions.EnvironmentException;
import dev.jvmguard.agent.config.transactions.NamingElement;

import java.io.DataInputStream;
import java.io.DataOutputStream;

@ConfigDoc("Adds a value obtained by a getter/field chain on a selected method parameter.")
public class MethodParameterElement extends AbstractGetterElement {

    @ConfigDoc("Zero-based index of the method parameter whose value contributes the name segment.")
    private int parameterIndex = 0;

    @DefaultConstructor
    public MethodParameterElement() {
    }

    public MethodParameterElement(int parameterIndex, String getterChain) {
        super(getterChain);
        this.parameterIndex = parameterIndex;
    }

    @Override
    public boolean isIdentical(NamingElement namingElement) {
        if (!super.isIdentical(namingElement)) {
            return false;
        }
        MethodParameterElement other = (MethodParameterElement)namingElement;
        return parameterIndex == other.parameterIndex;
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
        return "MethodParameterElement";
    }

    @Override
    public void readState(AgentReader reader) throws Exception {
        super.readState(reader);
        parameterIndex = reader.readInt("parameterIndex");
    }

    @Override
    public void writeState(AgentWriter writer) throws Exception {
        super.writeState(writer);
        writer.writeInt("parameterIndex", parameterIndex);
    }

    public int getParameterIndex() {
        return parameterIndex;
    }

    public void setParameterIndex(int parameterIndex) {
        int oldValue = this.parameterIndex;
        this.parameterIndex = parameterIndex;
        fireChanged(oldValue, parameterIndex);
    }

    @Override
    public String getDisplayName() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("Method parameter ");
        buffer.append(parameterIndex);
        appendGetterChain(buffer);
        return buffer.toString();
    }

    public void appendName(StringBuilder buffer, TransactionEnvironment environment) throws EnvironmentException {
        Object[] parameterObjects = environment.getParameterObjects();
        if (parameterObjects != null) {
            if (parameterObjects.length <= parameterIndex) {
                throw new EnvironmentException("Method has " + parameterObjects.length + " parameters, parameter index " + parameterIndex + " does not exist");
            }
            appendName(buffer, parameterObjects[parameterIndex]);
        }
    }

    public interface TransactionEnvironment {
        Object[] getParameterObjects();
    }

}
