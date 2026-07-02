package com.jvmguard.agent.instrument;

import com.jvmguard.agent.base.logging.Subsystem;
import com.jvmguard.agent.AgentProperties;
import com.jvmguard.agent.JvmGuardAgent;
import com.jvmguard.agent.util.Logger;
import com.jvmguard.agent.util.JvmGuardThreadFactory;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.concurrent.*;

class RetransformationUtil {
    private static final boolean RETRANSFORM_ASYNC = AgentProperties.getBoolean("retransformAsync", true);
    private static final int RETRANSFORM_WAIT = AgentProperties.getInteger("retransformWait", 500);

    private static ThreadPoolExecutor retransformationExecutor = new ThreadPoolExecutor(1, 1, 60L, TimeUnit.SECONDS, new LinkedBlockingDeque<>(), new JvmGuardThreadFactory("_jvmguard_retransformation", true, Thread.NORM_PRIORITY, JvmGuardAgent.AGENT_THREAD_GROUP));

    static void init() {
        retransformationExecutor.prestartAllCoreThreads();
    }

    static void doRetransformation(final Instrumentation instrumentation, final Class clazz) throws UnmodifiableClassException {
        if (!RETRANSFORM_ASYNC) {
            instrumentation.retransformClasses(clazz);
        } else {
            Future<?> future = retransformationExecutor.submit(() -> {
                try {
                    instrumentation.retransformClasses(clazz);
                } catch (Throwable e) {
                    System.err.println(clazz);
                    e.printStackTrace();
                }
            });
            if (RETRANSFORM_WAIT >= 0) {
                try {
                    if (RETRANSFORM_WAIT == 0) {
                        future.get();
                    } else {
                        future.get(RETRANSFORM_WAIT, TimeUnit.MILLISECONDS);
                    }
                } catch (TimeoutException e) {
                    Logger.log(Subsystem.INSTRUMENTATION, 2, false, "timeout waiting for retransformation of class %s (wait time %d)\n", clazz.getName(), RETRANSFORM_WAIT);
                } catch (InterruptedException | ExecutionException e) {
                    Logger.log(Subsystem.INSTRUMENTATION, 2, false, e);
                }
            }
        }
    }
}
