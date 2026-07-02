package com.jvmguard.agent.mbean;

import com.jvmguard.agent.config.base.DefaultConstructor;
import com.jvmguard.mbean.data.MBeanTransfer;
import com.jvmguard.mbean.data.OpenValueTransfer;
import com.jvmguard.agent.comm.CommunicationContext;

import javax.management.MBeanAttributeInfo;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class MBeanSetAttributeParameter extends MBeanParameter {
    private MBeanAttributeInfo attributeInfo;
    private Object value;

    @DefaultConstructor
    public MBeanSetAttributeParameter() {
    }

    public MBeanSetAttributeParameter(String name, MBeanAttributeInfo attributeInfo, Object value) {
        super(name);
        this.attributeInfo = attributeInfo;
        this.value = value;
    }

    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws IOException {
        super.write(context, out);
        MBeanTransfer.writeAttribute(out, attributeInfo, false);
        OpenValueTransfer.write(out, value, null);
    }

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws IOException {
        super.read(context, in);
        attributeInfo = MBeanTransfer.readAttribute(in);
        value = OpenValueTransfer.read(in);
    }

    public MBeanAttributeInfo getAttributeInfo() {
        return attributeInfo;
    }

    public Object getValue() {
        return value;
    }
}
