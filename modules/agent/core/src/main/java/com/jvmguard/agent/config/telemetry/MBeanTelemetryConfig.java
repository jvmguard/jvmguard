package com.jvmguard.agent.config.telemetry;

import com.jvmguard.agent.comm.*;
import com.jvmguard.agent.config.base.AbstractEntity;
import com.jvmguard.agent.config.base.ConfigDoc;
import com.jvmguard.agent.config.base.DefaultConstructor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;

public class MBeanTelemetryConfig extends AbstractEntity implements AgentSerializable, CodecEntity {

    @ConfigDoc("Display name of this telemetry chart.")
    private String name = "";

    @ConfigDoc("Measurement unit of the values (drives axis scaling and labels).")
    private TelemetryUnit unit = TelemetryUnit.PLAIN;
    @ConfigDoc("Power-of-ten scale factor applied to raw MBean values before display.")
    private int scale = 0;
    @ConfigDoc("If true, values are averaged across the VM group. If false, they are plotted per VM.")
    private boolean groupAveraged = true;
    @ConfigDoc("If true, the chart lines are drawn stacked.")
    private boolean stacked = false;

    @ConfigDoc("The individual MBean-attribute series that make up this chart.")
    private List<MBeanLineConfig> lines = new ArrayList<>();

    public MBeanTelemetryConfig(String name, TelemetryUnit unit, int scale, boolean groupAveraged, boolean stacked) {
        this.name = name;
        this.unit = unit;
        this.scale = scale;
        this.groupAveraged = groupAveraged;
        this.stacked = stacked;
    }

    @DefaultConstructor
    public MBeanTelemetryConfig() {
    }

    public TelemetryUnit getUnit() {
        return unit;
    }

    public void setUnit(TelemetryUnit unit) {
        TelemetryUnit oldValue = this.unit;
        this.unit = unit;
        fireChanged(oldValue, unit);
    }

    public int getScale() {
        return scale;
    }

    public void setScale(int scale) {
        int oldValue = this.scale;
        this.scale = scale;
        fireChanged(oldValue, scale);
    }

    public boolean isGroupAveraged() {
        return groupAveraged;
    }

    public void setGroupAveraged(boolean groupAveraged) {
        boolean oldValue = this.groupAveraged;
        this.groupAveraged = groupAveraged;
        fireChanged(oldValue, groupAveraged);
    }

    public boolean isStacked() {
        return stacked;
    }

    public void setStacked(boolean stacked) {
        boolean oldValue = this.stacked;
        this.stacked = stacked;
        fireChanged(oldValue, stacked);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        String oldValue = this.name;
        this.name = name;
        fireChanged(oldValue, name);
    }

    public List<MBeanLineConfig> getLines() {
        return lines;
    }

    public void setLines(List<MBeanLineConfig> lines) {
        List<MBeanLineConfig> oldValue = this.lines;
        this.lines = lines;
        fireChanged(oldValue, lines);
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
        return "MBeanTelemetryConfig";
    }

    @Override
    public void readState(AgentReader reader) throws Exception {
        name = reader.readString("name");
        unit = reader.readEnum("unit", TelemetryUnit.class);
        scale = reader.readInt("scale");
        groupAveraged = reader.readBoolean("groupAveraged");
        stacked = reader.readBoolean("stacked");
        reader.readList("lines", lines);
    }

    @Override
    public void writeState(AgentWriter writer) throws Exception {
        writer.writeString("name", name);
        writer.writeEnum("unit", unit);
        writer.writeInt("scale", scale);
        writer.writeBoolean("groupAveraged", groupAveraged);
        writer.writeBoolean("stacked", stacked);
        writer.writeList("lines", lines);
    }

    @Override
    public ProtocolRequirement getProtocolRequirement() {
        return ProtocolRequirement.V2;
    }

}
