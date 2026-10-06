package dev.jvmguard.agent.otel;

import dev.jvmguard.agent.comm.CommandType;
import dev.jvmguard.agent.config.transactions.PolicyEventType;
import dev.jvmguard.agent.parameter.CaptureContext;
import dev.jvmguard.agent.parameter.HeapDumpParameters;
import dev.jvmguard.agent.parameter.ThreadDumpParameters;

import dev.jvmguard.agent.otel.FakeOtelLogs.EmittedLogRecord;
import io.opentelemetry.api.logs.Severity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLClassLoader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OtelCaptureLoggerTest {

    @BeforeAll
    static void register() {
        FakeOtelLogs.register();
    }

    @BeforeEach
    void reset() {
        OtelCaptureLogger.resetForTest();
        FakeOtelLogs.records.clear();
        FakeOtelLogs.failOnEmit = false;
        FakeOtelLogs.lastInstrumentationScope = null;
    }

    @Test
    void emitsTriggerCapture() {
        CaptureContext context = new CaptureContext(
            CaptureContext.Origin.TRIGGER, "slow checkout", "Checkout.pay", PolicyEventType.VERY_SLOW);
        OtelCaptureLogger.emit(CommandType.HEAP_DUMP, new HeapDumpParameters(context), true);

        assertEquals(1, FakeOtelLogs.records.size());
        EmittedLogRecord record = FakeOtelLogs.records.getFirst();
        assertEquals("jvmguard capture: heap dump", record.body);
        assertEquals(Severity.WARN, record.severity);
        assertEquals("heap_dump", record.attributes.get("jvmguard.capture.type"));
        assertEquals("trigger", record.attributes.get("jvmguard.capture.origin"));
        assertEquals("Checkout.pay", record.attributes.get("jvmguard.transaction.name"));
        assertEquals("VERY_SLOW", record.attributes.get("jvmguard.policy.event"));
        assertEquals("slow checkout", record.attributes.get("jvmguard.trigger"));
        assertEquals("dev.jvmguard", FakeOtelLogs.lastInstrumentationScope);
    }

    @Test
    void emitsManualCaptureWithInfoSeverity() {
        OtelCaptureLogger.emit(CommandType.THREAD_DUMP, new ThreadDumpParameters(CaptureContext.manual()), true);

        assertEquals(1, FakeOtelLogs.records.size());
        EmittedLogRecord record = FakeOtelLogs.records.getFirst();
        assertEquals("jvmguard capture: thread dump", record.body);
        assertEquals(Severity.INFO, record.severity);
        assertEquals("thread_dump", record.attributes.get("jvmguard.capture.type"));
        assertEquals("manual", record.attributes.get("jvmguard.capture.origin"));
        assertNull(record.attributes.get("jvmguard.transaction.name"));
        assertNull(record.attributes.get("jvmguard.policy.event"));
        assertNull(record.attributes.get("jvmguard.trigger"));
    }

    @Test
    void errorSeverityMapping() {
        CaptureContext context = new CaptureContext(
            CaptureContext.Origin.TRIGGER, "errors", "Tx", PolicyEventType.ERROR);
        OtelCaptureLogger.emit(CommandType.JFR_SNAPSHOT, new HeapDumpParameters(context), true);
        assertEquals(Severity.ERROR, FakeOtelLogs.records.getFirst().severity);
    }

    @Test
    void absentCaptureContextSuppressesEmission() {
        // the server leaves the context out when the emission is disabled in the trigger settings
        OtelCaptureLogger.emit(CommandType.HEAP_DUMP, new HeapDumpParameters(null), true);
        assertTrue(FakeOtelLogs.records.isEmpty());
    }

    @Test
    void failedCaptureEmitsNothing() {
        OtelCaptureLogger.emit(CommandType.HEAP_DUMP, new HeapDumpParameters(CaptureContext.manual()), false);
        assertTrue(FakeOtelLogs.records.isEmpty());
    }

    @Test
    void emitFailureDisablesPermanently() {
        FakeOtelLogs.failOnEmit = true;
        OtelCaptureLogger.emit(CommandType.HEAP_DUMP, new HeapDumpParameters(CaptureContext.manual()), true);
        assertTrue(OtelCaptureLogger.isDisabled());
        assertTrue(FakeOtelLogs.records.isEmpty());

        FakeOtelLogs.failOnEmit = false;
        OtelCaptureLogger.emit(CommandType.HEAP_DUMP, new HeapDumpParameters(CaptureContext.manual()), true);
        assertTrue(FakeOtelLogs.records.isEmpty());
    }

    @Test
    void nonCaptureCommandsAreIgnored() {
        // emit is called for every command; only capture parameters may produce a record
        OtelCaptureLogger.emit(CommandType.TELEMETRY, new dev.jvmguard.agent.parameter.BaseParameter(), true);
        assertTrue(FakeOtelLogs.records.isEmpty());
    }

    @Test
    void bootstrapOnlyLoaderCannotSeeOtel() {
        // a loader with the bootstrap loader as parent cannot see the test-classpath fake API
        assertFalse(OtelCaptureLogger.tryInitialize(new URLClassLoader(new URL[0], null)));
    }

    @Test
    void severityNames() {
        assertEquals("ERROR", OtelCaptureLogger.getSeverityName(PolicyEventType.ERROR));
        assertEquals("WARN", OtelCaptureLogger.getSeverityName(PolicyEventType.VERY_SLOW));
        assertEquals("WARN", OtelCaptureLogger.getSeverityName(PolicyEventType.OVERDUE));
        assertEquals("INFO", OtelCaptureLogger.getSeverityName(PolicyEventType.SLOW));
        assertEquals("INFO", OtelCaptureLogger.getSeverityName(PolicyEventType.NORMAL));
        assertEquals("INFO", OtelCaptureLogger.getSeverityName(null));
    }
}
