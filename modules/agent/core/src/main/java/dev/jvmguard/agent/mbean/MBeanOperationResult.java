package dev.jvmguard.agent.mbean;

import dev.jvmguard.agent.comm.CommunicationContext;
import dev.jvmguard.mbean.data.MBeanManager;
import dev.jvmguard.mbean.data.OpenValueTransfer;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class MBeanOperationResult extends MBeanModificationResult<MBeanOperationParameter> implements MBeanOperationData {

    private Object returnValue;

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws IOException {
        super.read(context, in);
        returnValue = OpenValueTransfer.read(in);
    }

    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws IOException {
        super.write(context, out);
        OpenValueTransfer.write(out, returnValue, null);
    }

    @Override
    protected void execute(CommunicationContext context, MBeanOperationParameter parameter, ObjectName objectName, MBeanServer mbs) throws Exception {
        returnValue = MBeanManager.invokeOperation(objectName, mbs, parameter.getOperationInfo(), parameter.getParameters());
    }

    @Override
    public Object getReturnValue() {
        return returnValue;
    }
}
