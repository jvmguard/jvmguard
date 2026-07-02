package com.jvmguard.agent.data;

import com.jvmguard.agent.comm.CommunicationContext;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class JpsResult extends OrderedSnapshotTransferResult {

    // The fine-grained CPU recording action no longer records JVMTI data in the agent. The command is kept
    // so the trigger action stays wired end to end until it is replaced by the full JProfiler action.
    @Override
    protected Future<File> getFuture(CommunicationContext context) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected String getNullErrorMessage() {
        return "Fine-grained CPU recording is not available in this agent";
    }
}
