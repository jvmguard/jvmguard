package dev.jvmguard.agent.util;

import org.jetbrains.annotations.NotNull;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class ChecksumOutputStream extends FilterOutputStream {

    private Checksum checksum;

    public ChecksumOutputStream(OutputStream os) {
        super(os);
        checksum = new CRC32();
        checksum.reset();
    }

    @Override
    public void write(byte @NotNull [] b, int off, int len) throws IOException {
        out.write(b, off, len);
        checksum.update(b, off, len);
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
        checksum.update(b);
    }


    public long getChecksum() {
        return checksum.getValue();
    }

}