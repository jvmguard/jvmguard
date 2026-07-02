package com.jvmguard.agent.helper;

import com.jvmguard.agent.JvmGuardAgent;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

public class DumpCreator {
    private static final String THREAD_PRINT = "threadPrint";

    private static final String[] THREAD_PRINT_PARAMETERS = new String[] {"-l"};

    private static final boolean M_BEAN = DiagnosticMbeanHandler.isMBeanAvailable(THREAD_PRINT);

    public static synchronized AccessibleByteArrayOutputStream threadDump() {
        final AccessibleByteArrayOutputStream bout = new AccessibleByteArrayOutputStream();
        if (M_BEAN) {
            try {
                String threads = DiagnosticMbeanHandler.invoke(THREAD_PRINT, THREAD_PRINT_PARAMETERS);
                Writer writer = new PrintWriter(new OutputStreamWriter(bout, StandardCharsets.UTF_8));
                writer.append(threads);
                writer.flush();
            } catch (Throwable e) {
                JvmGuardAgent.log(e);
            }
        }
        return bout;
    }

    public static class AccessibleByteArrayOutputStream extends ByteArrayOutputStream {
        public byte[] getBuffer() {
            return buf;
        }
    }
}
