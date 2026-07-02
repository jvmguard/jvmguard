package com.jvmguard.integration.util;

public class HeapInfo {
    private long pageFaultCount;
    private long peakWorkingSetSize;
    private long workingSetSize;
    private long quotaPeakPagedPoolUsage;
    private long quotaPagedPoolUsage;
    private long quotaPeakNonPagedPoolUsage;
    private long quotaNonPagedPoolUsage;
    private long pagefileUsage;
    private long peakPagefileUsage;
    private long javaHeap;

    private String id;
    private boolean compare;

    public static synchronized HeapInfo getHeapInfo() {
        return new HeapInfo();
    }

    private HeapInfo() {
        javaHeap = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getPageFaultCount() {
        return pageFaultCount;
    }

    public long getPeakWorkingSetSize() {
        return peakWorkingSetSize;
    }

    public long getWorkingSetSize() {
        return workingSetSize;
    }

    public long getQuotaPeakPagedPoolUsage() {
        return quotaPeakPagedPoolUsage;
    }

    public long getQuotaPagedPoolUsage() {
        return quotaPagedPoolUsage;
    }

    public long getQuotaPeakNonPagedPoolUsage() {
        return quotaPeakNonPagedPoolUsage;
    }

    public long getQuotaNonPagedPoolUsage() {
        return quotaNonPagedPoolUsage;
    }

    public long getPagefileUsage() {
        return pagefileUsage;
    }

    public long getPeakPagefileUsage() {
        return peakPagefileUsage;
    }

    @Override
    public String toString() {
        return "HeapInfo{" +
                "pageFaultCount=" + pageFaultCount +
                ", peakWorkingSetSize=" + peakWorkingSetSize +
                ", workingSetSize=" + workingSetSize +
                ", quotaPeakPagedPoolUsage=" + quotaPeakPagedPoolUsage +
                ", quotaPagedPoolUsage=" + quotaPagedPoolUsage +
                ", quotaPeakNonPagedPoolUsage=" + quotaPeakNonPagedPoolUsage +
                ", quotaNonPagedPoolUsage=" + quotaNonPagedPoolUsage +
                ", pagefileUsage=" + pagefileUsage +
                ", peakPagefileUsage=" + peakPagefileUsage +
                '}';
    }

    public long getJavaHeap() {
        return javaHeap;
    }

    public void setCompare(boolean compare) {
        this.compare = compare;
    }

    public boolean isCompare() {
        return compare;
    }
}
