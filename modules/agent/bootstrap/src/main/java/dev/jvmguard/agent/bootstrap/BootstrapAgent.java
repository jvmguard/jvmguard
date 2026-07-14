package dev.jvmguard.agent.bootstrap;

import dev.jvmguard.agent.AgentInit;

import java.lang.instrument.Instrumentation;

public class BootstrapAgent {

    public static void premain(String agentArgs, Instrumentation inst) {
        init(false, agentArgs, inst);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        init(true, agentArgs, inst);
    }

    private static void init(boolean attach, String agentArgs, Instrumentation inst) {
        try {
            AgentLocation agentLocation = new AgentLocation();

            if (agentLocation.getJarFile() != null) {
                inst.appendToBootstrapClassLoaderSearch(agentLocation.getJarFile());
                AgentInit.init(attach, agentArgs, inst, agentLocation.getJavaAgentJar(), agentLocation.getBootstrapBaseDir(), agentLocation.getJvmGuardUserDir(), agentLocation.getAgentUserDir());
            } else {
                System.err.println("jar file not found in " + agentLocation.getJavaAgentJar());
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

}
