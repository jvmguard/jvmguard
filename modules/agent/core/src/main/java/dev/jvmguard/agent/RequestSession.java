package dev.jvmguard.agent;

import dev.jvmguard.agent.base.logging.Subsystem;
import dev.jvmguard.agent.parameter.ConfigurationParameter;
import dev.jvmguard.agent.thread.ThreadManager;
import dev.jvmguard.agent.tree.AgentTransactionInfo;
import dev.jvmguard.agent.tree.AgentTransactionTree;
import dev.jvmguard.agent.tree.MergeVisitor;
import dev.jvmguard.agent.tree.PolicyTransactionInfo;
import dev.jvmguard.agent.tree.Tree.Visitor;
import dev.jvmguard.agent.util.Logger;

import javax.annotation.concurrent.GuardedBy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class RequestSession {

    private static final int MAX_TRANSACTIONS = AgentProperties.getInteger("maxTransactions", 50000);
    private static final int MAX_STRING_POOL = AgentProperties.getInteger("maxStringPool", 20000);

    private static RequestSession instance = new RequestSession();

    private boolean detached = false;

    public static RequestSession getInstance() {
        return instance;
    }

    @GuardedBy("threads")
    private final List<ThreadManager> threads = new ArrayList<>();
    private final ConcurrentHashMap<AgentTransactionInfo, AgentTransactionInfo> transactionInfos = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> stringPool = new ConcurrentHashMap<>();
    private AtomicLong lastTransactionInfoId = new AtomicLong();

    private volatile long lastSnapshotTimestamp = 0;
    private volatile long lastSnapshotNanoTime = System.nanoTime();

    @GuardedBy("threads")
    private final DeadVirtualThreadAggregator deadVirtualThreadAggregator = new DeadVirtualThreadAggregator();

    private RequestSession() {
    }

    public AgentTransactionInfo getTransactionInfo(AgentTransactionInfo lookupInfo) {
        AgentTransactionInfo ret = transactionInfos.get(lookupInfo);
        if (ret == null) {
            ret = AgentTransactionInfo.create(lookupInfo.getName(), lookupInfo.getTransactionTypeId(), lastTransactionInfoId.incrementAndGet());
            AgentTransactionInfo previous = transactionInfos.putIfAbsent(ret, ret);
            if (previous != null) {
                ret = previous;
            }
        }
        return ret;
    }

    public void detach() {
        synchronized (threads) {
            detached = true;
            threads.clear();
            transactionInfos.clear();
        }
    }

    public void fillThreads(Collection<ThreadManager> newThreads) {
        synchronized (threads) {
            newThreads.addAll(threads);
        }
    }

    public ThreadManager[] getThreads() {
        synchronized (threads) {
            return threads.toArray(new ThreadManager[0]);
        }
    }

    private void removeThreads(Set<ThreadManager> removeSet) {
        synchronized (threads) {
            threads.removeIf(removeSet::contains);
        }
    }

    public long getLastSnapshotNanoTime() {
        return lastSnapshotNanoTime;
    }

    public Data getAndResetData(long snapshotTimeStamp) {
        try {
            return ConfigurationParameter.callWithoutConfigChange(() -> {
                MergeVisitor transactionVisitor;
                MergeVisitor overdueVisitor;
                synchronized (threads) {
                    transactionVisitor = new MergeVisitor(true, deadVirtualThreadAggregator.transactionVisitor.getResult());
                    overdueVisitor = new MergeVisitor(false, deadVirtualThreadAggregator.overdueVisitor.getResult());
                    deadVirtualThreadAggregator.init();
                }
                Set<ThreadManager> deadThreadManagers = Collections.newSetFromMap(new IdentityHashMap<>());
                for (ThreadManager threadManager : getThreads()) {
                    Thread thread = threadManager.getThread();
                    if (thread == null || !thread.isAlive()) {
                        deadThreadManagers.add(threadManager);
                    }
                    threadManager.mergeData(transactionVisitor, overdueVisitor);
                }
                AgentTransactionTree transactionRoot = transactionVisitor.getResult();
                checkSamplingAndAverages(transactionRoot);

                if (transactionInfos.size() > MAX_TRANSACTIONS) {
                    transactionInfos.clear();
                }
                if (stringPool.size() > MAX_STRING_POOL) {
                    stringPool.clear();
                }
                removeThreads(deadThreadManagers);

                return new Data(transactionRoot, overdueVisitor.getResult());
            });
        } catch (Throwable e) {
            Logger.log(Subsystem.COMMON, 0, true, e);
            return new Data(new AgentTransactionTree(), new AgentTransactionTree());
        } finally {
            lastSnapshotTimestamp = snapshotTimeStamp;
            lastSnapshotNanoTime = System.nanoTime();
        }
    }

    private void checkSamplingAndAverages(AgentTransactionTree tree) {
        tree.visit(new Visitor<AgentTransactionTree>() {
            @Override
            public boolean preVisit(AgentTransactionTree tree) {
                if (tree.getInfo() instanceof PolicyTransactionInfo) {
                    PolicyTransactionInfo policyTransactionInfo = (PolicyTransactionInfo)tree.getInfo();
                    policyTransactionInfo.addInvocations(tree.getCount(), tree.getTime());
                }
                return true;
            }

            @Override
            public void postVisit(AgentTransactionTree tree) {
            }
        });

        for (AgentTransactionInfo transactionInfo : transactionInfos.values()) {
            if (transactionInfo instanceof PolicyTransactionInfo) {
                PolicyTransactionInfo policyTransactionInfo = (PolicyTransactionInfo)transactionInfo;
                policyTransactionInfo.updateAverage();
            }
        }

    }

    public void virtualThreadEnd() {
        ThreadManager threadManager = threadManagerLocal.get();
        if (threadManager != null) {
            synchronized (threads) {
                deadVirtualThreadAggregator.addVirtual(threadManager);
                threads.remove(threadManager);
            }
        }
    }

    public static class Data {
        AgentTransactionTree transactionTree;
        AgentTransactionTree overdueTree;

        public Data(AgentTransactionTree transactionTree, AgentTransactionTree overdueTree) {
            this.transactionTree = transactionTree;
            this.overdueTree = overdueTree;
        }

        public AgentTransactionTree getTransactionTree() {
            return transactionTree;
        }

        public AgentTransactionTree getOverdueTree() {
            return overdueTree;
        }
    }

    private ThreadLocal<ThreadManager> threadManagerLocal = new ThreadLocal<>();

    public ThreadManager getThreadManager() {
        ThreadManager threadManager = threadManagerLocal.get();
        if (threadManager == null) {
            Thread thread = Thread.currentThread();
            threadManager = new ThreadManager(thread);
            synchronized (threads) {
                if (!detached) {
                    threads.add(threadManager);
                }
            }
            threadManagerLocal.set(threadManager);
        }
        return threadManager;
    }

    public void reset(long lastSnapshotTimestamp) {
        if (lastSnapshotTimestamp != this.lastSnapshotTimestamp || lastSnapshotTimestamp == -1) {
            getAndResetData(lastSnapshotTimestamp);
        }
    }


    public static class DeadVirtualThreadAggregator {
        MergeVisitor transactionVisitor;
        MergeVisitor overdueVisitor;

        public DeadVirtualThreadAggregator() {
            init();
        }

        public void addVirtual(ThreadManager threadManager) {
            threadManager.mergeData(transactionVisitor, overdueVisitor);
        }

        public void init() {
            transactionVisitor = new MergeVisitor(true);
            overdueVisitor = new MergeVisitor(false);
        }
    }
}
