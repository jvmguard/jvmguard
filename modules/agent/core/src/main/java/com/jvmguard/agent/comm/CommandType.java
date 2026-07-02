package com.jvmguard.agent.comm;

import com.jvmguard.agent.data.*;
import com.jvmguard.agent.mbean.*;
import com.jvmguard.agent.parameter.*;
import com.jvmguard.agent.telemetry.TelemetryResult;

import static com.jvmguard.agent.comm.ProtocolRequirement.*;

public enum CommandType {
    CONNECTION_INFO(null, ConnectionInfo.class, V1),
    TELEMETRY(null, TelemetryResult.class, V1),
    PACKAGE_STATS(null, PackageStats.class, V1),
    CLASSES_INFO(ClassesInfoParameter.class, ClassesInfo.class, V1),
    KILL(null, KillResult.class, V1),
    SET_CONFIGURATION(ConfigurationParameter.class, null, V1),
    JVMTI_RECORD(JvmtiRecordParameters.class, JpsResult.class, V1, true),
    RUN_GC(RunGcParameter.class, null, V1),
    FETCH_DEFERRED_DATA(null, DeferredDataResult.class, V1),
    HEAP_DUMP(null, HeapDumpResult.class, V1, true),
    THREAD_DUMP(null, ThreadDumpResult.class, V1),
    DATA_SET(DataSetParameter.class, DataSetResult.class, V1, true),
    RESET(ResetParameter.class, null, V1),
    METHOD_INFO(MethodInfoParameter.class, MethodInfoResult.class, V1),
    CHECK_AVAILABLE_AGENT(AvailableAgentParameter.class, AvailableAgentResult.class, V1),
    UPDATE_AGENT(UpdateAgentParameter.class, UpdateAgentResult.class, V1),
    ROUND_TRIP_PING(null, null, V1),
    REJECT(RejectParameter.class, RejectResult.class, V1),
    MBEAN_LIST(MBeanListParameter.class, MBeanListResult.class, V2),
    MBEAN_DATA(MBeanDataParameter.class, MBeanDataResult.class, V2),
    MBEAN_OPERATION(MBeanOperationParameter.class, MBeanOperationResult.class, V2, true),
    MBEAN_SET_ATTRIBUTE(MBeanSetAttributeParameter.class, MBeanSetAttributeResult.class, V2, true),
    JFR_SNAPSHOT(JfrRecordParameters.class, JfrSnapshotResult.class, V7, true);

    private final Class<? extends BaseParameter> parameterClass;
    private final Class<? extends BaseResult> resultClass;
    private final ProtocolRequirement protocolRequirement;
    private final boolean deferred;

    CommandType(Class<? extends BaseParameter> parameterClass, Class<? extends BaseResult> resultClass, ProtocolRequirement protocolRequirement) {
        this(parameterClass, resultClass, protocolRequirement, false);
    }

    CommandType(Class<? extends BaseParameter> parameterClass, Class<? extends BaseResult> resultClass, ProtocolRequirement protocolRequirement, boolean deferred) {
        this.parameterClass = parameterClass;
        this.resultClass = resultClass;
        this.protocolRequirement = protocolRequirement;
        this.deferred = deferred;
    }

    public ProtocolRequirement getProtocolRequirement() {
        return protocolRequirement;
    }

    public BaseParameter createParameter() {
        try {
            return parameterClass == null ? new BaseParameter() : parameterClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public BaseResult createResult() {
        try {
            return resultClass == null ? new BaseResult() : resultClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isDeferred() {
        return deferred;
    }
}
