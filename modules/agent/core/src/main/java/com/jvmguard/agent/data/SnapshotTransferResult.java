package com.jvmguard.agent.data;


import com.jvmguard.agent.comm.BlobHelper;
import com.jvmguard.agent.comm.BlobHelper.BlobResult;
import com.jvmguard.agent.comm.BlobHelper.TransferAction;
import com.jvmguard.agent.comm.CommunicationContext;
import com.jvmguard.agent.util.JvmGuardUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

public abstract class SnapshotTransferResult extends BaseResult implements FileMover {

    protected File file;
    protected String errorMessage;
    private boolean compressWhileMoving;
    private long uncompressedLength;

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws IOException {
        if (BlobHelper.writeBlob(context, out, errorMessage, file, isCompress()) == BlobHelper.TransferAction.CONTENT) {
            file.delete();
        }
    }

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws IOException {
        BlobResult blobResult = BlobHelper.readBlob(context, in, tempDir);
        errorMessage = blobResult.getErrorMessage();
        file = blobResult.getFile();
        if (file == null && (errorMessage == null || errorMessage.isEmpty())) {
            errorMessage = "Did not receive the snapshot file";
        }
        compressWhileMoving = blobResult.getAction() == TransferAction.FILENAME_UNCOMPRESSED;
        uncompressedLength = blobResult.getUncompressedLength();
    }

    @Override
    public void moveToFile(File dest) throws IOException {
        if (file != null && (compressWhileMoving || !file.renameTo(dest))) {
            JvmGuardUtil.copyFile(file, dest, compressWhileMoving);
            file.delete();
            file = dest;
        }
    }

    @Override
    public long getUncompressedLength() {
        return uncompressedLength;
    }

    protected boolean isCompress() {
        return true;
    }

    @Override
    public String toString() {
        return "SnapshotTransferResult{" +
            "file=" + file +
            ", errorMessage='" + errorMessage + '\'' +
            "} " + super.toString();
    }
}

