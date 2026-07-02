package com.jvmguard.agent.data;

import com.jvmguard.agent.base.logging.Subsystem;
import com.jvmguard.agent.comm.CommunicationContext;
import com.jvmguard.agent.util.Logger;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public abstract class OrderedSnapshotTransferResult extends SnapshotTransferResult {
    protected volatile Future<File> future;

    protected abstract Future<File> getFuture(CommunicationContext context);
    protected abstract String getNullErrorMessage();

    @Override
    public void prepareDeferredDirect(CommunicationContext context) {
        future = getFuture(context);
    }

    @Override
    public void prepareDeferredLater(CommunicationContext context) {
        try {
            file = future.get();
            if (file == null) {
                errorMessage = getNullErrorMessage();
            }
        } catch (Throwable e) {
            if (e instanceof ExecutionException && e.getCause() != null) {
                e = e.getCause();
            }
            errorMessage = e.toString();
            Logger.log(Subsystem.COMMON, 0, true, e);
        }
    }


}
