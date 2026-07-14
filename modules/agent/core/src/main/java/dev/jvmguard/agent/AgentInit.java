package dev.jvmguard.agent;

import java.io.File;
import java.lang.instrument.Instrumentation;

public class AgentInit {
    public static final String AGENT_JAR = "agent.jar";
    public static final String BUILD_VERSION_ATTRIBUTE_NAME = "Build-Version";

    private static final String AGENT_V1_DIR = "agent";

    public static void init(boolean attach, String agentArgs, Instrumentation inst, File javaAgentJar, File bootstrapBaseDir, File jvmguardUserDir) {
        init(attach, agentArgs, inst, javaAgentJar, bootstrapBaseDir, jvmguardUserDir, new File(jvmguardUserDir, AGENT_V1_DIR));
    }

    public static void init(boolean attach, String agentArgs, Instrumentation inst, File javaAgentJar, File bootstrapBaseDir, File jvmguardUserDir, File agentUserDir) {
        JvmGuardAgent.init(attach, agentArgs, inst, javaAgentJar, bootstrapBaseDir, jvmguardUserDir, agentUserDir);
    }
}
