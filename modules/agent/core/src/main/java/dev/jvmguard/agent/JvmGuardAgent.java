package dev.jvmguard.agent;

import dev.jvmguard.agent.base.logging.Subsystem;
import dev.jvmguard.agent.comm.CodecTypes;
import dev.jvmguard.agent.comm.JvmGuardCommunication;
import dev.jvmguard.agent.comm.JvmGuardKeyManager;
import dev.jvmguard.agent.instrument.Java9ModuleHelperImplementation;
import dev.jvmguard.agent.instrument.Transformer;
import dev.jvmguard.agent.parameter.ConfigurationParameter;
import dev.jvmguard.agent.thread.OverdueChecker;
import dev.jvmguard.agent.util.Logger;
import dev.jvmguard.agent.util.LoggingHandler;
import dev.jvmguard.agent.util.ModuleHelper;
import dev.jvmguard.agent.util.ModuleHelper.ModuleHelperImplementation;
import dev.jvmguard.agent.util.Util;
import dev.jvmguard.mbean.data.MBeanManager;
import dev.jvmguard.mbean.data.MBeanManager.LogAdapter;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.security.SecureRandom;
import java.util.List;
import java.util.jar.JarFile;

public class JvmGuardAgent {
    private static final String SYSPROP_JVMGUARD_LOADED = "jvmguard.loaded";
    public static final boolean DEBUG = Boolean.getBoolean("jvmguard.debug");
    public static final String MESSAGE_PREFIX = "jvmguard> ";

    private static final String SERVER_PARAMETER = "server";
    private static final String SERVER_PORT_PARAMETER = "port";

    private static final String NAME_PARAMETER = "name";
    private static final String POOL_PARAMETER = "pool";
    private static final String GROUP_PARAMETER = "group";

    private static final String KEYSTORE_PARAMETER = "keyStore";

    private static String vmName;
    private static String vmPool;
    private static String vmGroup;
    private static File jvmguardUserDir;
    private static long buildVersion;
    private static File agentUserDir;

    private static boolean noStdout;
    private static long instanceId;

    public static final ThreadGroup AGENT_THREAD_GROUP = new ThreadGroup("_jvmguard");
    private static ModuleHelperImplementation moduleHelperImplementation;

    public static long getInstanceId() {
        return instanceId;
    }

    public static String getVmName() {
        return vmName;
    }

    public static String getVmPool() {
        return vmPool;
    }

    public static String getVmGroup() {
        return vmGroup;
    }

    @SuppressWarnings("unused")
    public static void init(boolean attach, String agentArgs, Instrumentation inst, File javaAgentJar, File bootstrapBaseDir, File jvmguardUserDir, File agentUserDir) {
        CodecTypes.registerAll();
        AgentProperties.init(agentArgs, jvmguardUserDir);
        noStdout = AgentProperties.getBoolean("noStdout");

        checkWindowsTimerWorkaround();
        if (Boolean.getBoolean(SYSPROP_JVMGUARD_LOADED)) {
            println("Already loaded. Please remove superfluous -javaagent option.");
            return;
        }
        System.setProperty(SYSPROP_JVMGUARD_LOADED, "true");
        JvmGuardAgent.agentUserDir = agentUserDir;
        JvmGuardAgent.jvmguardUserDir = jvmguardUserDir;
        initModuleHelperImplementation(inst);

        try {
            List<String> arguments = getVmArguments();
            boolean pool = false;

            initVmIdentifier();
            addRmiExports();

            vmName = AgentProperties.getProperty(NAME_PARAMETER, "");
            String loggingName = vmName;
            vmPool = AgentProperties.getProperty(POOL_PARAMETER, "");
            if (!vmPool.isEmpty()) {
                pool = true;
                loggingName = vmPool;
                if (!vmName.isEmpty()) {
                    println("ERROR: Only a name or a pool can be specified. The pool parameter will be used.");
                    vmName = "";
                }
            }
            initLogging(loggingName);
            vmGroup = AgentProperties.getProperty(GROUP_PARAMETER, "");

            Transformer.getInstance().initFeatures();

            // Initialize JvmGuardCommunication up front, to avoid a class init monitor deadlock when
            // the communication would be initialized lazily from inside a transform callback
            JvmGuardCommunication.ensureInitialized();

            inst.addTransformer(Transformer.getInstance().setInstrumentation(inst).getWrapper(), true);
            if (inst.isNativeMethodPrefixSupported()) {
                inst.setNativeMethodPrefix(Transformer.getInstance().getWrapper(), AgentConstants.NATIVE_METHOD_PREFIX);
            }
            Transformer.getInstance().initLoadedClasses();

            String groupHierarchyPath = vmGroup;
            if (pool) {
                if (!groupHierarchyPath.isEmpty()) {
                    groupHierarchyPath += "/";
                }
                groupHierarchyPath += vmPool;
            }
            ConfigurationParameter.readAndApply(jvmguardUserDir, groupHierarchyPath, pool);

            if (!AgentProperties.getBoolean("disableOverdueChecker")) {
                OverdueChecker.getInstance().start();
            }

            int serverPort = JvmGuardCommunication.DEFAULT_PORT;
            String serverPortString = AgentProperties.getProperty(SERVER_PORT_PARAMETER);
            if (serverPortString != null) {
                try {
                    serverPort = Integer.parseInt(serverPortString);
                } catch (NumberFormatException e) {
                    println("ERROR: cannot parse port " + serverPortString);
                }
            }
            String keyStorePath = AgentProperties.getProperty(KEYSTORE_PARAMETER);
            if (keyStorePath == null) {
                keyStorePath = AgentProperties.getProperty(KEYSTORE_PARAMETER.toLowerCase());
            }
            if (keyStorePath == null || keyStorePath.isEmpty()) {
                File keyStoreFile = new File(bootstrapBaseDir, JvmGuardKeyManager.AGENT_STORE);
                if (!keyStoreFile.isFile()) {
                    keyStoreFile = new File(getJvmGuardUserDir(), JvmGuardKeyManager.AGENT_STORE);
                }
                keyStorePath = keyStoreFile.getAbsolutePath();
            }
            initBuildVersion(javaAgentJar);
            JvmGuardCommunication.init(AgentProperties.getProperty(SERVER_PARAMETER, "localhost"), serverPort, keyStorePath);
            if (useSpringWorkaround(arguments)) {
                println("Spring Loaded found. Deferring communication start");
            } else {
                JvmGuardCommunication.start();
            }
            println("Agent initialized (build version " + getBuildVersion() + ")");
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private static void initModuleHelperImplementation(Instrumentation inst) {
        if (Util.isJava9Plus()) {
            moduleHelperImplementation = new Java9ModuleHelperImplementation(inst, e -> Logger.log(Subsystem.INSTRUMENTATION, 0, false, e));
        } else {
            moduleHelperImplementation = new ModuleHelperImplementation() {
                @Override
                public void addOpens(Class clazz, String packageName) {
                }

                @Override
                public void addExports(Class clazz, String packageName) {
                }

            };
        }
        ModuleHelper.setImplementation(moduleHelperImplementation);
    }


    protected static void initLogging(String loggingName) {
        LoggingHandler.setName(loggingName.isEmpty() ? "jvm" : loggingName);
        MBeanManager.setLogAdapter(new LogAdapter() {
            @Override
            public void error(String message) {
                Logger.log(Subsystem.COMMUNICATION, 0, true, message);
            }

            @Override
            public void error(Throwable t) {
                Logger.log(Subsystem.COMMUNICATION, 0, true, t);
            }

            @Override
            public boolean isLogNotification() {
                return Logger.isEnabled(Subsystem.COMMUNICATION, 1);
            }
        });
    }

    private static void initVmIdentifier() {
        SecureRandom secureRandom = new SecureRandom();
        while (instanceId == 0) {
            instanceId = secureRandom.nextLong();
        }
    }

    private static void addRmiExports() {
        try {
            Class transportClass = Class.forName("sun.rmi.transport.Connection");
            ModuleHelper.addExports(transportClass, "sun.rmi.transport");
            ModuleHelper.addExports(transportClass, "sun.rmi.transport.tcp");
        } catch (Throwable t) {
            if (Boolean.getBoolean("jvmguard.debugModules")) {
                t.printStackTrace();
            }
            // rmi not included, continue
        }
    }

    private static void checkWindowsTimerWorkaround() {
        if (Util.isWindows()) {
            new Thread(AGENT_THREAD_GROUP, "_jvmguard_timer") {
                {
                    setDaemon(true);
                }

                @SuppressWarnings({"InfiniteLoopStatement", "BusyWait"})
                @Override
                public void run() {
                    while (true) {
                        try {
                            Thread.sleep(Long.MAX_VALUE);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }.start();
        }
    }

    // start comm thread deferred to work around a CME when the springloaded agent is placed after our agent
    private static boolean useSpringWorkaround(List<String> arguments) {
        boolean jvmguardFound = false;
        for (String argument : arguments) {
            if (argument.startsWith("-javaagent")) {
                if (argument.contains("jvmguard.jar")) {
                    jvmguardFound = true;
                } else if (argument.contains("springloaded") && jvmguardFound) {
                    return true;
                }
            }
        }
        return false;
    }

    private static List<String> getVmArguments() {
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        return runtimeMxBean.getInputArguments();
    }

    public static File getJvmGuardUserDir() {
        return jvmguardUserDir;
    }

    // do not alter, is called in generated code
    public static void log(Throwable t) {
        Logger.log(Subsystem.COMMON, 0, true, t);
        if (DEBUG) {
            t.printStackTrace();
        }
    }

    public static void log(String str) {
        Logger.log(Subsystem.COMMON, 0, true, str);
    }

    public static void println(Object obj) {
        if (!noStdout) {
            System.err.println(MESSAGE_PREFIX + obj);
        }
        Logger.log(Subsystem.COMMON, 1, false, obj);
    }

    public static void println() {
        if (!noStdout) {
            System.err.println();
        }
    }

    public static long getBuildVersion() {
        return buildVersion;
    }

    public static void initBuildVersion(File javaAgentJar) {
        try {
            Long override = Long.getLong("jvmguard.overrideBuildVersion");
            if (override != null) {
                buildVersion = override;
            } else {
                JarFile jarFile = new JarFile(javaAgentJar);
                buildVersion = Long.parseLong(jarFile.getManifest().getMainAttributes().getValue(AgentInit.BUILD_VERSION_ATTRIBUTE_NAME));
                jarFile.close();
            }
        } catch (Throwable e) {
            log(e);
        }
    }

    public static File getAgentUserDir() {
        return agentUserDir;
    }
}
