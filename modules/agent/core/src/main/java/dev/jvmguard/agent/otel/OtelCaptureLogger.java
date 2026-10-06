package dev.jvmguard.agent.otel;

import dev.jvmguard.agent.JvmGuardAgent;
import dev.jvmguard.agent.base.logging.Subsystem;
import dev.jvmguard.agent.comm.CommandType;
import dev.jvmguard.agent.config.transactions.PolicyEventType;
import dev.jvmguard.agent.parameter.BaseParameter;
import dev.jvmguard.agent.parameter.CaptureContext;
import dev.jvmguard.agent.parameter.CaptureContextProvider;
import dev.jvmguard.agent.util.Logger;

import java.lang.reflect.Method;

/**
 * Emits an OTel log record when a capture (heap dump, thread dump, JFR snapshot, JProfiler
 * recording) completes successfully.
 * <p>
 * The agent lives on the bootstrap class path and cannot see application classes by delegation,
 * so the OTel API is resolved against, in order: the class loader of an instrumented application
 * class (stashed via {@link #captureClassLoader(ClassLoader)}, covers an app-bundled SDK), the
 * bootstrap loader (covers the OTel javaagent, which injects the API there), the thread context
 * class loader and the system class loader.
 */
public class OtelCaptureLogger {

    private static final String GLOBAL_OTEL_CLASS_NAME = "io.opentelemetry.api.GlobalOpenTelemetry";
    private static final String OTEL_CLASS_NAME = "io.opentelemetry.api.OpenTelemetry";
    private static final String LOGGER_PROVIDER_CLASS_NAME = "io.opentelemetry.api.logs.LoggerProvider";
    private static final String LOGGER_BUILDER_CLASS_NAME = "io.opentelemetry.api.logs.LoggerBuilder";
    private static final String LOGGER_CLASS_NAME = "io.opentelemetry.api.logs.Logger";
    private static final String LOG_RECORD_BUILDER_CLASS_NAME = "io.opentelemetry.api.logs.LogRecordBuilder";
    private static final String SEVERITY_CLASS_NAME = "io.opentelemetry.api.logs.Severity";
    private static final String ATTRIBUTE_KEY_CLASS_NAME = "io.opentelemetry.api.common.AttributeKey";

    private static final String INSTRUMENTATION_SCOPE = "dev.jvmguard";

    private static volatile boolean disabled;
    private static volatile boolean initialized;
    private static volatile ClassLoader applicationClassLoader;

    // reflective handles into io.opentelemetry.api, all resolved against the same class loader.
    private static Object otelLogger; // io.opentelemetry.api.logs.Logger
    private static Method logRecordBuilderMethod;
    private static Method setBodyMethod;
    private static Method setSeverityMethod;
    private static Method setAttributeMethod;
    private static Method stringKeyMethod;
    private static Method emitMethod;
    private static Object severityInfo;
    private static Object severityWarn;
    private static Object severityError;

    /**
     * Called from the instrumentation path with the defining loader of instrumented application
     * classes. The first loader that can see the OTel API wins.
     */
    public static void captureClassLoader(ClassLoader classLoader) {
        if (classLoader != null && applicationClassLoader == null && !disabled) {
            try {
                Class.forName(GLOBAL_OTEL_CLASS_NAME, false, classLoader);
                applicationClassLoader = classLoader;
            } catch (Throwable ignored) {
            }
        }
    }

    public static void emit(CommandType commandType, BaseParameter parameter, boolean success) {
        if (!success || disabled || !(parameter instanceof CaptureContextProvider)) {
            return;
        }
        try {
            // the server leaves the capture context out when the emission is disabled in the
            // trigger settings of the group that fired the capture
            CaptureContext captureContext = ((CaptureContextProvider)parameter).getCaptureContext();
            if (captureContext == null) {
                return;
            }
            if (!initialized) {
                initialize();
            }
            if (disabled) {
                return;
            }
            PolicyEventType policyEventType = captureContext.getPolicyEventType();
            Object recordBuilder = logRecordBuilderMethod.invoke(otelLogger);
            setBodyMethod.invoke(recordBuilder, "jvmguard capture: " + getCaptureTypeName(commandType));
            setSeverityMethod.invoke(recordBuilder, getSeverity(policyEventType));
            setAttribute(recordBuilder, "jvmguard.capture.type", getCaptureTypeAttribute(commandType));
            setAttribute(recordBuilder, "jvmguard.capture.origin",
                captureContext.getOrigin() == CaptureContext.Origin.TRIGGER ? "trigger" : "manual");
            String vmName = JvmGuardAgent.getVmName();
            if (vmName != null && !vmName.isEmpty()) {
                setAttribute(recordBuilder, "jvmguard.vm.name", vmName);
            }
            if (!captureContext.getTransactionName().isEmpty()) {
                setAttribute(recordBuilder, "jvmguard.transaction.name", captureContext.getTransactionName());
            }
            if (policyEventType != null) {
                setAttribute(recordBuilder, "jvmguard.policy.event", policyEventType.name());
            }
            if (!captureContext.getTriggerDescription().isEmpty()) {
                setAttribute(recordBuilder, "jvmguard.trigger", captureContext.getTriggerDescription());
            }
            emitMethod.invoke(recordBuilder);
        } catch (Throwable t) {
            disable("error while emitting OTel log record: " + t);
        }
    }

    private static void setAttribute(Object recordBuilder, String key, String value) throws Exception {
        setAttributeMethod.invoke(recordBuilder, stringKeyMethod.invoke(null, key), value);
    }

    private static Object getSeverity(PolicyEventType policyEventType) {
        if (policyEventType == PolicyEventType.ERROR) {
            return severityError;
        }
        if (policyEventType == PolicyEventType.VERY_SLOW || policyEventType == PolicyEventType.OVERDUE) {
            return severityWarn;
        }
        return severityInfo;
    }

    static String getSeverityName(PolicyEventType policyEventType) {
        if (policyEventType == PolicyEventType.ERROR) {
            return "ERROR";
        }
        if (policyEventType == PolicyEventType.VERY_SLOW || policyEventType == PolicyEventType.OVERDUE) {
            return "WARN";
        }
        return "INFO";
    }

    private static String getCaptureTypeName(CommandType commandType) {
        switch (commandType) {
            case HEAP_DUMP:
                return "heap dump";
            case THREAD_DUMP:
                return "thread dump";
            case JFR_SNAPSHOT:
                return "JFR snapshot";
            case RECORD_JPROFILER:
                return "JProfiler recording";
            default:
                return commandType.name();
        }
    }

    private static String getCaptureTypeAttribute(CommandType commandType) {
        switch (commandType) {
            case HEAP_DUMP:
                return "heap_dump";
            case THREAD_DUMP:
                return "thread_dump";
            case JFR_SNAPSHOT:
                return "jfr_snapshot";
            case RECORD_JPROFILER:
                return "jprofiler_recording";
            default:
                return commandType.name().toLowerCase();
        }
    }

    private static synchronized void initialize() {
        if (initialized) {
            return;
        }
        // null is the bootstrap loader and a valid candidate: the OTel javaagent injects the API
        // there, and the agent itself is appended to the bootstrap class path
        ClassLoader[] candidates = {
            applicationClassLoader,
            null,
            Thread.currentThread().getContextClassLoader(),
            ClassLoader.getSystemClassLoader()
        };
        for (ClassLoader candidate : candidates) {
            if (tryInitialize(candidate)) {
                initialized = true;
                return;
            }
        }
        disable("OpenTelemetry API not found, capture events will not be emitted as OTel log records");
        initialized = true;
    }

    static synchronized boolean tryInitialize(ClassLoader classLoader) {
        if (disabled) {
            return false;
        }
        try {
            Class<?> globalClass = Class.forName(GLOBAL_OTEL_CLASS_NAME, true, classLoader);
            Object openTelemetry = globalClass.getMethod("get").invoke(null);
            Class<?> openTelemetryClass = Class.forName(OTEL_CLASS_NAME, false, classLoader);
            Object logsBridge = openTelemetryClass.getMethod("getLogsBridge").invoke(openTelemetry);
            Class<?> loggerProviderClass = Class.forName(LOGGER_PROVIDER_CLASS_NAME, false, classLoader);
            Object loggerBuilder = loggerProviderClass.getMethod("loggerBuilder", String.class).invoke(logsBridge, INSTRUMENTATION_SCOPE);
            Class<?> loggerBuilderClass = Class.forName(LOGGER_BUILDER_CLASS_NAME, false, classLoader);
            otelLogger = loggerBuilderClass.getMethod("build").invoke(loggerBuilder);

            Class<?> loggerClass = Class.forName(LOGGER_CLASS_NAME, false, classLoader);
            logRecordBuilderMethod = loggerClass.getMethod("logRecordBuilder");
            Class<?> recordBuilderClass = Class.forName(LOG_RECORD_BUILDER_CLASS_NAME, false, classLoader);
            setBodyMethod = recordBuilderClass.getMethod("setBody", String.class);
            Class<?> severityClass = Class.forName(SEVERITY_CLASS_NAME, false, classLoader);
            setSeverityMethod = recordBuilderClass.getMethod("setSeverity", severityClass);
            Class<?> attributeKeyClass = Class.forName(ATTRIBUTE_KEY_CLASS_NAME, false, classLoader);
            stringKeyMethod = attributeKeyClass.getMethod("stringKey", String.class);
            setAttributeMethod = recordBuilderClass.getMethod("setAttribute", attributeKeyClass, Object.class);
            emitMethod = recordBuilderClass.getMethod("emit");
            severityInfo = severityClass.getField("INFO").get(null);
            severityWarn = severityClass.getField("WARN").get(null);
            severityError = severityClass.getField("ERROR").get(null);
            Logger.log(Subsystem.COMMON, 1, true, "OTel capture logging initialized");
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void disable(String message) {
        disabled = true;
        Logger.log(Subsystem.COMMON, 1, true, message);
    }

    static synchronized void resetForTest() {
        disabled = false;
        initialized = false;
        applicationClassLoader = null;
        otelLogger = null;
    }

    static boolean isDisabled() {
        return disabled;
    }
}
