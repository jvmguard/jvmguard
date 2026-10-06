package dev.jvmguard.agent.otel;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.LoggerBuilder;
import io.opentelemetry.api.logs.LoggerProvider;
import io.opentelemetry.api.logs.LogRecordBuilder;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.context.Context;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Registers a recording logs bridge into GlobalOpenTelemetry so OtelCaptureLogger is tested
 * against the real OTel API
 */
public class FakeOtelLogs {

    public static final List<EmittedLogRecord> records = new ArrayList<>();
    public static String lastInstrumentationScope;
    public static boolean failOnEmit;

    private static boolean registered;

    public static synchronized void register() {
        if (!registered) {
            GlobalOpenTelemetry.set(new FakeOpenTelemetry());
            registered = true;
        }
    }

    private static class FakeOpenTelemetry implements OpenTelemetry {
        private final OpenTelemetry noop = OpenTelemetry.noop();
        private final LoggerProvider loggerProvider = new FakeLoggerProvider();

        @Override
        public io.opentelemetry.api.trace.TracerProvider getTracerProvider() {
            return noop.getTracerProvider();
        }

        @Override
        public io.opentelemetry.api.metrics.MeterProvider getMeterProvider() {
            return noop.getMeterProvider();
        }

        @Override
        public LoggerProvider getLogsBridge() {
            return loggerProvider;
        }

        @Override
        public io.opentelemetry.context.propagation.ContextPropagators getPropagators() {
            return noop.getPropagators();
        }
    }

    private static class FakeLoggerProvider implements LoggerProvider {
        @Override
        public LoggerBuilder loggerBuilder(@NotNull String instrumentationScopeName) {
            lastInstrumentationScope = instrumentationScopeName;
            return new FakeLoggerBuilder();
        }
    }

    private static class FakeLoggerBuilder implements LoggerBuilder {
        @Override
        public LoggerBuilder setInstrumentationVersion(@NotNull String instrumentationVersion) {
            return this;
        }

        @Override
        public LoggerBuilder setSchemaUrl(@NotNull String schemaUrl) {
            return this;
        }

        @Override
        public Logger build() {
            return new FakeLogger();
        }
    }

    private static class FakeLogger implements Logger {
        @Override
        public LogRecordBuilder logRecordBuilder() {
            return new FakeLogRecordBuilder();
        }
    }

    private static class FakeLogRecordBuilder implements LogRecordBuilder {
        private final EmittedLogRecord record = new EmittedLogRecord();

        @Override
        public LogRecordBuilder setTimestamp(long timestamp, @NotNull TimeUnit unit) {
            return this;
        }

        @Override
        public LogRecordBuilder setTimestamp(@NotNull Instant instant) {
            return this;
        }

        @Override
        public LogRecordBuilder setObservedTimestamp(long timestamp, @NotNull TimeUnit unit) {
            return this;
        }

        @Override
        public LogRecordBuilder setObservedTimestamp(@NotNull Instant instant) {
            return this;
        }

        @Override
        public LogRecordBuilder setContext(@NotNull Context context) {
            return this;
        }

        @Override
        public LogRecordBuilder setSeverity(@NotNull Severity severity) {
            record.severity = severity;
            return this;
        }

        @Override
        public LogRecordBuilder setSeverityText(@NotNull String severityText) {
            return this;
        }

        @Override
        public LogRecordBuilder setBody(@NotNull String body) {
            record.body = body;
            return this;
        }

        @Override
        public <T> LogRecordBuilder setAttribute(AttributeKey<T> key, T value) {
            record.attributes.put(key.getKey(), value);
            return this;
        }

        @Override
        public void emit() {
            if (failOnEmit) {
                throw new IllegalStateException("simulated emit failure");
            }
            records.add(record);
        }
    }

    public static class EmittedLogRecord {
        public String body;
        public Severity severity;
        public final Map<String, Object> attributes = new LinkedHashMap<>();
    }
}
