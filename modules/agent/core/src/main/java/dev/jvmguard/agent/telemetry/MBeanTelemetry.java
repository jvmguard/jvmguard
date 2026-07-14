package dev.jvmguard.agent.telemetry;

import dev.jvmguard.agent.AgentConstants;
import dev.jvmguard.agent.base.logging.Subsystem;
import dev.jvmguard.agent.config.telemetry.MBeanLineConfig;
import dev.jvmguard.agent.util.Logger;
import dev.jvmguard.mbean.common.MBeanHelper;
import dev.jvmguard.mbean.common.MBeanHelper.FindAttributeException;
import dev.jvmguard.mbean.data.MBeanManager;

import javax.management.*;
import java.util.Arrays;

class MBeanTelemetry extends CustomTelemetry {
    private ObjectName objectName;
    private String[] attributePath;

    public MBeanTelemetry(String telemetryName, MBeanLineConfig lineConfig) throws MalformedObjectNameException, IllegalArgumentException {
        super(telemetryName, lineConfig.getLineName());
        if (lineConfig.getBeanName().isEmpty()) {
            throw new IllegalArgumentException("mbean name empty");
        }
        if (lineConfig.getAttributePath().isEmpty()) {
            throw new IllegalArgumentException("mbean attribute path empty for " + lineConfig.getBeanName());
        }
        objectName = ObjectName.getInstance(lineConfig.getBeanName());
        attributePath = MBeanHelper.splitEscaped(lineConfig.getAttributePath());
    }

    @Override
    public String toString() {
        return "MBeanTelemetry{" +
            "name=" + getName() +
            ", objectName=" + objectName +
            ", attributePath=" + Arrays.toString(attributePath) +
            '}';
    }

    @Override
    public long getCurrentValue() throws IgnoreTelemetryException {
        MBeanServer server = MBeanManager.getMBeanServer(objectName);
        if (server == null) {
            Logger.log(Subsystem.MBEAN, 5, true, "%s not found\n", objectName);
            throw new IgnoreTelemetryException();
        } else if (attributePath == null || attributePath.length == 0) {
            Logger.log(Subsystem.MBEAN, 2, true, "%s: no attributes configured\n", objectName);
            throw new IgnoreTelemetryException();
        } else {
            Object value;
            try {
                value = server.getAttribute(objectName, attributePath[0]);
            } catch (MBeanException | ReflectionException | InstanceNotFoundException | AttributeNotFoundException e) {
                Logger.log(Subsystem.MBEAN, 1, true, "%s: %s\n", objectName, e);
                throw new IgnoreTelemetryException();
            }
            for (int i = 1; value != null && i < attributePath.length; i++) {
                try {
                    value = MBeanHelper.findAttribute(value, attributePath[i]);
                } catch (FindAttributeException e) {
                    Logger.log(Subsystem.MBEAN, 2, true, "%s: %s\n", this, e.getMessage());
                }
            }
            return convertNumber(value);
        }
    }

    @Override
    protected int getType() {
        return AgentConstants.TELEMETRY_TYPE_MBEAN;
    }

    @Override
    protected Subsystem getSubsystem() {
        return Subsystem.MBEAN;
    }


}
