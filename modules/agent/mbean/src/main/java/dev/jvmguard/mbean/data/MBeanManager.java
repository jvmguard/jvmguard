package dev.jvmguard.mbean.data;

import dev.jvmguard.mbean.common.MBeanHelper;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class MBeanManager {
    private static final String MESSAGE_PREFIX = "MBean> ";
    public static final boolean MBEAN_SUPPORTED = isManagementAvailable();
    private static final boolean LOG_NOTIFICATION = Boolean.getBoolean("jvmguard.mbeanNotificationDebug");

    private static final NameResult UNCHANGED_NAME_RESULT = new NameResult(Collections.emptySet(), false);

    private static SortedMap<Integer, ServerInfo> servers = new TreeMap<>();
    private static int nextId = 1;

    private static boolean serverListChanged = true;

    private static volatile boolean platformCreated;

    private static volatile LogAdapter logAdapter = new LogAdapter() {
        @Override
        public void error(String message) {
            System.err.println(MESSAGE_PREFIX + message);
        }

        @Override
        public void error(Throwable t) {
            System.err.println(MESSAGE_PREFIX + t.toString());
            t.printStackTrace();
        }

        @Override
        public boolean isLogNotification() {
            return LOG_NOTIFICATION;
        }
    };

    public static LogAdapter getLogAdapter() {
        return logAdapter;
    }

    public static void setLogAdapter(LogAdapter logAdapter) {
        MBeanManager.logAdapter = logAdapter;
    }

    private static boolean isManagementAvailable() {
        try {
            Class.forName("javax.management.openmbean.TabularData");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static synchronized void addServer(MBeanServer mBeanServer) {
        if (MBEAN_SUPPORTED) {
            if (mBeanServer != null) {
                boolean add = true;
                for (Iterator<ServerInfo> iterator = servers.values().iterator(); iterator.hasNext(); ) {
                    ServerInfo serverInfo = iterator.next();
                    MBeanServer previous = serverInfo.getServer();
                    if (previous == null) {
                        iterator.remove();
                    } else if (previous == mBeanServer) {
                        add = false;
                    }
                }
                if (add) {
                    servers.put(nextId, new ServerInfo(nextId, mBeanServer));
                    serverListChanged = true;
                    nextId++;
                }
            }
        }
    }

    public static synchronized NameResult getMBeanNames() {
        if (MBEAN_SUPPORTED) {
            if (serverListChanged || hasChangedServer()) {
                serverListChanged = false;
                Set<String> names = new HashSet<>();
                for (ServerInfo serverInfo : servers.values()) {
                    serverInfo.addNames(names);
                }
                return new NameResult(names, true);
            }
        }
        return UNCHANGED_NAME_RESULT;
    }

    protected static boolean hasChangedServer() {
        for (ServerInfo serverInfo : servers.values()) {
            if (serverInfo.isChanged()) {
                return true;
            }
        }
        return false;
    }

    public static synchronized void resetDifferentialDataTransfer() {
        serverListChanged = true;
    }

    public static synchronized MBeanServer getMBeanServer(ObjectName objectName) {
        for (ServerInfo serverInfo : servers.values()) {
            MBeanServer mBeanServer = serverInfo.getServer();
            if (mBeanServer != null) {
                if (mBeanServer.isRegistered(objectName)) {
                    return mBeanServer;
                }
            }
        }
        return null;
    }

    public static Object invokeOperation(ObjectName objectName, MBeanServer server, MBeanOperationInfo operationInfo, Object[] inputParameters) throws ReflectionException, InstanceNotFoundException, MBeanException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        String[] signature = new String[operationInfo.getSignature().length];
        Object[] params = new Object[operationInfo.getSignature().length];
        for (int i = 0; i < signature.length; i++) {
            signature[i] = operationInfo.getSignature()[i].getType();
            params[i] = MBeanHelper.convertArray(inputParameters[i], signature[i]);
        }
        return server.invoke(objectName, operationInfo.getName(), params, signature);
    }

    public static void setAttribute(ObjectName objectName, MBeanServer server, MBeanAttributeInfo attributeInfo, Object value) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, AttributeNotFoundException, MBeanException, ReflectionException, InstanceNotFoundException, InvalidAttributeValueException {
        server.setAttribute(objectName, new Attribute(attributeInfo.getName(), MBeanHelper.convertArray(value, attributeInfo.getType())));
    }

    public static AttributeList getOpenAttributes(ObjectName objectName, MBeanServer server, MBeanInfo beanInfo) throws ReflectionException, InstanceNotFoundException {
        MBeanAttributeInfo[] attributeInfos = beanInfo.getAttributes();
        String[] attributeNames = new String[attributeInfos.length];
        for (int i = 0; i < attributeInfos.length; i++) {
            attributeNames[i] = attributeInfos[i].getName();
        }
        return server.getAttributes(objectName, attributeNames);
    }

    public static void createPlatformServer() {
        try {
            if (!platformCreated) {
                MBeanServer server = ManagementFactory.getPlatformMBeanServer();
                if (server != null) {
                    addServer(server); // on shutdown, the intercepted builder methods are not called on IBM, so additionally add this here
                }
                platformCreated = true;
            }
        } catch (Throwable ignored) {
        }
    }


    public interface LogAdapter {
        void error(String message);
        void error(Throwable t);
        boolean isLogNotification();
    }

    public static class NameResult {
        private Set<String> names;
        private boolean changed;

        public NameResult(Set<String> names, boolean changed) {
            this.names = names;
            this.changed = changed;
        }

        public boolean isChanged() {
            return changed;
        }

        public Set<String> getNames() {
            return names == null ? Collections.emptySet() : names;
        }
    }
}
