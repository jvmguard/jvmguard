package dev.jvmguard.agent.config.transactions;

import dev.jvmguard.agent.comm.*;
import dev.jvmguard.agent.config.base.ConfigDoc;
import dev.jvmguard.agent.config.transactions.naming.OtelSpanNameElement;
import dev.jvmguard.agent.instrument.transaction.annotation.AnnotationDefinition;
import dev.jvmguard.agent.instrument.transaction.annotation.OtelAnnotationDefinition;

import java.io.DataInputStream;
import java.io.DataOutputStream;

@ConfigDoc("Maps the OpenTelemetry @WithSpan and Micrometer @Observed annotations to transactions. " +
        "@WithSpan is intercepted on methods, @Observed on methods and on classes (all public methods). " +
        "The default naming uses the span name attribute of the annotation (\"value\" for @WithSpan, " +
        "\"contextualName\" for @Observed), falling back to ClassName.methodName. The annotation value " +
        "filter matches against the span name.")
public class OtelTransactionDef extends AnnotatedTransactionDef {

    @Override
    public void initDefault() {
        getNaming().getNamingElements().add(new OtelSpanNameElement());
    }

    @Override
    public TransactionType getTransactionType() {
        return TransactionType.OTEL;
    }

    @Override
    protected String getValueFilterLabel(AnnotationValueFilter filter) {
        // the attribute is implicit: the span name of the matched annotation
        return "Span name";
    }

    @Override
    public String codecType() {
        return "OtelTransactionDef";
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
    public AnnotationDefinition[] getAnnotationDefinitions() {
        AnnotationValueFilter filter = getAnnotationValueFilter();
        boolean filterActive = filter != null && filter.isActive();
        return new AnnotationDefinition[] {
            withFilter(new OtelAnnotationDefinition(OtelAnnotationDefinition.Kind.WITH_SPAN, false, getTransactionType()), filter, filterActive),
            withFilter(new OtelAnnotationDefinition(OtelAnnotationDefinition.Kind.OBSERVED, false, getTransactionType()), filter, filterActive),
            withFilter(new OtelAnnotationDefinition(OtelAnnotationDefinition.Kind.OBSERVED, true, getTransactionType()), filter, filterActive)
        };
    }

    private static OtelAnnotationDefinition withFilter(OtelAnnotationDefinition definition, AnnotationValueFilter filter, boolean filterActive) {
        if (filterActive) {
            definition.valueFilter(filter.withAttributeName(definition.getSpanNameAttribute()));
        }
        return definition;
    }
}
