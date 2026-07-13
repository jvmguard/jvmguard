package com.jvmguard.agent.comm;

import com.jvmguard.agent.util.ChecksumOutputStream;
import com.jvmguard.agent.base.logging.Subsystem;
import com.jvmguard.agent.util.Logger;
import com.jvmguard.agent.util.PumpingInputStream;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class BlobHelper {
    private static final int BUFFER_SIZE = 1024 * 24;
    private static final int GZIP_TRANSFER_LENGTH = -1;

    public static TransferAction writeBlob(DataOutputStream out, String errorMessage, File file, boolean compress) throws IOException {
        TransferAction action = TransferAction.ERROR;
        if (errorMessage != null) {
            out.writeInt(action.ordinal());
            out.writeUTF(errorMessage);
        } else {
            action = TransferAction.CONTENT;
            //noinspection ConstantValue
            out.writeInt(action.ordinal());
            long inputLength = file.length();
            out.writeLong(compress ? GZIP_TRANSFER_LENGTH : inputLength);
            out.writeLong(inputLength);
            try (DataInputStream fileIn = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
                ChecksumOutputStream checkedOut;
                if (compress) {
                    ChunkedOutputStream chunkedOutputStream = new ChunkedOutputStream(out);
                    checkedOut = new ChecksumOutputStream(chunkedOutputStream);
                    GZIPOutputStream gzippedOut = new GZIPOutputStream(checkedOut);
                    copyBytes(inputLength, fileIn, gzippedOut);
                    gzippedOut.finish();
                    chunkedOutputStream.finish();
                } else {
                    checkedOut = new ChecksumOutputStream(out);
                    copyBytes(inputLength, fileIn, checkedOut);
                }
                checkedOut.flush();
                out.writeLong(checkedOut.getChecksum());
            }
        }
        return action;
    }

    public static BlobResult readBlob(DataInputStream in, File tempDir) throws IOException {
        BlobResult blobResult = new BlobResult();

        blobResult.action = TransferAction.values()[in.readInt()];
        if (blobResult.action == TransferAction.ERROR) {
            blobResult.errorMessage = in.readUTF();
        } else if (blobResult.action == TransferAction.CONTENT) {
            long bytesToBeRead = in.readLong();
            blobResult.uncompressedLength = in.readLong();
            ChecksumOutputStream out;
            try {
                blobResult.file = File.createTempFile("jf_tr", ".snap", tempDir);
                out = new ChecksumOutputStream(new BufferedOutputStream(new FileOutputStream(blobResult.file)));
            } catch (IOException e) {
                Logger.log(Subsystem.COMMUNICATION, 1, true, e);
                blobResult.file = null;
                out = new ChecksumOutputStream(new BufferedOutputStream(new NullOutputStream()));
                blobResult.errorMessage = "could not create temporary file in " + tempDir + ": " + e;
            }
            if (bytesToBeRead == GZIP_TRANSFER_LENGTH) {
                ChunkedInputStream inputStream = new ChunkedInputStream(in);
                copyBytes(bytesToBeRead, new DataInputStream(inputStream), out);
                inputStream.finish();
            } else {
                copyBytes(bytesToBeRead, in, out);
            }
            out.close();
            long remoteCrc = in.readLong();
            if (remoteCrc != out.getChecksum()) {
                blobResult.errorMessage = "crc error: " + remoteCrc + ", local: " + out.getChecksum();
            }
        } else {
            String fileName = in.readUTF();
            blobResult.errorMessage = "cannot read temp file " + fileName;
        }
        return blobResult;
    }

    private static void copyBytes(final long length, DataInputStream din, OutputStream os) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];

        if (length == GZIP_TRANSFER_LENGTH) {
            PumpingInputStream pumpingInputStream = new PumpingInputStream(din, os);
            GZIPInputStream gzipInputStream = new GZIPInputStream(pumpingInputStream);
            //noinspection StatementWithEmptyBody
            while (gzipInputStream.read(buffer) > -1) {
                // unzipping to find the undefined end of the gzip stream while pumping the original zipped data to os
            }
        } else {
            long bytesToBeRead = length;
            while (bytesToBeRead > 0) {
                int readNow = (int)Math.min(bytesToBeRead, BUFFER_SIZE);
                din.readFully(buffer, 0, readNow);
                os.write(buffer, 0, readNow);
                bytesToBeRead -= readNow;
            }
        }
    }

    //important: keep order
    public enum TransferAction {
        CONTENT,
        ERROR,
        FILENAME,
        FILENAME_UNCOMPRESSED
    }

    public static class BlobResult {
        private String errorMessage;
        private File file;
        private TransferAction action;
        private long uncompressedLength;

        public String getErrorMessage() {
            return errorMessage;
        }

        public File getFile() {
            return file;
        }

        public TransferAction getAction() {
            return action;
        }

        public long getUncompressedLength() {
            return uncompressedLength;
        }
    }

    public static class NullOutputStream extends OutputStream {

        public NullOutputStream() {
        }

        @Override
        public void write(int b) throws IOException {
        }

        @Override
        public void write(byte @NotNull [] b) throws IOException {
        }

        @Override
        public void write(byte @NotNull [] b, int off, int len) throws IOException {
        }
    }
}
