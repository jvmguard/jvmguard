package dev.jvmguard.agent.comm;

import dev.jvmguard.agent.config.AgentGroupConfig;
import dev.jvmguard.agent.config.recording.RecordingOptions;
import dev.jvmguard.agent.config.telemetry.MBeanLineConfig;
import dev.jvmguard.agent.config.telemetry.MBeanTelemetryConfig;
import dev.jvmguard.agent.config.telemetry.TelemetrySettings;
import dev.jvmguard.agent.config.transactions.*;
import dev.jvmguard.agent.config.transactions.naming.*;

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
                MatchedTransactionDef::new,
                MappedTransactionDef::new,
                DeclaredTransactionDef::new,
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
