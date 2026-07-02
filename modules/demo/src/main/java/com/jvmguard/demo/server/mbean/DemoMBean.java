package com.jvmguard.demo.server.mbean;

public class DemoMBean implements DemoMXBean {

    private int openStores = 3;
    private boolean open = true;
    private int refreshCount;

    @Override
    public StoreInfo getStoreInfo() {
        return new StoreInfo();
    }

    @Override
    public String[] getRegions() {
        return new String[] {"US", "EU", "APAC"};
    }

    @Override
    public int getOpenStores() {
        return openStores;
    }

    @Override
    public void setOpenStores(int openStores) {
        this.openStores = openStores;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public void setOpen(boolean open) {
        this.open = open;
    }

    @Override
    public StoreInfo[] listStores(int count) {
        if (count < 0 || count > 20) {
            throw new IllegalArgumentException("Count must be between 0 and 20");
        }
        StoreInfo[] stores = new StoreInfo[count];
        for (int i = 0; i < count; i++) {
            stores[i] = new StoreInfo();
        }
        return stores;
    }

    @Override
    public int refreshStats() {
        return ++refreshCount;
    }
}
