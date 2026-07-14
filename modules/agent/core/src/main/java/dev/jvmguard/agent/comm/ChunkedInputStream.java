package dev.jvmguard.agent.comm;

import org.jetbrains.annotations.NotNull;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.CRC32;

public class ChunkedInputStream extends InputStream {

    private DataInputStream in;
    private CRC32 crc32 = new CRC32();

    protected byte[] buf = new byte[1024 * 16];

    protected int count;
    protected int pos;
    protected boolean endReached = false;

    public ChunkedInputStream(DataInputStream in) {
        this.in = in;
    }

    @Override
    public int read() throws IOException {
        if (!checkChunk()) {
            return -1;
        }
        return buf[pos++] & 0xff;
    }

    @Override
    public int read(byte @NotNull [] b, int off, int len) throws IOException {
        if (len == 0) {
            return 0;
        }

        if (!checkChunk()) {
            return -1;
        }
        int usedLength = Math.min(len, count - pos);
        System.arraycopy(buf, pos, b, off, usedLength);
        pos += usedLength;
        return usedLength;
    }

    private boolean checkChunk() throws IOException {
        if (pos >= count) {
            readChunk();
            if (pos >= count) {
                return false;
            }
        }
        return true;
    }

    private void readChunk() throws IOException {
        if (endReached) {
            return;
        }
        int magic = in.readInt();
        if (magic == ChunkedOutputStream.MAGIC_NUMBER_END) {
            endReached = true;
            pos = 0;
            count = 0;
            return;
        }
        if (magic != ChunkedOutputStream.MAGIC_NUMBER) {
            throwException("magic does not match " + magic);
        }
        count = in.readInt();
        int count2 = in.readInt();
        if (count != count2) {
            throwException("count does not match " + count + ", " + count2);
        }
        long checksum = in.readLong();

        if (buf.length < count) {
            buf = new byte[count];
        }
        crc32.reset();
        in.readFully(buf, 0, count);
        crc32.update(buf, 0, count);
        if (checksum != crc32.getValue()) {
            throwException("checksum does not match " + checksum + ", " + crc32.getValue() + ", " + count);
        }
        pos = 0;
    }

    private void throwException(String text) throws IOException {
        try {
            close();
        } catch (IOException ignored) {
        }
        throw new IOException(text);
    }

    @Override
    public void close() throws IOException {
        buf = null;
        in.close();
        in = null;
    }

    public void finish() throws IOException {
        while (!endReached) {
            readChunk();
        }
    }
}
