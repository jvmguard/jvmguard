package dev.jvmguard.agent.data;

import dev.jvmguard.agent.JvmGuardAgent;
import dev.jvmguard.agent.base.logging.Subsystem;
import dev.jvmguard.agent.RequestSession;
import dev.jvmguard.agent.RequestSession.Data;
import dev.jvmguard.agent.comm.CommunicationContext;
import dev.jvmguard.agent.parameter.DataSetParameter;
import dev.jvmguard.agent.tree.AbstractTransactionTree.AbstractTransactionTreePrintVisitor;
import dev.jvmguard.agent.tree.AgentTransactionTree;
import dev.jvmguard.agent.util.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class DataSetResult extends BaseResult {
    private AgentTransactionTree transactionTree;
    private AgentTransactionTree overdueTree;

    private long instanceId;
    private int realInterval;

    public AgentTransactionTree getTransactionTree() {
        return transactionTree;
    }

    public AgentTransactionTree getOverdueTree() {
        return overdueTree;
    }

    @Override
    public void prepareDeferredLater(CommunicationContext context) {
        DataSetParameter parameter = (DataSetParameter)context.getProperty(CommunicationContext.PROPERTY_PARAMETER);
        RequestSession requestSession = RequestSession.getInstance();

        long previousNanoTime = requestSession.getLastSnapshotNanoTime();
        Data data = requestSession.getAndResetData(parameter.getSnapshotTimeStamp());
        transactionTree = data.getTransactionTree();
        overdueTree = data.getOverdueTree();

        if (Logger.isEnabled(Subsystem.COMMUNICATION, 4)) {
            AbstractTransactionTreePrintVisitor<AgentTransactionTree> visitor = new AbstractTransactionTreePrintVisitor<>(null);
            transactionTree.visit(visitor);
            Logger.log(Subsystem.COMMUNICATION, 4, true, visitor);
        }
        realInterval = (int)((requestSession.getLastSnapshotNanoTime() - previousNanoTime) / 1000 / 1000);
    }


    @Override
    public void read(CommunicationContext context, DataInputStream in) throws IOException {
        instanceId = in.readLong();
        realInterval = in.readInt();
        transactionTree = new AgentTransactionTree();
        overdueTree = new AgentTransactionTree();

        context.setProperty(CommunicationContext.PROPERTY_KEEP_LOOKUP_MAP, true);
        try {
            transactionTree.read(context, in);
            overdueTree.read(context, in);
        } finally {
            context.setProperty(CommunicationContext.PROPERTY_LOOKUP_MAP, null);
            context.setProperty(CommunicationContext.PROPERTY_KEEP_LOOKUP_MAP, false);
        }
    }

    @Override
    public void write(final CommunicationContext context, final DataOutputStream out) throws IOException {
        out.writeLong(JvmGuardAgent.getInstanceId());
        out.writeInt(realInterval);
        context.setProperty(CommunicationContext.PROPERTY_KEEP_SENT_ID_SET, true);
        transactionTree.write(context, out);
        context.setProperty(CommunicationContext.PROPERTY_KEEP_SENT_ID_SET, false);
        context.setProperty(CommunicationContext.PROPERTY_SENT_ID_SET, null);

        overdueTree.write(context, out);
    }

    public int getRealInterval() {
        return realInterval;
    }

    @Override
    public String toString() {
        return "DataSetResult{" +
            "transactionTree=" + transactionTree +
            ", instanceId=" + instanceId +
            ", realInterval=" + realInterval +
            "} " + super.toString();
    }
}
