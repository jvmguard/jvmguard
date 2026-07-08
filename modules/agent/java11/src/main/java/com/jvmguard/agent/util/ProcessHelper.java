package com.jvmguard.agent.util;

import java.lang.ProcessHandle;

@SuppressWarnings("Since15")
public class ProcessHelper {

    public static long currentPid() {
        return ProcessHandle.current().pid();
    }
}
