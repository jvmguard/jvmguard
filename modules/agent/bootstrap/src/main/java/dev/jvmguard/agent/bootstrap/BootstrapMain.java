package dev.jvmguard.agent.bootstrap;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

public class BootstrapMain {
    public static void main(String[] args) {
        try {
            AgentLocation agentLocation = new AgentLocation();

            if (agentLocation.getJarFile() != null) {
                @SuppressWarnings("resource")
                URLClassLoader urlClassLoader = new URLClassLoader(new URL[]{ agentLocation.getJavaAgentJar().toURI().toURL() });
                urlClassLoader.loadClass("dev.jvmguard.agent.tools.ToolsMain").getMethod("main", String[].class, File.class, File.class, File.class).
                    invoke(null, args, agentLocation.getJavaAgentJar(), agentLocation.getBootstrapBaseDir(), agentLocation.getJvmGuardUserDir());
            } else {
                System.err.println("jar file not found in " + agentLocation.getJavaAgentJar());
                System.exit(1);
            }
        } catch (Throwable e) {
            System.err.println("An error occurred during initialisation:");
            e.printStackTrace();
            System.exit(1);
        }

    }
}
