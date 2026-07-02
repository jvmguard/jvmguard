package com.jvmguard.agent.util;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class JvmGuardThreadFactory implements ThreadFactory {
    private final ThreadGroup group;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    protected final String namePrefix;
    private final boolean daemon;
    private final int priority;

    public JvmGuardThreadFactory(String prefix, boolean daemon, int priority) {
        this(prefix, daemon, priority, System.getSecurityManager() != null ? System.getSecurityManager().getThreadGroup() : Thread.currentThread().getThreadGroup());
    }

    public JvmGuardThreadFactory(String prefix, boolean daemon, int priority, ThreadGroup parentGroup) {
        this.daemon = daemon;
        this.priority = priority;
        group = new ThreadGroup(parentGroup, prefix);
        namePrefix = prefix + "-thread-";
    }

    @Override
    public Thread newThread(@NotNull Runnable runnable) {
        Thread thread = new Thread(group, runnable, namePrefix, 0);
        updateThreadName(thread);
        thread.setDaemon(daemon);
        thread.setPriority(priority);
        return thread;
    }

    public void updateThreadName(Thread thread) {
        thread.setName(namePrefix + threadNumber.getAndIncrement());
    }
}
