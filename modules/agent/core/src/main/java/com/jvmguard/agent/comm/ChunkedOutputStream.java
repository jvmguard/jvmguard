package com.jvmguard.agent.comm;

import org.jetbrains.annotations.NotNull;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.CRC32;

public class ChunkedOutputStream extends OutputStream {
    public static final int MAGIC_NUMBER = 0xBCC0FF63;
    public static final int MAGIC_NUMBER_END = 0xBCC0FF64;

    private DataOutputStream out;
    private CRC32 crc32 = new CRC32();

    protected byte[] buf = new byte[1024 * 16];
    protected int count;

    public ChunkedOutputStream(DataOutputStream out) {
        this.out = out;
    }

    private void flushBuffer() throws IOException {
        if (count > 0) {
            writeChunk(buf, 0, count);
            count = 0;
        }
    }

    @Override
    public synchronized void write(int b) throws IOException {
        if (count >= buf.length) {
            flushBuffer();
        }
        buf[count++] = (byte)b;
    }

    @Override
    public synchronized void write(byte @NotNull [] b, int off, int len) throws IOException {
        if (len >= buf.length) {
            flushBuffer();
            writeChunk(b, off, len);
            return;
        }
        if (len > buf.length - count) {
            flushBuffer();
        }
        System.arraycopy(b, off, buf, count, len);
        count += len;
    }

    private void writeChunk(byte[] b, int off, int len) throws IOException {
        crc32.reset();
        crc32.update(b, off, len);
        out.writeInt(MAGIC_NUMBER);
        out.writeInt(len);
        out.writeInt(len);
        out.writeLong(crc32.getValue());
        out.write(b, off, len);
        out.flush();
    }

    @Override
    public synchronized void flush() throws IOException {
        flushBuffer();
        out.flush();
    }

    public synchronized void finish() throws IOException {
        flushBuffer();
        out.writeInt(MAGIC_NUMBER_END);
        out.flush();
    }
}
