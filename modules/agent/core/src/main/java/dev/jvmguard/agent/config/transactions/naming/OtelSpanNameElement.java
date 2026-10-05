package dev.jvmguard.agent.config.transactions.naming;

import dev.jvmguard.agent.comm.*;
import dev.jvmguard.agent.config.base.ConfigDoc;
import dev.jvmguard.agent.config.transactions.NamingElement;
import dev.jvmguard.agent.instrument.transaction.annotation.OtelAnnotationDefinition;
import dev.jvmguard.agent.util.ClassNameFormatter;

import java.io.DataInputStream;
import java.io.DataOutputStream;

@ConfigDoc("Adds the span name of the matched tracing annotation (the \"value\" attribute of @WithSpan or the " +
        "\"contextualName\" attribute of @Observed), falling back to ClassName.methodName when the attribute is " +
        "empty - the same default span name that OpenTelemetry uses.")
public class OtelSpanNameElement extends NamingElement {

    @Override
    public String codecType() {
        return "OtelSpanNameElement";
    }

    @Override
    public ProtocolRequirement getSinceVersion() {
        return ProtocolRequirement.V2;
    }

    @Override
    public String getDisplayName() {
        return "OTel span name";
    }

    @Override
    public boolean canBeStatic() {
        return true;
    }

    public void appendName(StringBuilder buffer, TransactionEnvironment environment) {
        String value = null;
        String spanNameAttribute = OtelAnnotationDefinition.getSpanNameAttribute(environment.getAnnotationDescriptor());
        if (spanNameAttribute != null) {
            value = environment.getAnnotationAttribute(spanNameAttribute);
        }
        if (value == null || value.isEmpty()) {
            ClassNameFormatter.append(buffer, environment.getClassName(), ClassNameFormatter.PackageMode.NONE);
            buffer.append('.').append(environment.getMethodName());
        } else {
            buffer.append(value);
        }
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
    }

    @Override
    public void writeState(AgentWriter writer) throws Exception {
    }

    public interface TransactionEnvironment extends AnnotationAttributeElement.TransactionEnvironment {
        String getAnnotationDescriptor();
        String getClassName();
        String getMethodName();
    }
}
