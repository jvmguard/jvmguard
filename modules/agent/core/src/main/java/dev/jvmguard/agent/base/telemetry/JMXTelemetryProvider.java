package dev.jvmguard.agent.base.telemetry;

import dev.jvmguard.agent.AgentConstants;
import dev.jvmguard.agent.util.Util;
import dev.jvmguard.agent.util.reflection.ReflectionUtil;

import java.lang.management.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class JMXTelemetryProvider extends AdditionalTelemetryProvider {

    private static final boolean DEBUG_TELEMETRY = Boolean.getBoolean("jvmguard.debugTelemetry");

    private List<MemoryPoolMXBean> memoryPoolMXBeans;
    private MemoryPoolMXBean metaspacePool;
    private MemoryPoolMXBean compressedClassPool;

    private List<GarbageCollectorMXBean> garbageCollectorMXBeans;
    private OperatingSystemMXBean operatingSystemMXBean;
    private Method getProcessCpuTimeMethod;
    private Method getProcessCpuLoadMethod;
    private Method getSystemCpuLoadMethod;

    private long[] lastGcTimes;

    @Override
    protected int addAdditionalData(boolean inKb, long[] data, long elapsedTime) {
        int pos = 0;
        MemoryUsage compressedClassPoolUsage = compressedClassPool != null ? compressedClassPool.getUsage() : null;
        for (MemoryPoolMXBean memoryPoolMXBean : memoryPoolMXBeans) {
            MemoryUsage usage;
            if (memoryPoolMXBean == compressedClassPool) {
                usage = compressedClassPoolUsage;
            } else {
                usage = memoryPoolMXBean.getUsage();
            }
            long usedValue = usage == null ? 0 : usage.getUsed();
            long committedValue = usage == null ? 0 : usage.getCommitted();
            if (memoryPoolMXBean == metaspacePool && compressedClassPoolUsage != null) {
                long compressedUsed = compressedClassPoolUsage.getUsed();
                if (compressedUsed >= 0 && compressedUsed <= usedValue) {
                    usedValue -= compressedUsed;
                }
                long compressedCommitted = compressedClassPoolUsage.getCommitted();
                if (compressedCommitted >= 0 && compressedCommitted <= committedValue) {
                    committedValue -= compressedCommitted;
                }
            }

            data[pos++] = inKb ? usedValue / 1000 : usedValue;
            data[pos++] = inKb ? committedValue / 1000 : committedValue;
        }

        for (int i = 0; i < garbageCollectorMXBeans.size(); i++) {
            GarbageCollectorMXBean garbageCollectorMXBean = garbageCollectorMXBeans.get(i);
            long gcTime = garbageCollectorMXBean.getCollectionTime();
            if (lastDataTime == 0) {
                data[pos++] = 0;
            } else {
                data[pos++] = (gcTime - lastGcTimes[i]) * PERCENT_100_VALUE / elapsedTime;
            }
            lastGcTimes[i] = gcTime;
        }
        return pos;
    }

    private static long getProcessCpuTime(OperatingSystemMXBean operatingSystemMXBean, Method getProcessCpuTimeMethod) {
        if (getProcessCpuTimeMethod != null && operatingSystemMXBean != null) {
            try {
                Long ret = (Long)getProcessCpuTimeMethod.invoke(operatingSystemMXBean, ReflectionUtil.EMPTY_OBJECTS);
                if (ret != null) {
                    return ret;
                }
            } catch (Throwable e) {
                if (DEBUG_TELEMETRY) {
                    e.printStackTrace();
                }
            }
        }
        return -1;
    }

    @Override
    protected long getSystemLoad(long elapsedTime) {
        if (getSystemCpuLoadMethod != null) {
            double systemCpuLoad = getCpuLoad(getSystemCpuLoadMethod);
            if (systemCpuLoad < 0) {
                return -1;
            } else {
                return (long)(systemCpuLoad * PERCENT_100_VALUE);
            }
        }
        return -1;
    }

    @Override
    protected long getCpuLoad(long elapsedTime) {
        if (getProcessCpuLoadMethod == null) {
            return handleProcessCpuTime(elapsedTime, getProcessCpuTime(operatingSystemMXBean, getProcessCpuTimeMethod));
        } else {
            double processCpuLoad = getCpuLoad(getProcessCpuLoadMethod);
            if (processCpuLoad < 0) {
                return handleProcessCpuTime(elapsedTime, getProcessCpuTime(operatingSystemMXBean, getProcessCpuTimeMethod));
            } else {
                lastCpuLoad = (int)(processCpuLoad * 100);
                return (long)(processCpuLoad * PERCENT_100_VALUE);
            }
        }
    }

    private double getCpuLoad(Method getCpuLoadMethod) {
        if (getCpuLoadMethod != null) {
            try {
                Double ret = (Double)getCpuLoadMethod.invoke(operatingSystemMXBean, ReflectionUtil.EMPTY_OBJECTS);
                if (ret != null) {
                    return ret;
                }
            } catch (Throwable e) {
                if (DEBUG_TELEMETRY) {
                    e.printStackTrace();
                }
            }
        }
        return -1;
    }

    private void initMemoryPoolBeans() {
        memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();
        if (!Util.isJ9VM()) {
            MemoryPoolMXBean metaspacePool = null;
            MemoryPoolMXBean compressedClassPool = null;
            for (MemoryPoolMXBean memoryPoolMXBean : memoryPoolMXBeans) {
                if (memoryPoolMXBean.getType() == MemoryType.NON_HEAP) {
                    String poolName = memoryPoolMXBean.getName();
                    if ("Metaspace".equals(poolName)) {
                        metaspacePool = memoryPoolMXBean;
                    } else if ("Compressed Class Space".equals(poolName)) {
                        compressedClassPool = memoryPoolMXBean;
                    }
                }
            }
            if (metaspacePool != null && compressedClassPool != null) {
                this.metaspacePool = metaspacePool;
                this.compressedClassPool = compressedClassPool;
            }
        }
    }

    @Override
    protected void init() {
        initMemoryPoolBeans();

        for (MemoryPoolMXBean memoryPoolMXBean : memoryPoolMXBeans) {
            String poolName = memoryPoolMXBean.getName();
            if (memoryPoolMXBean.getType() == MemoryType.HEAP) {
                descriptions.add(new TelemetryDescription(poolName, AgentConstants.TELEMETRY_TYPE_MEMORY_HEAP_USED));
                descriptions.add(new TelemetryDescription(poolName, AgentConstants.TELEMETRY_TYPE_MEMORY_HEAP_COMMITTED));
            } else {
                if (metaspacePool == memoryPoolMXBean) {
                    poolName = "Metaspace (non-class)";
                }
                descriptions.add(new TelemetryDescription(poolName, AgentConstants.TELEMETRY_TYPE_MEMORY_NON_HEAP_USED));
                descriptions.add(new TelemetryDescription(poolName, AgentConstants.TELEMETRY_TYPE_MEMORY_NON_HEAP_COMMITTED));
            }
        }

        garbageCollectorMXBeans = new ArrayList<>(ManagementFactory.getGarbageCollectorMXBeans());
        for (Iterator<GarbageCollectorMXBean> iterator = garbageCollectorMXBeans.iterator(); iterator.hasNext(); ) {
            GarbageCollectorMXBean garbageCollectorMXBean = iterator.next();
            if (garbageCollectorMXBean.getCollectionTime() == -1) {
                iterator.remove();
            } else {
                descriptions.add(new TelemetryDescription(garbageCollectorMXBean.getName(), AgentConstants.TELEMETRY_TYPE_GC_ACTIVITY));
            }
        }

        OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
        try {
            Method getProcessCpuTimeMethod = operatingSystemMXBean.getClass().getMethod("getProcessCpuTime");
            if (Modifier.isPublic(getProcessCpuTimeMethod.getModifiers())) {
                ReflectionUtil.setAccessible(getProcessCpuTimeMethod);
                if (getProcessCpuTimeMethod.getReturnType().equals(long.class) && getProcessCpuTime(operatingSystemMXBean, getProcessCpuTimeMethod) > -1) {
                    addProcessCpuDescription();
                    this.getProcessCpuTimeMethod = getProcessCpuTimeMethod;
                    this.operatingSystemMXBean = operatingSystemMXBean;
                    if (!Util.isJ9VM() || (!Util.isMac() && Util.isJava18Plus())) { // cannot check for -1 now because this is returned by some VMs during startup
                        try {
                            Method getProcessCpuLoadMethod = operatingSystemMXBean.getClass().getMethod("getProcessCpuLoad");
                            ReflectionUtil.setAccessible(getProcessCpuLoadMethod);
                            this.getProcessCpuLoadMethod = getProcessCpuLoadMethod;
                            Method getSystemCpuLoadMethod = operatingSystemMXBean.getClass().getMethod("getSystemCpuLoad");
                            ReflectionUtil.setAccessible(getSystemCpuLoadMethod);
                            this.getSystemCpuLoadMethod = getSystemCpuLoadMethod;

                            addSystemCpuDescription();
                        } catch (NoSuchMethodException ignored) {
                        }
                    }
                }
            }
        } catch (Throwable e) {
            if (DEBUG_TELEMETRY) {
                e.printStackTrace();
            }
        }

        lastGcTimes = new long[garbageCollectorMXBeans.size()];
    }

}
