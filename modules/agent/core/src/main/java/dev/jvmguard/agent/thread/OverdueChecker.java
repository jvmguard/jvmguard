package dev.jvmguard.agent.thread;

import dev.jvmguard.agent.AgentProperties;
import dev.jvmguard.agent.JvmGuardAgent;
import dev.jvmguard.agent.RequestSession;
import dev.jvmguard.agent.base.logging.Subsystem;
import dev.jvmguard.agent.util.Logger;

import java.util.ArrayList;

public class OverdueChecker extends Thread {
    private static final long CHECK_NANOS = AgentProperties.getLong("overdueCheckNanos", 1000 * 1000 * 5); // 5 ms
    private static final int CHECK_INTERVAL_MULTIPLIER = AgentProperties.getInteger("overdueCheckIntervalMultiplier", 200); // 1 s

    private static final OverdueChecker instance = new OverdueChecker();

    public static OverdueChecker getInstance() {
        return instance;
    }

    private OverdueChecker() {
        super(JvmGuardAgent.AGENT_THREAD_GROUP, "_jvmguard_overdue_checker");
        setDaemon(true);
        setPriority(Thread.MAX_PRIORITY);
    }

    @Override
    public void run() {
        try {
            checkOverdueLoop();
        } catch (Throwable t) {
            Logger.log(Subsystem.COMMON, 0, true, t);
        }
    }

    private void checkOverdueLoop() {
        long currentTime = System.nanoTime();

        ArrayList<ThreadManager> allThreadManagers = new ArrayList<>();

        long count = 0;
        //noinspection InfiniteLoopStatement
        while (true) {
            if ((++count % CHECK_INTERVAL_MULTIPLIER) == 0) { // 1s by default
                RequestSession.getInstance().fillThreads(allThreadManagers);
                for (ThreadManager threadManager : allThreadManagers) {
                    threadManager.checkOverdue(currentTime);
                }
                allThreadManagers.clear();
            }

            try {
                //noinspection BusyWait
                Thread.sleep(CHECK_NANOS / 1000 / 1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            currentTime = System.nanoTime();
        }
    }
}
