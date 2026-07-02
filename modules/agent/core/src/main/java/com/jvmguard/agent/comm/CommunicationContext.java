package com.jvmguard.agent.comm;

import java.util.HashMap;
import java.util.Map;

public class CommunicationContext {
    public static final String PROPERTY_DEFERRED_DATA = "deferredData";
    public static final String PROPERTY_PARAMETER = "parameter";
    public static final String PROPERTY_LOOKUP_MAP = "lookupMap";
    public static final String PROPERTY_KEEP_LOOKUP_MAP = "keepLookupMap";
    public static final String PROPERTY_KEEP_SENT_ID_SET = "keepSentIdSet";
    public static final String PROPERTY_SENT_ID_SET = "sentIdSet";
    public static final String PROPERTY_STRING_LOOKUP_MAP = "payloadLookupMap";
    public static final String PROPERTY_REMOTE_HOST_NAME = "hostName";
    public static final String PROPERTY_REMOTE_PORT = "hostPort";

    private Type type = Type.PRIMARY;
    private int protocolVersion;
    private boolean terminate;

    private Map<String, Object> properties = new HashMap<>();

    public CommunicationContext(int protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    @SuppressWarnings("CopyConstructorMissesField")
    public CommunicationContext(CommunicationContext primaryContext) {
        type = Type.DEFERRED;
        protocolVersion = primaryContext.protocolVersion;
        properties.putAll(primaryContext.properties);
    }

    public boolean satisfies(ProtocolRequirement protocolRequirement) {
        return protocolRequirement.satisfies(protocolVersion);
    }

    public Type getType() {
        return type;
    }

    public CommunicationContext setType(Type type) {
        this.type = type;
        return this;
    }

    public boolean isTerminate() {
        return terminate;
    }

    public void setTerminate(boolean terminate) {
        this.terminate = terminate;
    }

    public Object getProperty(String name) {
        return properties.get(name);
    }

    public CommunicationContext setProperty(String name, Object data) {
        properties.put(name, data);
        return this;
    }

    public enum Type {
        PRIMARY,
        DEFERRED
    }
}
