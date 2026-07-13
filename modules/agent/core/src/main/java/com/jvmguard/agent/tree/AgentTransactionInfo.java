package com.jvmguard.agent.tree;

import com.jvmguard.agent.comm.AgentSerializable;
import com.jvmguard.agent.comm.CommunicationContext;
import com.jvmguard.agent.config.transactions.TransactionType;
import com.jvmguard.agent.util.JvmGuardUtil;
import org.jetbrains.annotations.NotNull;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Objects;

public class AgentTransactionInfo implements AgentSerializable, Comparable<AgentTransactionInfo> {
    private String name;
    private int transactionTypeId;
    private long id;

    public static AgentTransactionInfo create(String name, int transactionTypeId, long id) {
        return new PolicyTransactionInfo(name, transactionTypeId, id);
    }

    public AgentTransactionInfo() {
    }

    protected AgentTransactionInfo(String name, int transactionTypeId, long id) {
        this.name = name;
        this.transactionTypeId = transactionTypeId;
        this.id = id;
    }

    public AgentTransactionInfo init(String name, int transactionTypeId) {
        this.name = name;
        this.transactionTypeId = transactionTypeId;
        return this;
    }

    public String getName() {
        return name;
    }

    public TransactionType getTransactionType() {
        return TransactionType.fromId(transactionTypeId);
    }

    public int getTransactionTypeId() {
        return transactionTypeId;
    }

    public long getId() {
        return id;
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AgentTransactionInfo)) {
            return false;
        }

        AgentTransactionInfo that = (AgentTransactionInfo)o;

        if (transactionTypeId != that.transactionTypeId) {
            return false;
        }
        if (!Objects.equals(name, that.name)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + transactionTypeId;
        return result;
    }

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws IOException {
        id = in.readLong();
        name = in.readUTF();
        transactionTypeId = in.readShort();
    }

    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws IOException {
        out.writeLong(id);
        if (name == null) {
            out.writeUTF("<null>");
        } else if (name.length() > 10000) {
            out.writeUTF(name.substring(0, 10000));
        } else {
            out.writeUTF(name);
        }
        out.writeShort(transactionTypeId);
    }

    @Override
    public int compareTo(@NotNull AgentTransactionInfo o) {
        int val = transactionTypeId - o.transactionTypeId;
        if (val == 0) {
            val = JvmGuardUtil.compareNullable(name, o.name);
        }
        return val;
    }

    public long getAverage() {
        return -1;
    }
}
