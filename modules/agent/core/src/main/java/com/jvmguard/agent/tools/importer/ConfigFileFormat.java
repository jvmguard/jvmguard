package com.jvmguard.agent.tools.importer;

public final class ConfigFileFormat {
    public static final int FILE_VERSION = 1;
    public static final String TYPE_SERVER_INIT = "serverInit";
    public static final String TYPE_RECORDING_CONFIG = "recordingConfig";
    public static final String KEY_VERSION = "version";
    public static final String KEY_TYPE = "type";
    public static final String KEY_GROUPS = "groups";
    public static final String KEY_PATH = "path";
    public static final String KEY_GROUP_TYPE = "groupType";
    public static final String KEY_ID = "id";
    public static final String KEY_AGENT_CONFIG = "agentConfig";
    public static final String KEY_SERVER_CONFIG = "serverConfig";

    private ConfigFileFormat() {
    }
}
