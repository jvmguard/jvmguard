package dev.jvmguard.integration;

import dev.jvmguard.integration.util.HeapInfo;
import dev.jvmguard.integration.util.SleepHelper;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jetbrains.annotations.NotNull;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractRun {
    private static final int MAXIMUM_COOL_DOWN = 10000;
    protected static final String TEST_ATTACH_WAIT = "test.attachWait";

    private static final AtomicInteger fence = new AtomicInteger(0);
    protected int lastIndex = 0;
    private List<Timer> timers = new ArrayList<>();
    private List<HeapInfo> heapInfos = new ArrayList<>();

    private long startTime;
    private boolean firstTiming = true;
    private boolean firstHeap = true;

    public static final int VM_NO =  Integer.getInteger(Util.VMNO_PROP_NAME, 1);
    public static final int RUN_NO =  Integer.getInteger(Util.RUNNO_PROP_NAME, 1);

    public abstract void run() throws Throwable;

    protected final void coolDown() {
        coolDown(MAXIMUM_COOL_DOWN);
    }

    private void coolDown(int ms) {
        System.gc();
        sleep(ms);
    }

    public static void sleep(int ms) {
        SleepHelper.sleep(ms);
    }

    public int getLastIndex() {
        return lastIndex;
    }

    protected final File getSnapshotFile(String id, String suffix) {
        return new File(Util.removeCommonPrefix(System.getProperty(Util.DEFAULT_RUNCLASS_PROP_NAME, getClass().getName())) + "_" + id + suffix);
    }

    protected final File getSnapshotFile(int index, String suffix) {
        return getSnapshotFile(index, getVmNo(), suffix);
    }

    public final File getSnapshotFile(int index, int vmNo, String suffix) {
        return new File(Util.removeCommonPrefix(System.getProperty(Util.DEFAULT_RUNCLASS_PROP_NAME, getClass().getName())) + "_" + getRunNo() + "_" + vmNo + "_" + index + suffix);
    }

    protected final int getRunNo() {
        return RUN_NO;
    }

    protected final int getVmNo() {
        return VM_NO;
    }

    protected final int getLibraryNo() {
        return Integer.getInteger(Util.LIBRARYNO_PROP_NAME, 1);
    }

    protected final String getVmName() {
        return System.getProperty(Util.VM_PROP_NAME);
    }

    protected static boolean isJava15() {
        return System.getProperty("java.version").startsWith("1.5");
    }

    public static boolean isJava16() {
        return System.getProperty("java.version").startsWith("1.6");
    }

    public static boolean isJava17() {
        return System.getProperty("java.version").startsWith("1.7");
    }

    protected static boolean isJava9Plus() {
        return !System.getProperty("java.version").startsWith("1.");
    }

    protected final boolean is32bit() {
        return "32".equals(System.getProperty("sun.arch.data.model"));
    }

    public static boolean isIBM() {
        String vendor = System.getProperty("java.vendor").toLowerCase();
        String vmVendor = System.getProperty("java.vm.vendor").toLowerCase();
        return vendor.contains("ibm") || vendor.contains("openj9") || vmVendor.contains("ibm") || vmVendor.contains("openj9");
    }

    public static boolean isAttachable() {
        return Boolean.getBoolean(Util.ATTACHABLE_PROP_NAME);
    }

    protected final void runAndWait(int count, Runnable runnable) {
        ThreadGroup threadGroup = new ThreadGroup("worker-group");
        Thread[] threads = new Thread[count];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(threadGroup, runnable, "test"  + i);
            threads[i].start();
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    protected static void waitForAttach() {
        System.out.println("waiting for attach");
        sleep(1000 * Integer.getInteger(TEST_ATTACH_WAIT, 8));
    }

    protected void resetCompare() {
        firstTiming = true;
        firstHeap = true;
    }

    protected void saveTiming(String id) {
        saveTiming(id, false);
    }

    protected void saveTiming(String id, boolean compare) {
        if (firstTiming) {
            compare = false;
        }
        long currentTime = System.currentTimeMillis();
        if (id != null) {
            timers.add(new Timer(id, currentTime - startTime, compare));
            firstTiming = false;
        }
        startTime = currentTime;
    }

    protected void startTiming() {
        startTime = System.currentTimeMillis();
    }

    protected void saveHeapInfo(String id) {
        saveHeapInfo(id, false);
    }

    protected void saveHeapInfo(String id, boolean compare) {
        if (Util.isWindows()) {
            if (firstHeap) {
                compare = false;
            }
            if (id != null) {
                System.gc();
                HeapInfo heapInfo = HeapInfo.getHeapInfo();
                heapInfo.setId(id);
                heapInfo.setCompare(compare);
                heapInfos.add(heapInfo);
                firstHeap = false;
            }
        }
    }

    protected void createMarkerFile(String suffix) {
        File file = getMarkerFile(suffix);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(1);
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @NotNull
    protected File getMarkerFile(String suffix) {
        return new File(Util.removeCommonPrefix(getClass().getName()) + "_z_" + suffix + "_" + getRunNo());
    }

    public String getHelperFileName(String suffix) {
        return Util.removeCommonPrefix(getClass().getName()) + "_y_" + suffix;
    }

    protected void waitForMarkerFile(String suffix) {
        File file = getMarkerFile(suffix);
        while (!file.exists()) {
            sleep(100);
        }
        fence.getAndSet(0);
        sleep(1000);
        fence.getAndSet(0);
    }

    protected void waitForMarkerFileNoAdditionalSleep(String suffix) {
        File file = getMarkerFile(suffix);
        while (!file.exists()) {
            sleep(100);
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    protected void waitForMarkerFileSpinning(String suffix) {
        File file = getMarkerFile(suffix);
        while (!file.exists());
        long startTime = System.nanoTime();
        while ((System.nanoTime() - startTime) < 1000L * 1000 * 1000);
    }

    protected void savePerformance(String id) throws IOException, JDOMException {
        Element root;
        Document document;
        int runNo = 1;

        File snapshotFile = getSnapshotFile(id, ".perf");
        if (snapshotFile.isFile()) {
            InputStream in = new BufferedInputStream(new FileInputStream(snapshotFile));
            document = new SAXBuilder().build(in);
            root = document.getRootElement();
            runNo = Integer.parseInt(root.getAttributeValue("runNo")) + 1;
        } else {
            root = new Element("performance");
            document = new Document(root);
        }
        root.setAttribute("runNo", String.valueOf(runNo));
        root.addContent(getHeapInfoList());
        root.addContent(getTimerList());
        for (Element element : root.getChildren()) {
            if (element.getAttribute("runNo") == null) {
                element.setAttribute("runNo", String.valueOf(runNo));
            }
        }

        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        FileOutputStream fos = new FileOutputStream(snapshotFile);
        outputter.output(document, fos);
        fos.close();
    }

    private List<Element> getTimerList() {
        List<Element> list = new ArrayList<>(timers.size());
        for (Timer timer : timers) {
            Element element = new Element("timer");
            element.setAttribute("id", timer.getId());
            element.setAttribute("compare", String.valueOf(timer.isCompare()));
            element.setAttribute("value", String.valueOf(timer.getTime()));
            list.add(element);
        }
        return list;
    }

    private List<Element> getHeapInfoList() {
        List<Element> list = new ArrayList<>(heapInfos.size());
        for (HeapInfo heapInfo : heapInfos) {
            Element element = new Element("heapInfo");
            element.setAttribute("id", heapInfo.getId());
            element.setAttribute("compare", String.valueOf(heapInfo.isCompare()));
            element.setAttribute("peakWorkingSetSize", String.valueOf(heapInfo.getPeakWorkingSetSize()));
            element.setAttribute("workingSetSize", String.valueOf(heapInfo.getWorkingSetSize()));
            element.setAttribute("pagefileUsage", String.valueOf(heapInfo.getPagefileUsage()));
            element.setAttribute("peakPagefileUsage", String.valueOf(heapInfo.getPeakPagefileUsage()));
            element.setAttribute("javaHeap", String.valueOf(heapInfo.getJavaHeap()));
            list.add(element);
        }
        return list;
    }

    protected boolean isReloading() {
        return Boolean.getBoolean(Util.RELOADING_PROP_NAME);
    }

    protected boolean isCheckUnloaded() {
        return true;
    }

    private WeakReference<ClassLoader> reloadClassLoaderRef;

    void runReloaded() throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException, SQLException {
        Object test = reloadTest();
        Class<?> testClass = test.getClass();
        reloadClassLoaderRef = new WeakReference<>(testClass.getClassLoader());
        Class<?> abstractRunClass = testClass.getClassLoader().loadClass(AbstractRun.class.getName());
        Field lastIndexField = abstractRunClass.getDeclaredField("lastIndex");
        lastIndexField.setAccessible(true);
        lastIndexField.set(test, 100);
        Thread.currentThread().setContextClassLoader(testClass.getClassLoader());
        testClass.getMethod("run").invoke(test);
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            DriverManager.deregisterDriver(drivers.nextElement());
        }
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
    }

    void checkReloaded() {
        if (reloadClassLoaderRef != null) {
            System.gc();
            for (int i = 0; i < 20 && reloadClassLoaderRef.get() != null; i++) {
                sleep(1000);
                System.gc();
            }
            if (!isCheckUnloaded()) {
                System.out.println("CLASS LOADER " + reloadClassLoaderRef.get());
                System.exit(0);
            } else if (reloadClassLoaderRef.get() != null) {
                System.out.println("CLASS LOADER NOT UNLOADED " + reloadClassLoaderRef.get());
                System.exit(1);
            } else {
                System.out.println("CLASS LOADER UNLOADED");
                System.exit(0);
            }
        }
    }

    private Object reloadTest() {
        try {
            URLClassLoader classLoader = ReloadingClassLoader.newInstance(getReloadingUrls());
            Class<?> reloadedTestClass = classLoader.loadClass(getClass().getName());
            if (reloadedTestClass == getClass()) {
                System.err.println("reload failed");
                System.exit(1);
            }
            System.out.println("STARTING RELOADED " + reloadedTestClass.getClassLoader());
            return reloadedTestClass.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
            throw new RuntimeException();
        }
    }

    @NotNull
    protected URLClassLoader newReloadingClassLoader() {
        return new URLClassLoader(getReloadingUrls(), getClass().getClassLoader().getParent());
    }

    @NotNull
    private URL[] getReloadingUrls() {
        String[] jars = System.getProperty("java.class.path").split(File.pathSeparator);
        URL[] urls = new URL[jars.length];
        for (int i = 0; i < urls.length; i++) {
            try {
                urls[i] = new File(jars[i]).toURI().toURL();
            } catch (MalformedURLException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
        return urls;
    }

    public static class ReloadingClassLoader extends URLClassLoader {
        public static URLClassLoader newInstance(URL[] urls) {
            return new ReloadingClassLoader(urls, ReloadingClassLoader.class.getClassLoader().getParent());
        }

        ReloadingClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            checkAgentClasses(name);
            return super.loadClass(name);
        }

        @Override
        public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            checkAgentClasses(name);
            return super.loadClass(name, resolve);
        }

        private static void checkAgentClasses(String name) {
            if (name.startsWith("dev.jvmguard.agent") && !name.startsWith("dev.jvmguard.agent.__JvmGuard_")) {
                System.err.println("AGENT CLASS UNEXPECTED " + name);
                System.exit(1);
            }
        }
    }

    private static class Timer {
        long time;
        private boolean compare;
        String id;

        Timer(String id, long time, boolean compare) {
            this.id = id;
            this.time = time;
            this.compare = compare;
        }

        public long getTime() {
            return time;
        }

        public String getId() {
            return id;
        }

        public boolean isCompare() {
            return compare;
        }
    }

    protected static void printStackTrace() {
        StackTracePrinter.printStackTrace();
    }

    protected static class StackTracePrinter {
        protected static void printStackTrace() {
            try {
                MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
                ObjectName objectName = new ObjectName("com.sun.management:type=DiagnosticCommand");
                System.out.println(platformMBeanServer.invoke(objectName, "threadPrint", new Object[]{ new String[0] }, new String[] {String[].class.getName()}));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static class DoNotRunException extends Exception {
    }
}
