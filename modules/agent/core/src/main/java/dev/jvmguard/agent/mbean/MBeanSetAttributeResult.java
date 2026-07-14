package dev.jvmguard.agent.mbean;

import dev.jvmguard.agent.comm.CommunicationContext;
import dev.jvmguard.mbean.data.MBeanManager;

import javax.management.MBeanServer;
import javax.management.ObjectName;

public class MBeanSetAttributeResult extends MBeanModificationResult<MBeanSetAttributeParameter> {
    @Override
    protected void execute(CommunicationContext context, MBeanSetAttributeParameter parameter, ObjectName objectName, MBeanServer mbs) throws Exception {
        MBeanManager.setAttribute(objectName, mbs, parameter.getAttributeInfo(), parameter.getValue());
    }
}
