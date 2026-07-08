package com.jvmguard.agent.bootstrap;

import com.jvmguard.agent.AgentInit;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;

public class AgentLocation {
    private static final String AGENT_DIR = "agent";
    private static final String USER_HOME = "user.home";
    private static final String JVMGUARD_USER_DIR = ".jvmguard";

    private File bootstrapBaseDir;
    private File javaAgentJar;
    private JarFile jarFile;
    private File jvmguardUserDir;
    private File agentUserDir;

    public AgentLocation() throws IOException {
        bootstrapBaseDir = BootstrapAgentEnvironment.getAgentBaseDir();
        File agentDir = new File(bootstrapBaseDir, "lib");

        javaAgentJar = new File(agentDir, AgentInit.AGENT_JAR);
        if (javaAgentJar.isFile()) {
            jarFile = new JarFile(javaAgentJar);

            long defaultBuildVersion = 0;
            try {
                defaultBuildVersion = Long.parseLong(jarFile.getManifest().getMainAttributes().getValue(AgentInit.BUILD_VERSION_ATTRIBUTE_NAME));
            } catch (Throwable t) {
                if (BootstrapAgentEnvironment.DEBUG_BOOTSTRAP) {
                    t.printStackTrace();
                }
            }

            String userDirOverride = System.getProperty("jvmguard.userDirectory");
            if (userDirOverride != null) {
                jvmguardUserDir = new File(userDirOverride);
            } else {
                jvmguardUserDir = new File(System.getProperty(USER_HOME), JVMGUARD_USER_DIR);
            }
            agentUserDir = new File(new File(jvmguardUserDir, AGENT_DIR), BootstrapFileUtil.getHashedPath(getBootstrapBasePath()));

            long preferredAgentVersion = Long.getLong("jvmguard.preferredAgentVersion", -1);

            long newestUserVersion = -1;
            if (preferredAgentVersion != defaultBuildVersion) {
                File[] agentDirs = agentUserDir.listFiles();
                if (agentDirs != null) {
                    for (File file : agentDirs) {
                        if (file.isDirectory()) {
                            try {
                                long currentVersion = Long.parseLong(file.getName());
                                if (currentVersion == preferredAgentVersion) {
                                    newestUserVersion = preferredAgentVersion;
                                    defaultBuildVersion = 0;
                                    break;
                                } else if (currentVersion > newestUserVersion) {
                                    if (new File(file, AgentInit.AGENT_JAR).isFile()) {
                                        newestUserVersion = currentVersion;
                                    }
                                }
                            } catch (Throwable t) {
                                if (BootstrapAgentEnvironment.DEBUG_BOOTSTRAP) {
                                    System.err.println("for dir " + file);
                                    t.printStackTrace();
                                }
                            }
                        }
                    }
                }
            }

            if (newestUserVersion >= defaultBuildVersion) {
                javaAgentJar = new File(agentUserDir, newestUserVersion + File.separator + AgentInit.AGENT_JAR);
                jarFile.close();
                jarFile = new JarFile(javaAgentJar);
            } else if (defaultBuildVersion > 0) {
                copyInstalledAgentToUserHome(defaultBuildVersion);
            }

            if (BootstrapAgentEnvironment.DEBUG_BOOTSTRAP) {
                System.err.println("jvmguard bootstrap: loading agent jar " + javaAgentJar
                    + " (dist build version " + defaultBuildVersion + ", newest cached version " + newestUserVersion + ")");
            }
        }
    }

    private String getBootstrapBasePath() {
        try {
            return bootstrapBaseDir.getCanonicalPath();
        } catch (IOException e) {
            return bootstrapBaseDir.getAbsolutePath();
        }
    }

    private void copyInstalledAgentToUserHome(long defaultBuildVersion) {
        try {
            if (BootstrapAgentEnvironment.DEBUG_BOOTSTRAP) {
                System.err.println("copying agent to user home");
            }

            File defaultVersionUserDir = new File(agentUserDir, String.valueOf(defaultBuildVersion));

            if (!defaultVersionUserDir.exists()) {
                agentUserDir.mkdirs();
                File dirTempFile = File.createTempFile("agent", ".tmp", agentUserDir);
                File tempDir = new File(dirTempFile.getParentFile(), dirTempFile.getName() + ".dir");
                tempDir.mkdirs();
                if (tempDir.isDirectory()) {
                    BootstrapFileUtil.copyFile(javaAgentJar, new File(tempDir, javaAgentJar.getName()));
                    for (String platformDescriptor : BootstrapAgentEnvironment.getPlatformDescriptors()) {
                        BootstrapFileUtil.duplicateDirectory(new File(javaAgentJar.getParentFile(), platformDescriptor), tempDir);
                    }
                }
                if (!tempDir.renameTo(defaultVersionUserDir)) {
                    BootstrapFileUtil.deleteDirectory(tempDir);
                }
                if (!dirTempFile.delete()) {
                    dirTempFile.deleteOnExit();
                }
            }

            File defaultVersionAgentUserAgentJar = new File(defaultVersionUserDir, AgentInit.AGENT_JAR);
            if (defaultVersionUserDir.isDirectory() && defaultVersionAgentUserAgentJar.isFile()) {
                javaAgentJar = defaultVersionAgentUserAgentJar;
                jarFile.close();
                jarFile = new JarFile(javaAgentJar);

            }
        } catch (Throwable t) {
            if (BootstrapAgentEnvironment.DEBUG_BOOTSTRAP) {
                t.printStackTrace();
            }
        }
    }

    public File getBootstrapBaseDir() {
        return bootstrapBaseDir;
    }

    public File getJavaAgentJar() {
        return javaAgentJar;
    }

    public JarFile getJarFile() {
        return jarFile;
    }

    public File getJvmGuardUserDir() {
        return jvmguardUserDir;
    }

    public File getAgentUserDir() {
        return agentUserDir;
    }
}
