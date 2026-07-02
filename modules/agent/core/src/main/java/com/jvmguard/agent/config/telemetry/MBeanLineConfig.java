package com.jvmguard.agent.config.telemetry;

import com.jvmguard.agent.comm.*;
import com.jvmguard.agent.config.base.AbstractEntity;

import java.io.DataInputStream;
import java.io.DataOutputStream;

public class MBeanLineConfig extends AbstractEntity implements AgentSerializable, CodecEntity {

    private String beanName = "";
    private String attributePath = "";
    private String lineName = "";

    public MBeanLineConfig() {
    }

    public MBeanLineConfig(String beanName, String attributePath, String lineName) {
        this.beanName = beanName;
        this.attributePath = attributePath;
        this.lineName = lineName;
    }

    public String getLineName() {
        return lineName;
    }

    public void setLineName(String lineName) {
        String oldValue = this.lineName;
        this.lineName = lineName;
        fireChanged(oldValue, lineName);
    }

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        String oldValue = this.beanName;
        this.beanName = beanName;
        fireChanged(oldValue, beanName);
    }

    public String getAttributePath() {
        return attributePath;
    }

    public void setAttributePath(String attributePath) {
        String oldValue = this.attributePath;
        this.attributePath = attributePath;
        fireChanged(oldValue, attributePath);
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
        return "MBeanLineConfig";
    }

    @Override
    public void readState(AgentReader reader) throws Exception {
        beanName = reader.readString("beanName");
        attributePath = reader.readString("attributePath");
        lineName = reader.readString("lineName");
    }

    @Override
    public void writeState(AgentWriter writer) throws Exception {
        writer.writeString("beanName", beanName);
        writer.writeString("attributePath", attributePath);
        writer.writeString("lineName", lineName);
    }

    @Override
    public ProtocolRequirement getProtocolRequirement() {
        return ProtocolRequirement.V2;
    }

}
