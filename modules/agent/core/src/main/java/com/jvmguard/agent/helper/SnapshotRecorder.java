package com.jvmguard.agent.helper;

import com.jvmguard.agent.JvmGuardAgent;
import com.jvmguard.agent.util.JvmGuardThreadFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SnapshotRecorder {

    private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 60L, TimeUnit.SECONDS, new LinkedBlockingDeque<>(), new JvmGuardThreadFactory("_jvmguard_recording", false, Thread.MAX_PRIORITY, JvmGuardAgent.AGENT_THREAD_GROUP));

    public static void shutdownWithoutWait() {
        executor.shutdown();
    }

    public static <T> Future<T> execute(Callable<T> callable) {
        return executor.submit(callable);
    }
}
