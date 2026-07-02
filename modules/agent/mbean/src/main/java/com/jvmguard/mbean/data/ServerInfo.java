package com.jvmguard.mbean.data;

import javax.management.*;
import java.lang.ref.WeakReference;
import java.util.Set;

public class ServerInfo implements NotificationListener {

    private WeakReference<MBeanServer> server;
    private int id;
    private volatile boolean namesChanged = true;
    private NotificationState notificationState = NotificationState.UNREGISTERED;

    public ServerInfo(int id, MBeanServer mBeanServer) {
        this.id = id;
        server = new WeakReference<>(mBeanServer);
    }

    MBeanServer getServer() {
        if (server == null) {
            return null;
        } else {
            MBeanServer ret = server.get();
            if (ret == null) {
                namesChanged = true;
                server = null;
            }
            return ret;
        }
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return "ServerInfo{" +
            "server=" + server +
            ", id=" + id +
            '}';
    }

    public void addNames(Set<String> names) {
        MBeanServer mBeanServer = getServer();
        namesChanged = false;
        if (mBeanServer != null) {
            checkRegisterNotification(mBeanServer);

            for (ObjectName objectName : mBeanServer.queryNames(null, null)) {
                if (!MBeanServerDelegate.DELEGATE_NAME.equals(objectName)) {
                    names.add(objectName.getCanonicalName());
                }
            }
        } else {
            notificationState = NotificationState.REGISTERED;
        }
    }

    public boolean isChanged() {
        getServer();
        return notificationState != NotificationState.REGISTERED || namesChanged;
    }

    protected void checkRegisterNotification(MBeanServer mBeanServer) {
        if (notificationState == NotificationState.UNREGISTERED) {
            try {
                mBeanServer.addNotificationListener(MBeanServerDelegate.DELEGATE_NAME, this, null, null);
                notificationState = NotificationState.REGISTERED;
            } catch (Throwable e) {
                notificationState = NotificationState.UNAVAILABLE;
                if (MBeanManager.getLogAdapter().isLogNotification()) {
                    MBeanManager.getLogAdapter().error(e);
                }
            }
        }
    }

    @Override
    public void handleNotification(Notification notification, Object handback) {
        namesChanged = true;
    }

    private enum NotificationState {
        UNREGISTERED,
        UNAVAILABLE,
        REGISTERED
    }
}
