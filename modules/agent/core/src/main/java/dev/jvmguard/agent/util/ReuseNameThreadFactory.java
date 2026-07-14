package dev.jvmguard.agent.util;

import javax.annotation.concurrent.GuardedBy;
import java.lang.Thread.State;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class ReuseNameThreadFactory extends JvmGuardThreadFactory {
    @GuardedBy("currentThreads")
    private final List<WeakReference<Thread>> currentThreads = new ArrayList<>();

    public ReuseNameThreadFactory(String prefix, boolean daemon, int priority, ThreadGroup parentGroup) {
        super(prefix, daemon, priority, parentGroup);
    }

    @Override
    public void updateThreadName(Thread thread) {
        synchronized (currentThreads) {
            for (int pos = 0; pos < currentThreads.size(); pos++) {
                Thread previousThread = currentThreads.get(pos).get();
                if (previousThread == null || previousThread.getState() == State.TERMINATED) {
                    currentThreads.set(pos, new WeakReference<>(thread));
                    thread.setName(namePrefix + (pos + 1));
                    return;
                }
            }
            currentThreads.add(new WeakReference<>(thread));
            thread.setName(namePrefix + currentThreads.size());
        }
    }
}
