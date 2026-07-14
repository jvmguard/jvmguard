package dev.jvmguard.agent.mbean;

import dev.jvmguard.mbean.data.MBeanManager;
import dev.jvmguard.mbean.data.BaseValueTransfer;
import dev.jvmguard.agent.util.Util;
import dev.jvmguard.agent.base.logging.Subsystem;
import dev.jvmguard.agent.comm.CommunicationContext;
import dev.jvmguard.agent.data.BaseResult;
import dev.jvmguard.agent.util.Logger;

import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.RuntimeMBeanException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class MBeanModificationResult<T extends MBeanParameter> extends BaseResult implements MBeanModificationData {
    private String errorMessage;
    private String stackTrace;

    protected abstract void execute(CommunicationContext context, T parameter, ObjectName objectName, MBeanServer mbs) throws Exception;

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String getStackTrace() {
        return stackTrace;
    }

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws IOException {
        errorMessage = BaseValueTransfer.readOptionalLongString(in);
        stackTrace = BaseValueTransfer.readOptionalLongString(in);
    }

    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws IOException {
        BaseValueTransfer.writeOptionalLongString(out, errorMessage);
        BaseValueTransfer.writeOptionalLongString(out, stackTrace);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void prepareDeferredLater(CommunicationContext context) {
        T parameter = (T)context.getProperty(CommunicationContext.PROPERTY_PARAMETER);
        try {
            ObjectName objectName = ObjectName.getInstance(parameter.getName());
            MBeanServer mbs = MBeanManager.getMBeanServer(objectName);
            if (mbs != null) {
                execute(context, parameter, objectName, mbs);
            } else {
                errorMessage = "MBean not found";
            }
        } catch (MBeanException | RuntimeMBeanException e) {
            handleMBeanException(e);
        } catch (Throwable e) {
            handleThrowable(e);
        }
    }

    private void handleMBeanException(Exception e) {
        if (e.getCause() != null) {
            handleThrowable(e.getCause());
        } else {
            handleThrowable(e);
        }
    }

    private void handleThrowable(Throwable e) {
        Logger.log(Subsystem.MBEAN, 1, true, e);
        errorMessage = (e.getMessage() != null && !e.getMessage().isEmpty()) ? e.getMessage() : e.getClass().getName();
        stackTrace = Util.buildMessage(null, e);
    }


}
