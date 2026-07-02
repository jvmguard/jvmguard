package com.jvmguard.agent.util;

import org.jetbrains.annotations.NotNull;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PumpingInputStream extends FilterInputStream {
    private final OutputStream out;

    public PumpingInputStream(InputStream in, OutputStream out) {
        super(in);
        this.out = out;
    }

    @Override
    public int read() throws IOException {
        int ret = super.read();
        if (ret != -1) {
            out.write(ret);
        }
        return ret;
    }

    @Override
    public int read(byte @NotNull [] b) throws IOException {
        int ret = super.read(b);
        if (ret > 0) {
            out.write(b, 0, ret);
        }
        return ret;
    }

    @Override
    public int read(byte @NotNull [] b, int off, int len) throws IOException {
        int ret = super.read(b, off, len);
        if (ret > 0) {
            out.write(b, off, ret);
        }
        return ret;
    }
}
