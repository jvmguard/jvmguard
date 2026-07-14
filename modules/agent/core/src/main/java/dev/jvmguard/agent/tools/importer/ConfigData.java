package dev.jvmguard.agent.tools.importer;

import com.grack.nanojson.JsonObject;
import dev.jvmguard.agent.comm.JsonAgentReader;
import dev.jvmguard.agent.config.AgentGroupConfig;
import dev.jvmguard.agent.config.VmType;
import dev.jvmguard.agent.config.recording.RecordingOptions;
import dev.jvmguard.agent.config.telemetry.TelemetrySettings;
import dev.jvmguard.agent.config.transactions.TransactionSettings;

public class ConfigData {
    private final String hierarchyPath;
    private final VmType vmType;
    private final AgentGroupConfig agentGroupConfig;

    public ConfigData(JsonObject groupJson) throws Exception {
        hierarchyPath = groupJson.getString(ConfigFileFormat.KEY_PATH, "");
        vmType = VmType.fromDatabaseId(groupJson.getInt(ConfigFileFormat.KEY_GROUP_TYPE, 0));
        JsonObject agentConfig = groupJson.getObject(ConfigFileFormat.KEY_AGENT_CONFIG);
        if (agentConfig != null) {
            agentGroupConfig = new AgentGroupConfig();
            agentGroupConfig.readState(new JsonAgentReader(agentConfig));
        } else {
            agentGroupConfig = null;
        }
    }

    public String getHierarchyPath() {
        return hierarchyPath;
    }

    public VmType getVmType() {
        return vmType;
    }

    public TransactionSettings getTransactionSettings() {
        return agentGroupConfig == null ? null : agentGroupConfig.getTransactionSettings();
    }

    public RecordingOptions getRecordingOptions() {
        return agentGroupConfig == null ? null : agentGroupConfig.getRecordingOptions();
    }

    public TelemetrySettings getTelemetrySettings() {
        return agentGroupConfig == null ? null : agentGroupConfig.getTelemetrySettings();
    }

    @Override
    public String toString() {
        return "ConfigData{" +
            "hierarchyPath='" + hierarchyPath + '\'' +
            ", vmType=" + vmType +
            ", agentGroupConfig=" + agentGroupConfig +
            '}';
    }
}
