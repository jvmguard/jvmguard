package dev.jvmguard.agent.config.transactions;

import dev.jvmguard.agent.comm.*;
import dev.jvmguard.agent.config.base.ConfigDoc;
import dev.jvmguard.agent.config.base.EntityChangeListener;
import dev.jvmguard.agent.instrument.transaction.annotation.AnnotationDefinition;

public abstract class AnnotatedTransactionDef extends ClassFilterTransactionDef {

    @ConfigDoc("Optional filter on an annotation attribute value for mapped and OTel transaction " +
            "definitions: only annotations where the attribute is present and its value matches the " +
            "pattern create transactions. Null when inactive.")
    private AnnotationValueFilter annotationValueFilter;

    public abstract AnnotationDefinition[] getAnnotationDefinitions();

    @Override
    public String getAutomaticName() {
        return describeFilters();
    }

    protected String describeFilters() {
        String className = getClassName();
        boolean classFilterSet = !className.isEmpty() && !className.equals("*");
        AnnotationValueFilter filter = getAnnotationValueFilter();
        boolean valueFilterSet = filter != null && filter.isActive();
        if (!classFilterSet && !valueFilterSet) {
            return "*";
        }
        StringBuilder result = new StringBuilder();
        if (classFilterSet) {
            result.append("Classes: ").append(className);
        }
        if (valueFilterSet) {
            if (result.length() > 0) {
                result.append(", ");
            }
            result.append(getValueFilterLabel(filter)).append(": ").append(filter.getValue());
        }
        return result.toString();
    }

    protected String getValueFilterLabel(AnnotationValueFilter filter) {
        return filter.getAttributeName();
    }

    public AnnotationValueFilter getAnnotationValueFilter() {
        return annotationValueFilter;
    }

    public void setAnnotationValueFilter(AnnotationValueFilter annotationValueFilter) {
        AnnotationValueFilter oldValue = this.annotationValueFilter;
        this.annotationValueFilter = annotationValueFilter;
        fireChanged(oldValue, annotationValueFilter);
    }

    @Override
    public void addChangeListener(EntityChangeListener listener) {
        super.addChangeListener(listener);
        if (annotationValueFilter != null) {
            annotationValueFilter.addChangeListener(listener);
        }
    }

    @Override
    public void removeChangeListener(EntityChangeListener listener) {
        super.removeChangeListener(listener);
        if (annotationValueFilter != null) {
            annotationValueFilter.removeChangeListener(listener);
        }
    }

    @Override
    public void readState(AgentReader reader) throws Exception {
        super.readState(reader);
        if (reader.satisfies(ProtocolRequirement.V2)) {
            annotationValueFilter = reader.readObject("annotationValueFilter");
        }
    }

    @Override
    public void writeState(AgentWriter writer) throws Exception {
        super.writeState(writer);
        if (writer.satisfies(ProtocolRequirement.V2)) {
            writer.writeObject("annotationValueFilter", annotationValueFilter);
        }
    }
}
