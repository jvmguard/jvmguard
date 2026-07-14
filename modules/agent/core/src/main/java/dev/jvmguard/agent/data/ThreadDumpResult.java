package dev.jvmguard.agent.data;

import dev.jvmguard.agent.RequestSession;
import dev.jvmguard.agent.comm.CommunicationContext;
import dev.jvmguard.agent.helper.DumpCreator;
import dev.jvmguard.agent.helper.DumpCreator.AccessibleByteArrayOutputStream;
import dev.jvmguard.agent.thread.ThreadManager;
import dev.jvmguard.agent.util.JvmGuardUtil;
import dev.jvmguard.agent.util.reflection.FieldInfo;
import dev.jvmguard.agent.util.reflection.ReflectionUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map.Entry;

public class ThreadDumpResult extends BaseResult implements FileMover {
    private static final boolean IS_32_BIT = "32".equals(System.getProperty("sun.arch.data.model"));

    private static FieldInfo eetopField;
    private byte[] data;

    public byte[] getData() {
        return data;
    }

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws IOException {
        data = new byte[in.readInt()];
        in.readFully(data);
    }

    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws IOException {
        AccessibleByteArrayOutputStream bout = DumpCreator.threadDump();

        PrintWriter pw = null;

        if (bout.size() == 0) {
            doFallbackDump(bout);
        }

        for (ThreadManager threadManager : RequestSession.getInstance().getThreads()) {
            if (threadManager.isOverdue()) {
                if (pw == null) {
                    pw = new PrintWriter(new OutputStreamWriter(bout, StandardCharsets.UTF_8));
                    pw.println("-------------- Overdue Threads -------------------");
                    pw.println();
                }
                Thread thread = threadManager.getThread();
                printHeader(pw, thread);
            }
        }
        if (pw != null) {
            pw.println();
            pw.close();
        }

        out.writeInt(bout.size());
        out.write(bout.getBuffer(), 0, bout.size());
    }

    private void printHeader(PrintWriter pw, Thread thread) {
        pw.println("\"" + thread.getName() + "\" tid=" + getNativeId(thread) + ", javaId=" + thread.getId());
    }

    private void doFallbackDump(AccessibleByteArrayOutputStream bout) {
        PrintWriter pw = new PrintWriter(bout);

        for (Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
            printHeader(pw, entry.getKey());
            for (StackTraceElement stackTraceElement : entry.getValue()) {
                pw.println(stackTraceElement);
            }
            pw.println();
        }

        pw.close();
    }

    @Override
    public void moveToFile(File file) throws IOException {
        OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
        JvmGuardUtil.pumpStream(new ByteArrayInputStream(data), out, data.length);
        out.close();
    }

    @Override
    public long getUncompressedLength() {
        return data.length;
    }

    @Override
    public String toString() {
        return "ThreadDumpResult{" +
            "data=" + new String(data) +
            '}';
    }

    private static String getNativeId(Thread thread) {
        if (eetopField == null) {
            eetopField = ReflectionUtil.getAccessibleField(Thread.class, "eetop");
        }
        String nativeId = "";
        if (eetopField.field != null) {
            try {
                nativeId = Long.toHexString((Long)eetopField.field.get(thread));
                nativeId = padLeft(nativeId, '0', IS_32_BIT ? 8 : 16);
                nativeId = "0x" + nativeId;
            } catch (IllegalAccessException ignored) {
            }
        }
        return nativeId;
    }

    @SuppressWarnings("JavaExistingMethodCanBeUsed")
    private static String padLeft(String val, char padChar, int length) {
        StringBuilder buffer = new StringBuilder(length);
        for (int i = 0; i < length - val.length(); i++) {
            buffer.append(padChar);
        }
        buffer.append(val, 0, Math.min(length, val.length()));
        return buffer.toString();
    }
}
