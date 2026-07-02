package com.jvmguard.agent.util;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MaxThreadPoolExecutor extends ThreadPoolExecutor {
    public MaxThreadPoolExecutor(int maxThreads, long keepAliveTime, TimeUnit unit, ThreadFactory threadFactory) {
        super(maxThreads, maxThreads, keepAliveTime, unit, new LinkedBlockingQueue<>(), threadFactory);
        allowCoreThreadTimeOut(true);
    }
}
