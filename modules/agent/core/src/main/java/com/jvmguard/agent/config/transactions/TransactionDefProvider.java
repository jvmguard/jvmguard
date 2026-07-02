package com.jvmguard.agent.config.transactions;

import java.util.List;

public interface TransactionDefProvider {
    List<TransactionDef> getTransactionDefs();
}
