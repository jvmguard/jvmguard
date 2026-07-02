package com.jvmguard.agent.telemetry;

import com.jvmguard.agent.AgentConstants;
import com.jvmguard.agent.JvmGuardAgent;
import com.jvmguard.agent.base.logging.Subsystem;
import com.jvmguard.agent.base.telemetry.AdditionalTelemetryProvider;
import com.jvmguard.agent.base.telemetry.JMXTelemetryProvider;
import com.jvmguard.agent.base.telemetry.TelemetryDescription;
import com.jvmguard.agent.comm.CommunicationContext;
import com.jvmguard.agent.comm.ProtocolRequirement;
import com.jvmguard.agent.config.telemetry.MBeanLineConfig;
import com.jvmguard.agent.config.telemetry.MBeanTelemetryConfig;
import com.jvmguard.agent.config.telemetry.TelemetrySettings;
import com.jvmguard.agent.util.Logger;
import com.jvmguard.annotation.Telemetry;

import javax.annotation.concurrent.GuardedBy;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;
import java.util.*;

public class TelemetryCollector extends Thread {
    private static volatile TelemetryCollector instance;

    private final Map<String, AnnotationTelemetry> annotationTelemetries = Collections.synchronizedMap(new HashMap<>());
    private volatile Set<MBeanTelemetry> mbeanTelemetries = new HashSet<>();

    @GuardedBy("this")
    private AveragingData cpuData = new AveragingData(2);
    @GuardedBy("this")
    private long[] lastJmxData;
    @GuardedBy("this")
    private final JMXTelemetryProvider jmxTelemetryProvider;

    private ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    private volatile boolean terminateRecording;

    private TelemetryCollector() {
        super(JvmGuardAgent.AGENT_THREAD_GROUP, "_jvmguard_telemetry");
        setDaemon(true);
        jmxTelemetryProvider = new JMXTelemetryProvider();
        jmxTelemetryProvider.getDescriptions();
    }

    public static synchronized TelemetryCollector getInstance() {
        if (instance == null) {
            instance = new TelemetryCollector();
            instance.start();
        }
        return instance;
    }

    @Override
    public void run() {
        for (; ; ) {
            if (terminateRecording) {
                return;
            }

            synchronized (this) {
                cpuData.currentValues[1] = 0;
                lastJmxData = jmxTelemetryProvider.getData(false);
                for (int i = 0; i < lastJmxData.length; i++) {
                    TelemetryDescription description = jmxTelemetryProvider.getDescriptions().get(i);
                    int type = description.getType();
                    if (type == AgentConstants.TELEMETRY_TYPE_CPU_USAGE) {
                        cpuData.currentValues[0] = lastJmxData[i];
                    } else if (type == AgentConstants.TELEMETRY_TYPE_GC_ACTIVITY) {
                        cpuData.currentValues[1] += lastJmxData[i];
                    }
                }
                if (cpuData.currentValues[1] > AdditionalTelemetryProvider.PERCENT_100_VALUE) {
                    cpuData.currentValues[1] = AdditionalTelemetryProvider.PERCENT_100_VALUE;
                }
                cpuData.addAverage();
            }

            try {
                //noinspection BusyWait
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void write(CommunicationContext context, DataOutputStream out) throws IOException {
        synchronized (this) {
            if (lastJmxData != null) {
                for (int i = 0; i < lastJmxData.length; i++) {
                    TelemetryDescription description = jmxTelemetryProvider.getDescriptions().get(i);
                    int type = description.getType();
                    if (type != AgentConstants.TELEMETRY_TYPE_CPU_USAGE && type != AgentConstants.TELEMETRY_TYPE_GC_ACTIVITY) {
                        out.writeBoolean(true);
                        out.writeInt(description.getType());
                        out.writeUTF(description.getName());
                        out.writeLong(lastJmxData[i]);
                        if (context.satisfies(ProtocolRequirement.V2)) {
                            out.writeBoolean(false); // format
                        }
                    }
                }
            }
        }
        if (!annotationTelemetries.isEmpty()) {
            for (Object valueObject : annotationTelemetries.values().toArray()) {
                AnnotationTelemetry annotationTelemetry = (AnnotationTelemetry)valueObject;
                annotationTelemetry.writeValue(context, out);
            }
        }
        if (context.satisfies(ProtocolRequirement.V2)) {
            for (MBeanTelemetry mbeanTelemetry : mbeanTelemetries) {
                mbeanTelemetry.writeValue(context, out);
            }
        }
        out.writeBoolean(false);

        Runtime runtime = Runtime.getRuntime();
        out.writeLong(runtime.totalMemory());
        out.writeLong(runtime.freeMemory());
        if (context.satisfies(ProtocolRequirement.V6)) {
            out.writeLong(runtime.maxMemory());
        }
        long[] cpuDataAverage;
        synchronized (this) {
            cpuDataAverage = cpuData.getAverage();
        }
        out.writeLong(cpuDataAverage[0]);
        out.writeLong(cpuDataAverage[1]);
        out.writeLong(threadMXBean.getThreadCount());

        out.writeBoolean(false); // no messages
    }

    public static void terminateRecording() {
        TelemetryCollector telemetryCollector = instance;
        if (telemetryCollector != null) {
            telemetryCollector.terminateRecording = true;
        }
    }

    public void addAnnotationTelemetry(Method method, Telemetry telemetry) {
        AnnotationTelemetry annotationTelemetry = new AnnotationTelemetry(method, telemetry);
        annotationTelemetries.put(annotationTelemetry.getName(), annotationTelemetry);
    }

    public void setConfig(TelemetrySettings telemetrySettings) {
        Set<MBeanTelemetry> mbeanTelemetries = new HashSet<>();
        for (MBeanTelemetryConfig telemetryConfig : telemetrySettings.getMbeanTelemetries()) {
            for (MBeanLineConfig lineConfig : telemetryConfig.getLines()) {
                try {
                    mbeanTelemetries.add(new MBeanTelemetry(telemetryConfig.getName(), lineConfig));
                } catch (Throwable t) {
                    Logger.log(Subsystem.MBEAN, 0, true, t);
                }
            }
        }
        this.mbeanTelemetries = mbeanTelemetries;
    }

}
