package dev.jvmguard.agent.tree;

import dev.jvmguard.agent.comm.CommunicationContext;
import dev.jvmguard.agent.config.transactions.TransactionType;
import dev.jvmguard.agent.util.JvmGuardUtil;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.jetbrains.annotations.NotNull;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class AgentTransactionTree extends AbstractTransactionTree<AgentTransactionInfo, AgentTransactionTree> {
    private int hashCode;

    public AgentTransactionTree() {
    }

    public AgentTransactionTree(AgentTransactionTree parent, AgentTransactionInfo methodId, String type) {
        super(parent, methodId, type);
        hashCode = super.hashCode();
    }

    @Override
    public String getName() {
        return getInfo() != null ? getInfo().getName() : null;
    }

    @Override
    public TransactionType getTransactionType() {
        return getInfo() != null ? getInfo().getTransactionType() : null;
    }

    public AgentTransactionTree init(AgentTransactionInfo info, String policyTypeString) {
        this.info = info;
        this.policyTypeString = policyTypeString;
        hashCode = super.hashCode();
        return this;
    }

    @Override
    protected void writeEntry(CommunicationContext context, DataOutputStream out, AgentTransactionInfo methodId) throws IOException {
        out.writeBoolean(methodId != null);
        if (methodId != null) {
            out.writeLong(methodId.getId());
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected AgentTransactionInfo readEntry(CommunicationContext context, DataInputStream in) throws IOException {
        if (in.readBoolean()) {
            Long2ObjectOpenHashMap<AgentTransactionInfo> map = (Long2ObjectOpenHashMap<AgentTransactionInfo>)context.getProperty(CommunicationContext.PROPERTY_LOOKUP_MAP);
            return map.get(in.readLong());
        }
        return null;
    }

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws IOException {
        boolean rootTree = context.getProperty(CommunicationContext.PROPERTY_STRING_LOOKUP_MAP) == null;
        if (rootTree) {
            Long2ObjectOpenHashMap<AgentTransactionInfo> idToInfo = new Long2ObjectOpenHashMap<>();
            while (in.readBoolean()) {
                AgentTransactionInfo info = new AgentTransactionInfo();
                info.read(context, in);
                idToInfo.put(info.getId(), info);
            }
            Map<Integer, String> idToString = new HashMap<>();
            int count = in.readInt();
            for (int i = 0; i < count; i++) {
                int id = in.readInt();
                String value = in.readUTF();
                idToString.put(id, value);
            }

            context.setProperty(CommunicationContext.PROPERTY_LOOKUP_MAP, idToInfo);
            context.setProperty(CommunicationContext.PROPERTY_STRING_LOOKUP_MAP, idToString);
        }
        try {
            super.read(context, in);
        } finally {
            if (rootTree) {
                if (!Boolean.TRUE.equals(context.getProperty(CommunicationContext.PROPERTY_KEEP_LOOKUP_MAP))) {
                    context.setProperty(CommunicationContext.PROPERTY_LOOKUP_MAP, null);
                }
                context.setProperty(CommunicationContext.PROPERTY_STRING_LOOKUP_MAP, null);
            }
        }
    }

    @Override
    public void write(final CommunicationContext context, final DataOutputStream out) throws IOException {
        boolean rootTree = context.getProperty(CommunicationContext.PROPERTY_STRING_LOOKUP_MAP) == null;

        if (rootTree) {
            final LongOpenHashSet sentIdSet = new LongOpenHashSet();
            final Map<String, Integer> stringToId = new HashMap<>();
            visit(new Visitor<AgentTransactionTree>() {
                int nextStringId = 1;

                @Override
                public boolean preVisit(AgentTransactionTree transactionTree) throws Exception {
                    AgentTransactionInfo transactionInfo = transactionTree.getInfo();
                    if (transactionInfo != null && sentIdSet.add(transactionInfo.getId())) {
                        out.writeBoolean(true);
                        transactionInfo.write(context, out);
                    }
                    if (transactionTree.getPolicyTypeString() != null && PolicyType.getSpecialByString(transactionTree.getPolicyTypeString()) == null &&
                        !stringToId.containsKey(transactionTree.getPolicyTypeString())) {
                        stringToId.put(transactionTree.getPolicyTypeString(), nextStringId++);
                    }
                    return true;
                }

                @Override
                public void postVisit(AgentTransactionTree tree) throws Exception {
                }
            });
            out.writeBoolean(false);

            out.writeInt(stringToId.size());
            for (Entry<String, Integer> entry : stringToId.entrySet()) {
                out.writeInt(entry.getValue());
                JvmGuardUtil.writeCappedUTF(out, entry.getKey());
            }
            context.setProperty(CommunicationContext.PROPERTY_STRING_LOOKUP_MAP, stringToId);
            if (Boolean.TRUE.equals(context.getProperty(CommunicationContext.PROPERTY_KEEP_SENT_ID_SET))) {
                context.setProperty(CommunicationContext.PROPERTY_SENT_ID_SET, sentIdSet);
            }
        }
        try {
            super.write(context, out);
        } finally {
            if (rootTree) {
                context.setProperty(CommunicationContext.PROPERTY_STRING_LOOKUP_MAP, null);
            }
        }
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    protected AgentTransactionTree createChildInt(AgentTransactionTree lookupTree) {
        if (lookupTree == null) {
            return new AgentTransactionTree(this, null, null);
        } else {
            return new AgentTransactionTree(this, lookupTree.getInfo(), lookupTree.getPolicyTypeString());
        }
    }

    public void putChild(AgentTransactionTree child) {
        if (children == null) {
            children = new HashMap<>();
        }
        child.setParent(this);
        children.put(child, child);
    }

    @Override
    public String toString() {
        return getInfo() + ": " + getCount() + ", " + getTime();
    }

    @Override
    public int compareTo(@NotNull AgentTransactionTree o) {
        int val = JvmGuardUtil.compareNullable(getInfo(), o.getInfo());
        if (val == 0) {
            val = JvmGuardUtil.compareNullable(getPolicyTypeString(), o.getPolicyTypeString());
        }
        return val;
    }

    @SuppressWarnings("UnusedDeclaration")
    public static class PrintVisitor extends AbstractTransactionTreePrintVisitor<AgentTransactionTree> {
        public PrintVisitor(Appendable appendable) {
            super(appendable);
        }

        @Override
        protected List<String> getAdditionalLines(AgentTransactionTree tree) {
            return super.getAdditionalLines(tree);
        }
    }

}
