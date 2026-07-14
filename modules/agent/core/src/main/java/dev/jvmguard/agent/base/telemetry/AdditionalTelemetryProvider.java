package dev.jvmguard.agent.base.telemetry;

import dev.jvmguard.agent.AgentConstants;
import dev.jvmguard.agent.util.Util;

import java.util.ArrayList;
import java.util.List;

public class AdditionalTelemetryProvider {
    public static final String SYSTEM_LOAD_DESCRIPTION = "System load";
    public static final String PROCESS_LOAD_DESCRIPTION = "Process load";

    public static final int PERCENT_100_VALUE = 10000;

    protected long lastDataTime;

    protected long lastCpuTime;

    protected int lastCpuLoad = -1;
    protected final long cpuDivider;

    protected List<TelemetryDescription> descriptions;

    protected boolean processCpuAvailable;
    protected boolean systemCpuAvailable;


    public AdditionalTelemetryProvider() {
        long cpuDivider = 1000000;
        if (Util.isJ9VM() && !Util.isJava9Plus() && !Util.isMac()) { // J9 VMs on Mac are always new and getProcessCpuLoad cannot be used
            cpuDivider = 10000; // this actually changed during IBM 1.8 releases. For newer versions, getProcessCpuLoad should be available, though, so this won't be used.
        }
        this.cpuDivider = cpuDivider;
    }

    protected int addAdditionalData(boolean inKb, long[] data, long elapsedTime) {
        return 0;
    }

    @SuppressWarnings("UnusedAssignment")
    public final long[] getData(boolean inKb) {
        long[] data = new long[descriptions.size()];
        long thisTime = System.nanoTime();
        long elapsedTime = (thisTime - lastDataTime) / 1000000;

        int pos = addAdditionalData(inKb, data, elapsedTime);
        if (processCpuAvailable) {
            long cpuLoad = getCpuLoad(elapsedTime);
            data[pos++] = cpuLoad;
            if (systemCpuAvailable) {
                long systemLoad = getSystemLoad(elapsedTime);
                if (systemLoad < cpuLoad) {
                    systemLoad = cpuLoad;
                }
                data[pos++] = systemLoad;
            }
        }

        lastDataTime = thisTime;
        return data;
    }

    public List<TelemetryDescription> getDescriptions() {
        if (descriptions == null) {
            descriptions = new ArrayList<>();
            init();
        }
        return descriptions;
    }

    public final void reset() {
        lastDataTime = 0;
    }

    protected void init() {
    }

    protected long getCpuLoad(long elapsedTime) {
        return -1;
    }

    protected final long handleProcessCpuTime(long elapsedTime, long cpuTime) {
        if (cpuTime > -1) {
            long ret = 0;
            if (lastDataTime != 0) {
                ret = (cpuTime - lastCpuTime) * PERCENT_100_VALUE / cpuDivider / elapsedTime / Runtime.getRuntime().availableProcessors();
            }
            lastCpuTime = cpuTime;
            lastCpuLoad = (int)(ret / 100);
            return ret;
        }
        return -1;
    }

    protected long getSystemLoad(long elapsedTime) {
        return -1;
    }


    protected final void addSystemCpuDescription() {
        systemCpuAvailable = true;
        descriptions.add(new TelemetryDescription(SYSTEM_LOAD_DESCRIPTION, AgentConstants.TELEMETRY_TYPE_SYSTEM_LOAD));
    }

    protected final void addProcessCpuDescription() {
        processCpuAvailable = true;
        descriptions.add(new TelemetryDescription(PROCESS_LOAD_DESCRIPTION, AgentConstants.TELEMETRY_TYPE_CPU_USAGE));
    }
}
