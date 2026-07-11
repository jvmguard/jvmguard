package com.jvmguard.agent.config.transactions.naming;

import com.jvmguard.agent.comm.*;
import com.jvmguard.agent.config.base.ConfigDoc;
import com.jvmguard.agent.config.base.DefaultConstructor;
import com.jvmguard.agent.config.transactions.NamingElement;

import java.io.DataInputStream;
import java.io.DataOutputStream;

@ConfigDoc("Adds a literal text segment to the transaction name.")
public class TextElement extends NamingElement {

    @ConfigDoc("A literal text segment inserted verbatim into the transaction name.")
    private String text = "";

    public TextElement(String text) {
        this.text = text;
    }

    @DefaultConstructor
    public TextElement() {
    }

    @Override
    public boolean isIdentical(NamingElement namingElement) {
        if (!super.isIdentical(namingElement)) {
            return false;
        }
        TextElement other = (TextElement)namingElement;
        return text.equals(other.text);
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
        return "TextElement";
    }

    @Override
    public void readState(AgentReader reader) throws Exception {
        text = reader.readString("text");
    }

    @Override
    public void writeState(AgentWriter writer) throws Exception {
        writer.writeString("text", text);
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        String oldValue = this.text;
        this.text = text;
        fireChanged(oldValue, text);
    }

    @Override
    public String getDisplayName() {
        return "Text \"" + text + "\"";
    }

    @Override
    public boolean canBeStatic() {
        return true;
    }

    public void appendName(StringBuilder buffer) {
        buffer.append(text);
    }
}
