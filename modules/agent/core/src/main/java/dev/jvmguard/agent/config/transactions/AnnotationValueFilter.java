package dev.jvmguard.agent.config.transactions;

import dev.jvmguard.agent.comm.*;
import dev.jvmguard.agent.config.base.AbstractEntity;
import dev.jvmguard.agent.config.base.ConfigDoc;
import dev.jvmguard.agent.helper.matcher.PatternMatcher;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Map;
import java.util.Objects;

@ConfigDoc("Filters annotation-based interceptions by the value of an annotation attribute. Only interceptions " +
        "where the attribute is present and its rendered value matches the pattern are created. An empty value " +
        "or \"*\" deactivates the filter and matches all annotations.")
public class AnnotationValueFilter extends AbstractEntity implements AgentSerializable, CodecEntity {

    @ConfigDoc("Name of the annotation attribute to match.")
    private String attributeName = "";
    @ConfigDoc("Wildcard (comma-separated) or regular expression matched against the rendered attribute value. " +
            "An empty value or \"*\" deactivates the filter.")
    private String value = "";
    @ConfigDoc("Whether the value is matched as a wildcard pattern or a regular expression.")
    private ComparisonType comparisonType = ComparisonType.WILDCARD;

    private transient volatile PatternMatcher patternMatcher;

    public AnnotationValueFilter() {
    }

    public AnnotationValueFilter(String attributeName, String value) {
        this.attributeName = attributeName;
        this.value = value;
    }

    public boolean isActive() {
        return !value.isEmpty() && !value.equals("*");
    }

    public boolean matches(Map<String, String> attributes) {
        if (!isActive() || attributeName.isEmpty()) {
            return true;
        }
        String attributeValue = attributes == null ? null : attributes.get(attributeName);
        if (attributeValue == null) {
            return false;
        }
        PatternMatcher matcher = patternMatcher;
        if (matcher == null) {
            matcher = PatternMatcher.create(value, comparisonType, true, false, false);
            patternMatcher = matcher;
        }
        return matcher.matches(attributeValue);
    }

    public AnnotationValueFilter withAttributeName(String newAttributeName) {
        AnnotationValueFilter copy = new AnnotationValueFilter(newAttributeName, value);
        copy.comparisonType = comparisonType;
        return copy;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        String oldValue = this.attributeName;
        this.attributeName = attributeName;
        fireChanged(oldValue, attributeName);
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        String oldValue = this.value;
        this.value = value;
        patternMatcher = null;
        fireChanged(oldValue, value);
    }

    public ComparisonType getComparisonType() {
        return comparisonType;
    }

    public void setComparisonType(ComparisonType comparisonType) {
        ComparisonType oldValue = this.comparisonType;
        this.comparisonType = comparisonType;
        patternMatcher = null;
        fireChanged(oldValue, comparisonType);
    }

    @Override
    public String codecType() {
        return "AnnotationValueFilter";
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
        value = reader.readString("value");
        comparisonType = reader.readEnum("comparisonType", ComparisonType.class);
    }

    @Override
    public void writeState(AgentWriter writer) throws Exception {
        writer.writeString("attributeName", attributeName);
        writer.writeString("value", value);
        writer.writeEnum("comparisonType", comparisonType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AnnotationValueFilter that = (AnnotationValueFilter)o;
        return attributeName.equals(that.attributeName) && value.equals(that.value) && comparisonType == that.comparisonType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributeName, value, comparisonType);
    }

    @Override
    public String toString() {
        return attributeName + "=" + value;
    }
}
