package com.jvmguard.agent.mbean;

import com.jvmguard.agent.base.logging.Subsystem;
import com.jvmguard.agent.comm.CommunicationContext;
import com.jvmguard.agent.data.BaseResult;
import com.jvmguard.agent.util.Logger;
import com.jvmguard.mbean.data.MBeanManager;
import com.jvmguard.mbean.data.MBeanTransfer;

import javax.management.AttributeList;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class MBeanDataResult extends BaseResult implements MBeanData {
    private MBeanInfo beanInfo;
    private List<Object> values;

    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws IOException {
        AttributeList attributeList = null;
        MBeanDataParameter parameter = (MBeanDataParameter)context.getProperty(CommunicationContext.PROPERTY_PARAMETER);
        try {
            ObjectName objectName = ObjectName.getInstance(parameter.getName());
            MBeanServer mbs = MBeanManager.getMBeanServer(objectName);
            if (mbs != null) {
                beanInfo = mbs.getMBeanInfo(objectName);
                if (beanInfo != null && parameter.isFetchValues()) {
                    attributeList = MBeanManager.getOpenAttributes(objectName, mbs, beanInfo);
                }
            }
        } catch (Throwable e) {
            Logger.log(Subsystem.COMMUNICATION, 0, true, e);
        }
        if (parameter.isFetchStructure()) {
            out.writeBoolean(true);
            MBeanTransfer.writeInfo(out, beanInfo, false);
        } else {
            out.writeBoolean(false);
        }
        if (parameter.isFetchValues()) {
            out.writeBoolean(true);
            MBeanTransfer.writeOpenTypeValues(out, beanInfo, attributeList);
        } else {
            out.writeBoolean(false);
        }
    }

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws IOException {
        if (in.readBoolean()) {
            beanInfo = MBeanTransfer.readBeanInfo(in);
        }
        if (in.readBoolean()) {
            values = MBeanTransfer.readSimpleValues(in);
        }
    }

    @Override
    public MBeanInfo getBeanInfo() {
        return beanInfo;
    }

    @Override
    public List<Object> getValues() {
        return values;
    }

    @Override
    public String toString() {
        return "MBeanDataResult{" +
            "beanInfo=" + beanInfo +
            ", values=" + values +
            '}';
    }
}
