package dev.jvmguard.integration.tests.jvmguard.tracing.classes;

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
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

public class FakeOtelLogs {

    public static void register() {
        GlobalOpenTelemetry.set(new FakeOpenTelemetry());
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
        private String body;
        private Severity severity;
        private final Map<String, Object> attributes = new TreeMap<>();

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
            this.severity = severity;
            return this;
        }

        @Override
        public LogRecordBuilder setSeverityText(@NotNull String severityText) {
            return this;
        }

        @Override
        public LogRecordBuilder setBody(@NotNull String body) {
            this.body = body;
            return this;
        }

        @Override
        public <T> LogRecordBuilder setAttribute(AttributeKey<T> key, T value) {
            attributes.put(key.getKey(), value);
            return this;
        }

        @Override
        public void emit() {
            StringBuilder sb = new StringBuilder("OTEL-CAPTURE-RECORD body=").append(body).append("; severity=").append(severity);
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                sb.append("; ").append(entry.getKey()).append('=').append(entry.getValue());
            }
            System.out.println(sb);
        }
    }
}
