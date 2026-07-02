package com.jvmguard.agent.comm;

import com.jvmguard.agent.JvmGuardAgent;
import com.jvmguard.agent.util.MaxThreadPoolExecutor;
import com.jvmguard.agent.util.ReuseNameThreadFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DeferredExecutor {
    private final ReuseNameThreadFactory threadFactory = new ReuseNameThreadFactory("_jvmguard_deferred_comm", true, Thread.NORM_PRIORITY, JvmGuardAgent.AGENT_THREAD_GROUP);

    private final ExecutorService secondaryExecutor = new MaxThreadPoolExecutor(2, 2, TimeUnit.MINUTES, threadFactory);

    private final ExecutorService primaryExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new SynchronousQueue<>(), threadFactory, (r, executor) -> secondaryExecutor.submit(r));

    public void submit(Runnable runnable) {
        primaryExecutor.submit(runnable);
    }
}
