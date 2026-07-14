package dev.jvmguard.agent.tree;

import dev.jvmguard.agent.comm.CommunicationContext;
import dev.jvmguard.agent.config.transactions.TransactionType;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public abstract class AbstractTransactionTree<M, T extends AbstractTransactionTree<M, T>> extends Tree<T> {

    protected String policyTypeString = PolicyType.NORMAL.getTypeString();

    protected long time;
    protected long count;

    protected M info;

    public abstract String getName();
    public abstract TransactionType getTransactionType();

    public AbstractTransactionTree() {
    }

    public AbstractTransactionTree(T parent, M info, String policyTypeString) {
        super(parent);
        this.info = info;
        if (policyTypeString != null) {
            this.policyTypeString = policyTypeString;
        }
    }

    public AbstractTransactionTree(M info, String policyTypeString) {
        this(null, info, policyTypeString);
    }

    public M getInfo() {
        return info;
    }

    public String getPolicyTypeString() {
        return policyTypeString;
    }

    public PolicyType getPolicyType() {
        PolicyType policyType = PolicyType.getSpecialByString(policyTypeString);
        if (policyType == null) {
            policyType = PolicyType.ERROR;
        }
        return policyType;
    }

    public String getVerbosePolicyType(boolean lowerCaseConstants) {
        return getVerbosePolicyType(policyTypeString, lowerCaseConstants, false);
    }

    public static String getVerbosePolicyType(String policyTypeString, boolean lowerCaseConstants, boolean nullForNormal) {
        PolicyType policyType = PolicyType.getSpecialByString(policyTypeString);
        if (policyType == null) {
            return policyTypeString;
        } else {
            if (nullForNormal && policyType == PolicyType.NORMAL) {
                return null;
            } else {
                return lowerCaseConstants ? policyType.toString().toLowerCase() : policyType.toString();
            }
        }
    }


    protected abstract void writeEntry(CommunicationContext context, DataOutputStream out, M info) throws IOException;
    protected abstract M readEntry(CommunicationContext context, DataInputStream in) throws IOException;

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws IOException {
        super.read(context, in);
        info = readEntry(context, in);
        time = in.readLong();
        count = in.readLong();

        int typeId = in.readInt();
        PolicyType policyType = PolicyType.getSpecialById(typeId);
        if (policyType != null) {
            policyTypeString = policyType.getTypeString();
        } else {
            Map idToDescription = (Map)context.getProperty(CommunicationContext.PROPERTY_STRING_LOOKUP_MAP);
            policyTypeString = (String)idToDescription.get(typeId);
        }
    }

    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws IOException {
        super.write(context, out);
        writeEntry(context, out, info);
        out.writeLong(time);
        out.writeLong(count);

        PolicyType policyType = PolicyType.getSpecialByString(policyTypeString);
        if (policyType != null) {
            out.writeInt(policyType.getId());
        } else {
            Map descriptionToId = (Map)context.getProperty(CommunicationContext.PROPERTY_STRING_LOOKUP_MAP);
            out.writeInt(policyTypeString == null ? 0 : (Integer)descriptionToId.get(policyTypeString));
        }
    }

    public long getTime() {
        return time;
    }

    public long getCount() {
        return count;
    }

    public long getInherentTime() {
        long ret = time;
        if (children != null) {
            for (T tree : children.keySet()) {
                ret -= tree.getTime();
            }
        }
        return ret;
    }

    public void addData(T addFromTree) {
        addCount(addFromTree.getCount());
        addTime(addFromTree.getTime());
    }

    public void addTime(long time) {
        this.time += time;
    }

    public void addCount(long count) {
        this.count += count;
    }

    public void addCountRecursive(long count) {
        addCount(count);
        if (getParent() != null) {
            getParent().addCountRecursive(count);
        }
    }

    public void addTimeRecursive(long time) {
        addTime(time);
        if (getParent() != null) {
            getParent().addTimeRecursive(time);
        }
    }

    public void clear() {
        time = 0;
        count = 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AbstractTransactionTree)) {
            return false;
        }

        AbstractTransactionTree that = (AbstractTransactionTree)o;

        if (!Objects.equals(info, that.info)) {
            return false;
        }
        if (!Objects.equals(policyTypeString, that.policyTypeString)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = policyTypeString != null ? policyTypeString.hashCode() : 0;
        result = 31 * result + (info != null ? info.hashCode() : 0);
        return result;
    }

    public enum PolicyType {
        NORMAL(".", "Normal", 0),
        PARTIAL(".p", "Partial", -1),
        SLOW(".s", "Slow", -2),
        VERY_SLOW(".v", "Very slow", -3),
        ERROR(null, "Error", Integer.MIN_VALUE);

        private final String typeString;
        private final String prefix;
        private final String verbose;
        private final int id;

        private static final Long2ObjectOpenHashMap<PolicyType> idToType = new Long2ObjectOpenHashMap<>();
        private static final Map<String, PolicyType> stringToType = new HashMap<>();

        static {
            for (PolicyType policyType : values()) {
                if (policyType.getTypeString() != null) {
                    idToType.put(policyType.getId(), policyType);
                    stringToType.put(policyType.getTypeString(), policyType);
                }
            }
        }

        PolicyType(String typeString, String verbose, int id) {
            this.typeString = typeString;
            this.verbose = verbose;
            this.id = id;
            prefix = "[" + verbose + "] ";
        }

        public String getTypeString() {
            return typeString;
        }

        public int getId() {
            return id;
        }

        public static PolicyType getSpecialById(long id) {
            return idToType.get(id);
        }

        public static PolicyType getSpecialByString(String str) {
            return stringToType.get(str);
        }

        public String getPrefix() {
            return prefix;
        }

        @Override
        public String toString() {
            return verbose;
        }
    }

    public static class AbstractTransactionTreePrintVisitor<T extends AbstractTransactionTree<?, T>> extends PrintVisitor<T> {

        public AbstractTransactionTreePrintVisitor(Appendable appendable) {
            super(appendable);
        }

        @Override
        protected List<String> getAdditionalLines(T tree) {
            List<String> ret = new ArrayList<>();
            //noinspection StringEquality
            if (tree.getPolicyTypeString() != PolicyType.NORMAL.getTypeString()) {
                ret.add("TYPE: " + tree.getPolicyTypeString());
            }
            return ret;
        }

        @Override
        protected void appendInfo(Appendable appendable, T tree) throws IOException {
            appendable.append(String.valueOf(tree.getInfo() == null ? "root" : tree.getInfo())).append(": ");
            appendable.append(String.valueOf(tree.getCount())).append(" inv, ");
            appendable.append(String.valueOf(tree.getTime() / 1000 / 1000)).append(" ms");
        }
    }
}
