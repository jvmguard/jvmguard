package dev.jvmguard.agent.mbean;

import dev.jvmguard.agent.config.base.DefaultConstructor;
import dev.jvmguard.mbean.data.MBeanTransfer;
import dev.jvmguard.mbean.data.OpenValueTransfer;
import dev.jvmguard.agent.comm.CommunicationContext;

import javax.management.MBeanOperationInfo;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class MBeanOperationParameter extends MBeanParameter {
    private MBeanOperationInfo operationInfo;
    private Object[] parameters;

    @DefaultConstructor
    public MBeanOperationParameter() {
    }

    public MBeanOperationParameter(String name, MBeanOperationInfo operationInfo, Object[] parameters) {
        super(name);
        this.operationInfo = operationInfo;
        this.parameters = parameters;
    }

    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws IOException {
        super.write(context, out);
        MBeanTransfer.writeOperation(out, operationInfo);
        if (parameters == null) {
            out.writeInt(0);
        } else {
            out.writeInt(parameters.length);
            for (Object parameter : parameters) {
                OpenValueTransfer.write(out, parameter, null);
            }
        }
    }

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws IOException {
        super.read(context, in);
        operationInfo = MBeanTransfer.readOperation(in);
        int length = in.readInt();
        parameters = new Object[length];
        for (int i = 0; i < parameters.length; i++) {
            parameters[i] = OpenValueTransfer.read(in);
        }
    }

    public MBeanOperationInfo getOperationInfo() {
        return operationInfo;
    }

    public Object[] getParameters() {
        return parameters;
    }
}
