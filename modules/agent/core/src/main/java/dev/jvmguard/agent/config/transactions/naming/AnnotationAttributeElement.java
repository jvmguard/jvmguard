package dev.jvmguard.agent.config.transactions.naming;

import dev.jvmguard.agent.comm.*;
import dev.jvmguard.agent.config.base.ConfigDoc;
import dev.jvmguard.agent.config.base.DefaultConstructor;
import dev.jvmguard.agent.config.transactions.NamingElement;

import java.io.DataInputStream;
import java.io.DataOutputStream;

@ConfigDoc("Adds the value of an annotation attribute, captured at instrumentation time, as a name segment. " +
        "Only useful for annotation-based transaction definitions (mapped and OTel).")
public class AnnotationAttributeElement extends NamingElement {

    @ConfigDoc("Name of the annotation attribute whose value is added as a name segment.")
    private String attributeName = "";

    @DefaultConstructor
    public AnnotationAttributeElement() {
    }

    public AnnotationAttributeElement(String attributeName) {
        this.attributeName = attributeName;
    }

    @Override
    public boolean isIdentical(NamingElement namingElement) {
        if (!super.isIdentical(namingElement)) {
            return false;
        }
        AnnotationAttributeElement other = (AnnotationAttributeElement)namingElement;
        return attributeName.equals(other.attributeName);
    }

    @Override
    public String codecType() {
        return "AnnotationAttributeElement";
    }

    @Override
    public ProtocolRequirement getSinceVersion() {
        return ProtocolRequirement.V2;
    }

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws Exception {
        readState(new BinaryAgentReader(in, context));
    }

    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws Exception {
        writeState(new BinaryAgentWriter(out, context));
    }

    @Override
    public void readState(AgentReader reader) throws Exception {
        attributeName = reader.readString("attributeName");
    }

    @Override
    public void writeState(AgentWriter writer) throws Exception {
        writer.writeString("attributeName", attributeName);
    }

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        String oldValue = this.attributeName;
        this.attributeName = attributeName;
        fireChanged(oldValue, attributeName);
    }

    @Override
    public String getDisplayName() {
        return "Annotation attribute \"" + attributeName + "\"";
    }

    @Override
    public boolean canBeStatic() {
        return true;
    }

    public void appendName(StringBuilder buffer, TransactionEnvironment environment) {
        String value = environment.getAnnotationAttribute(attributeName);
        if (value != null) {
            buffer.append(value);
        }
    }

    public interface TransactionEnvironment {
        /**
         * Returns the value of an annotation attribute captured at instrumentation time, or null if
         * the attribute is not set or no attributes are available (runtime name recalculation).
         */
        String getAnnotationAttribute(String attributeName);
    }
}
