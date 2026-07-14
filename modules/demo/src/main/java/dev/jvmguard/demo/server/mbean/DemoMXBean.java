package dev.jvmguard.demo.server.mbean;

@SuppressWarnings("unused")
public interface DemoMXBean {

    StoreInfo getStoreInfo();

    String[] getRegions();

    int getOpenStores();

    void setOpenStores(int openStores);

    boolean isOpen();

    void setOpen(boolean open);

    StoreInfo[] listStores(int count);

    int refreshStats();
}
