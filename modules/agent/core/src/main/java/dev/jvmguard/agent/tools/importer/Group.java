package dev.jvmguard.agent.tools.importer;

import dev.jvmguard.agent.config.VmType;
import dev.jvmguard.agent.config.recording.RecordingOptions;
import dev.jvmguard.agent.config.telemetry.TelemetrySettings;
import dev.jvmguard.agent.config.transactions.TransactionSettings;
import dev.jvmguard.agent.parameter.ConfigurationParameter;
import dev.jvmguard.agent.util.JvmGuardUtil;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Group {
    private ConfigData config;

    private Group parent;

    public Group(Group parent) {
        this.parent = parent;
    }

    private Map<GroupIdentifier, Group> children = new HashMap<>();

    public void addConfig(ConfigData config, String[] hierarchyPath, int pathIndex) {
        if (pathIndex == hierarchyPath.length) {
            this.config = config;
        } else {
            boolean pool = pathIndex < hierarchyPath.length - 1 ? false : config.getVmType() == VmType.POOL;
            GroupIdentifier identifier = new GroupIdentifier(hierarchyPath[pathIndex], pool);
            Group child = children.get(identifier);
            if (child == null) {
                child = new Group(this);
                children.put(identifier, child);
            }
            child.addConfig(config, hierarchyPath, pathIndex + 1);
        }
    }

    public File getConfigFile(File baseDir) {
        return ConfigurationParameter.getGroupFile(baseDir, config.getHierarchyPath(), config.getVmType() == VmType.POOL);
    }

    public void deleteHierarchy(File baseDir) {
        File configDir = new File(baseDir, ConfigurationParameter.CONFIG_DIR_NAME);
        if (config.getHierarchyPath().isEmpty()) {
            JvmGuardUtil.emptyDirectory(configDir, Collections.emptySet());
        } else {
            getConfigFile(baseDir).delete();
            if (config.getVmType() == VmType.POOL) {
                deleteFiles(configDir, ConfigurationParameter.getGroupFileNameWithoutSuffix(config.getHierarchyPath(), true));
                deleteFiles(configDir, ConfigurationParameter.getGroupFileNameWithoutSuffix(config.getHierarchyPath(), false));
            }
        }
    }

    private void deleteFiles(File configDir, String prefix) {
        File[] files = configDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().startsWith(prefix) && !file.getName().equals(prefix + ConfigurationParameter.CONFIG_SUFFIX)) {
                    file.delete();
                }
            }
        }
    }

    public void visit(Visitor visitor) {
        if (config != null) {
            visitor.visit(this);
        }
        for (Group child : children.values()) {
            child.visit(visitor);
        }
    }

    @Override
    public String toString() {
        return "Group{" +
            "config=" + config +
            ",\nchildren=" + children +
            '}';
    }

    public RecordingOptions getRecordingOptions() {
        if (parent == null) {
            if (config != null && config.getRecordingOptions() != null) {
                return config.getRecordingOptions();
            } else {
                return new RecordingOptions();
            }
        } else if (config != null && config.getRecordingOptions() != null && config.getRecordingOptions().isUsed()) {
            return config.getRecordingOptions();
        } else {
            return parent.getRecordingOptions();
        }
    }

    public TransactionSettings getTransactionSettings() {
        if (parent == null) {
            if (config != null && config.getTransactionSettings() != null) {
                return config.getTransactionSettings();
            } else {
                return new TransactionSettings();
            }
        } else if (config != null && config.getTransactionSettings() != null && config.getTransactionSettings().isUsed()) {
            return config.getTransactionSettings();
        } else {
            return parent.getTransactionSettings();
        }
    }

    public TelemetrySettings getTelemetrySettings() {
        if (parent == null) {
            if (config != null && config.getTelemetrySettings() != null) {
                return config.getTelemetrySettings();
            } else {
                return new TelemetrySettings();
            }
        } else if (config != null && config.getTelemetrySettings() != null && config.getTelemetrySettings().isUsed()) {
            return config.getTelemetrySettings();
        } else {
            return parent.getTelemetrySettings();
        }
    }

    public interface Visitor {
        void visit(Group group);
    }
}
