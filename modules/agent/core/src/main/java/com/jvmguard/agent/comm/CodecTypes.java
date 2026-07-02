package com.jvmguard.agent.comm;

import com.jvmguard.agent.config.AgentGroupConfig;
import com.jvmguard.agent.config.recording.RecordingOptions;
import com.jvmguard.agent.config.telemetry.MBeanLineConfig;
import com.jvmguard.agent.config.telemetry.MBeanTelemetryConfig;
import com.jvmguard.agent.config.telemetry.TelemetrySettings;
import com.jvmguard.agent.config.transactions.*;
import com.jvmguard.agent.config.transactions.naming.*;

public final class CodecTypes {
    private CodecTypes() {
    }

    private static volatile boolean registered;

    public static void registerAll() {
        if (registered) {
            return;
        }
        synchronized (CodecTypes.class) {
            if (registered) {
                return;
            }
            CodecRegistry.register(
                AgentGroupConfig::new,
                RecordingOptions::new,
                TransactionSettings::new,
                TelemetrySettings::new,
                MBeanTelemetryConfig::new,
                MBeanLineConfig::new,
                Policy::new,
                PolicySubDef::new,
                TransactionNaming::new,
                PojoTransactionDef::new,
                CustomAnnotatedTransactionDef::new,
                DevOpsAnnotatedTransactionDef::new,
                ClassNameElement::new,
                InstanceClassNameElement::new,
                InstanceElement::new,
                MethodNameElement::new,
                MethodParameterElement::new,
                TextElement::new
            );
            registered = true;
        }
    }
}
