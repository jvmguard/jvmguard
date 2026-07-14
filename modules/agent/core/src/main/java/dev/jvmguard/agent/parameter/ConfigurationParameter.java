package dev.jvmguard.agent.parameter;

import dev.jvmguard.agent.JvmGuardAgent;
import dev.jvmguard.agent.base.logging.Subsystem;
import dev.jvmguard.agent.comm.CommunicationContext;
import dev.jvmguard.agent.comm.JvmGuardCommunication;
import dev.jvmguard.agent.config.AgentGroupConfig;
import dev.jvmguard.agent.config.base.DefaultConstructor;
import dev.jvmguard.agent.config.recording.RecordingOptions;
import dev.jvmguard.agent.config.telemetry.TelemetrySettings;
import dev.jvmguard.agent.config.transactions.*;
import dev.jvmguard.agent.instrument.Transformer;
import dev.jvmguard.agent.instrument.transaction.annotation.AnnotationDefinition;
import dev.jvmguard.agent.instrument.transaction.annotation.AnnotationTransactionDefList;
import dev.jvmguard.agent.instrument.transaction.matched.MatchedDefinition;
import dev.jvmguard.agent.instrument.transaction.matched.MatchedTransactionDefList;
import dev.jvmguard.agent.policy.PolicyHandler;
import dev.jvmguard.agent.telemetry.TelemetryCollector;
import dev.jvmguard.agent.util.FileNameEncoding;
import dev.jvmguard.agent.util.Logger;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.*;
import java.util.concurrent.Callable;

public class ConfigurationParameter extends BaseParameter {
    public static final String CONFIG_DIR_NAME = "config";
    public static final String CONFIG_SUFFIX = ".cfg";

    private static File groupFile;

    private static final String PROPERTY_DONT_STORE = "ConfigurationParameter.dontStore";

    public static volatile RecordingOptionsListener recordingOptionsListener;
    private static final boolean WAIT_FOR_LISTENER = Boolean.getBoolean("jvmguard.waitForListener");

    private AgentGroupConfig agentGroupConfig = new AgentGroupConfig();

    public ConfigurationParameter(RecordingOptions recordingOptions, TransactionSettings transactionSettings, TelemetrySettings telemetrySettings) {
        agentGroupConfig.setRecordingOptions(recordingOptions);
        agentGroupConfig.setTransactionSettings(transactionSettings);
        agentGroupConfig.setTelemetrySettings(telemetrySettings);
    }

    @DefaultConstructor
    public ConfigurationParameter() {
    }

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws Exception {
        agentGroupConfig.read(context, in);
        applyOptions(this, false);
        store(this);
    }

    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws Exception {
        agentGroupConfig.write(context, out);
    }

    public static synchronized void applyOptions(ConfigurationParameter parameters, boolean initial) {
        if (!initial) {
            checkListener();
            if (recordingOptionsListener != null) {
                recordingOptionsListener.preSet(parameters);
            }
        }
        try {
            TelemetryCollector.getInstance().setConfig(parameters.getTelemetrySettings());

            Map<MatchedDefinition, MatchedTransactionDefList> pojoDefinitionToTransactionDefs = new HashMap<>();
            Map<AnnotationDefinition, AnnotationTransactionDefList> annotationDefinitionToTransactionDefs = new HashMap<>();

            PolicyOptions policyOptions = new PolicyOptions();
            Set<AnnotationDefinition> declaredGroupDefinitions = new HashSet<>();

            for (TransactionDef transactionDef : parameters.getTransactionSettings().getTransactionDefs()) {
                transactionDef.getNaming().getGroup().setUsedValue(transactionDef.getNaming().getGroup().getUsedValue().intern());
                initPolicyHandler(transactionDef, policyOptions);
                initSubPolicyHandler(transactionDef, policyOptions);
                transactionDef.prepareForUsage();

                {
                    if (transactionDef instanceof MatchedTransactionDef) {
                        MatchedTransactionDef pojoTransactionDef = (MatchedTransactionDef)transactionDef;
                        MatchedDefinition pojoDefinition = pojoTransactionDef.createMatchedDefinition();

                        MatchedTransactionDefList transactionDefList = pojoDefinitionToTransactionDefs.get(pojoDefinition);
                        if (transactionDefList == null) {
                            transactionDefList = new MatchedTransactionDefList(pojoDefinition);
                            pojoDefinitionToTransactionDefs.put(pojoDefinition, transactionDefList);
                        }
                        transactionDefList.addTransactionDef(pojoTransactionDef);
                    } else if (transactionDef instanceof DeclaredTransactionDef) {
                        DeclaredTransactionDef declaredTransactionDev = (DeclaredTransactionDef)transactionDef;

                        for (AnnotationDefinition annotationDefinition : declaredTransactionDev.getAnnotationDefinitions()) {
                            AnnotationTransactionDefList transactionDefList = getAnnotationTransactionDefList(annotationDefinitionToTransactionDefs, annotationDefinition);
                            if (declaredTransactionDev.getGroup().getUsedValue().isEmpty()) {
                                transactionDefList.addTransactionDef(declaredTransactionDev);
                            } else {
                                declaredGroupDefinitions.add(annotationDefinition);
                            }
                        }
                    } else if (transactionDef instanceof AnnotatedTransactionDef) {
                        AnnotatedTransactionDef annotatedTransactionDef = (AnnotatedTransactionDef)transactionDef;

                        for (AnnotationDefinition annotationDefinition : annotatedTransactionDef.getAnnotationDefinitions()) {
                            AnnotationTransactionDefList transactionDefList = getAnnotationTransactionDefList(annotationDefinitionToTransactionDefs, annotationDefinition);
                            transactionDefList.addTransactionDef(annotatedTransactionDef);
                        }
                    }
                }
            }
            Logger.log(Subsystem.INSTRUMENTATION, 5, true, "used declared group definitions %s\n", declaredGroupDefinitions);
            if (!declaredGroupDefinitions.isEmpty()) {
                // for every group defined, a list must be constructed containing the entries from this group and the entries for all groups in the correct order
                for (TransactionDef transactionDef : parameters.getTransactionSettings().getTransactionDefs()) {
                    if (transactionDef instanceof DeclaredTransactionDef) {
                        DeclaredTransactionDef declaredTransactionDev = (DeclaredTransactionDef)transactionDef;

                        if (declaredTransactionDev.getGroup().getUsedValue().isEmpty()) {
                            for (AnnotationDefinition annotationDefinition : declaredGroupDefinitions) {
                                AnnotationTransactionDefList transactionDefList = getAnnotationTransactionDefList(annotationDefinitionToTransactionDefs, annotationDefinition);
                                transactionDefList.addTransactionDef(declaredTransactionDev);
                            }
                        } else {
                            for (AnnotationDefinition annotationDefinition : declaredTransactionDev.getAnnotationDefinitions()) {
                                AnnotationTransactionDefList transactionDefList = getAnnotationTransactionDefList(annotationDefinitionToTransactionDefs, annotationDefinition);
                                transactionDefList.addTransactionDef(declaredTransactionDev);
                            }
                        }
                    }
                }
            }

            Transformer.getInstance().setTransactionDefs(pojoDefinitionToTransactionDefs, annotationDefinitionToTransactionDefs, initial, parameters.getTransactionSettings().getRetransformationType());
        } catch (Throwable e) {
            Logger.log(Subsystem.COMMUNICATION, 0, true, "while updating configuration: " + e);
            Logger.log(Subsystem.COMMUNICATION, 0, true, e);
        } finally {
            if (!initial) {
                if (recordingOptionsListener != null) {
                    recordingOptionsListener.postSet(parameters);
                }
            }
        }
    }

    public static synchronized <T> T callWithoutConfigChange(Callable<T> callable) throws Exception {
        return callable.call();
    }

    private static void checkListener() {
        while (WAIT_FOR_LISTENER && recordingOptionsListener == null) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static AnnotationTransactionDefList getAnnotationTransactionDefList(Map<AnnotationDefinition, AnnotationTransactionDefList> annotationDefinitionToTransactionDefs, AnnotationDefinition annotationDefinition) {
        AnnotationTransactionDefList transactionDefList = annotationDefinitionToTransactionDefs.get(annotationDefinition);
        if (transactionDefList == null) {
            transactionDefList = AnnotationTransactionDefList.create(annotationDefinition);
            annotationDefinitionToTransactionDefs.put(annotationDefinition, transactionDefList);
        }
        return transactionDefList;
    }

    private static void initSubPolicyHandler(TransactionDef transactionDef, PolicyOptions policyOptions) {
        for (PolicySubDef policySubDef : transactionDef.getPolicySubDefs()) {
            initPolicyHandler(policySubDef, policyOptions);
            policySubDef.prepareForUsage();
        }
    }

    private static void initPolicyHandler(PolicyDef policyDef, PolicyOptions policyOptions) {
        PolicyHandler policyHandler = policyDef.initPolicyHandler();
        if (policyHandler != null) {
            Policy policy = policyHandler.getPolicy();
            if (policy.isLoggedWarningAsError() || policy.isLoggedErrorAsError()) {
                policyOptions.loggingInterception = true;
            }
        }
    }

    private static class PolicyOptions {
        boolean loggingInterception = false;
    }

    public TransactionSettings getTransactionSettings() {
        return agentGroupConfig.getTransactionSettings();
    }

    public void setTransactionSettings(TransactionSettings transactionSettings) {
        agentGroupConfig.setTransactionSettings(transactionSettings);
    }

    public void setRecordingOptions(RecordingOptions recordingOptions) {
        agentGroupConfig.setRecordingOptions(recordingOptions);
    }

    public TelemetrySettings getTelemetrySettings() {
        return agentGroupConfig.getTelemetrySettings();
    }

    public void setTelemetrySettings(TelemetrySettings telemetrySettings) {
        agentGroupConfig.setTelemetrySettings(telemetrySettings);
    }

    public static void store(ConfigurationParameter configurationParameter) {
        store(configurationParameter, groupFile, new LogHandler() {
            @Override
            public void println(String s) {
                JvmGuardAgent.println(s);
            }

            @Override
            public void log(Throwable t) {
                JvmGuardAgent.log(t);
            }
        });
    }

    public static synchronized void store(ConfigurationParameter configurationParameter, File groupFile, LogHandler logHandler) {
        if (groupFile != null) {
            try {
                logHandler.println("Storing configuration in " + groupFile.getCanonicalPath());
            } catch (IOException e) {
                e.printStackTrace();
            }

            RandomAccessFile raFile = null;
            FileOutputStream fileOutputStream = null;
            try {
                groupFile.getParentFile().mkdirs();

                raFile = new RandomAccessFile(groupFile, "rw");
                if (tryLock(raFile.getChannel(), false, 40)) {
                    raFile.setLength(0);
                    fileOutputStream = new FileOutputStream(raFile.getFD());
                    DataOutputStream out = new DataOutputStream(new BufferedOutputStream(fileOutputStream));
                    out.writeInt(JvmGuardCommunication.MAGIC_NUMBER);
                    out.writeInt(JvmGuardCommunication.PROTOCOL_VERSION);
                    CommunicationContext communicationContext = new CommunicationContext(JvmGuardCommunication.PROTOCOL_VERSION);
                    configurationParameter.write(communicationContext, out);
                    out.flush();
                }
            } catch (Throwable e) {
                logHandler.println("Could not store configuration in " + groupFile);
                logHandler.log(e);
            } finally {
                try {
                    if (raFile != null) {
                        raFile.close();
                    }
                    if (fileOutputStream != null) {
                        fileOutputStream.close();
                    }
                } catch (IOException e) {
                    logHandler.log(e);
                }

            }
        }
    }

    public static synchronized void readAndApply(File baseDir, String groupName, boolean pool) {
        DataInputStream din = null;
        File currentFile = null;
        try {
            groupFile = getGroupFile(baseDir, groupName, pool);
            if (!Boolean.getBoolean("jvmguard.ignoreConfig")) {
                currentFile = getBestGroupFile(groupFile);
                if (currentFile != null) {
                    JvmGuardAgent.println("Loading configuration from " + currentFile.getCanonicalPath());
                    FileInputStream in = new FileInputStream(currentFile);
                    if (tryLock(in.getChannel(), true, 40)) {
                        din = new DataInputStream(new BufferedInputStream(in));
                        int magicNumber = din.readInt();
                        if (magicNumber == JvmGuardCommunication.MAGIC_NUMBER) {
                            int protocolVersion = din.readInt();
                            if (protocolVersion >= JvmGuardCommunication.MINIMUM_PROTOCOL_VERSION && protocolVersion <= JvmGuardCommunication.PROTOCOL_VERSION) {
                                CommunicationContext communicationContext = new CommunicationContext(protocolVersion);
                                communicationContext.setProperty(ConfigurationParameter.PROPERTY_DONT_STORE, true);
                                ConfigurationParameter configurationParameter = new ConfigurationParameter();
                                configurationParameter.agentGroupConfig.read(communicationContext, din);
                                applyOptions(configurationParameter, true);
                            }
                        }
                    } else {
                        JvmGuardAgent.println("Could not lock configuration from " + currentFile);
                    }
                    in.close();
                } else {
                    ConfigurationParameter configurationParameter = new ConfigurationParameter();

                    List<TransactionDef> transactionDefs = new ArrayList<>();
                    transactionDefs.add(new DeclaredTransactionDef());
                    configurationParameter.getTransactionSettings().setTransactionDefs(transactionDefs);

                    applyOptions(configurationParameter, true);
                }
            }
        } catch (Throwable e) {
            JvmGuardAgent.println("Could not read configuration from " + currentFile);
            JvmGuardAgent.log(e);
        } finally {
            if (din != null) {
                try {
                    din.close();
                } catch (Throwable e) {
                    JvmGuardAgent.log(e);
                }
            }
        }
    }

    private static File getBestGroupFile(File myGroupFile) {
        File currentFile = myGroupFile;
        while (currentFile != null && !currentFile.isFile()) {
            String fileName = currentFile.getName();
            int lastEqual = fileName.lastIndexOf('=');
            if (lastEqual == -1) {
                currentFile = null;
            } else {
                currentFile = new File(currentFile.getParentFile(), "g" + fileName.substring(1, lastEqual) + CONFIG_SUFFIX);
            }
        }
        return currentFile;
    }

    public static String getGroupFileNameWithoutSuffix(String groupName, boolean pool) {
        String usedName = "g";
        if (groupName != null && !groupName.trim().isEmpty()) {
            usedName = (pool ? "p/" : "g/") + groupName;
        }
        usedName = FileNameEncoding.encode(usedName);
        return usedName;
    }

    public static File getGroupFile(File baseDir, String groupName, boolean pool) {
        return new File(baseDir, CONFIG_DIR_NAME + File.separatorChar + getGroupFileNameWithoutSuffix(groupName, pool) + CONFIG_SUFFIX);
    }

    private static boolean tryLock(FileChannel channel, boolean shared, int tryCount) throws IOException {
        for (int i = 0; i < tryCount; i++) {
            FileLock readLock = channel.tryLock(0, Long.MAX_VALUE, shared);
            if (readLock != null) {
                return true;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public interface RecordingOptionsListener {
        void preSet(ConfigurationParameter parameters);
        void postSet(ConfigurationParameter parameters);
    }

    public interface LogHandler {
        void println(String s);
        void log(Throwable t);
    }
}
