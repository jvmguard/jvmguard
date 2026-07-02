package com.jvmguard.agent.config.telemetry;

import com.jvmguard.annotation.Unit;

import java.util.HashMap;
import java.util.Map;

public enum TelemetryUnit {
    PLAIN("plain", Unit.PLAIN, 3, ""),
    PER_SECOND("per second", Unit.PER_SECOND, 4, "per second", "per minute", "per hour"),
    PERCENT("percent", Unit.PERCENT, 3, "%"),
    MILLISECONDS("milliseconds", Unit.MILLISECONDS, 4, "ms", "s"),
    @SuppressWarnings("UnnecessaryUnicodeEscape")
    MICROSECONDS("microseconds", Unit.MICROSECONDS, 4, "\u00b5s", "ms", "s"),
    @SuppressWarnings("UnnecessaryUnicodeEscape")
    NANOSECONDS("nanoseconds", Unit.NANOSECONDS, 4, "ns", "\u00b5s", "ms", "s"),
    BYTES("bytes", Unit.BYTES, 4, "b", "KB", "MB", "GB");

    private final int displayDigits;
    private final String[] labels;
    private final String verbose;
    private final Unit annotationUnit;

    private static final Map<Unit, TelemetryUnit> annotationUnitToTelemetryUnit = new HashMap<>();

    static {
        for (TelemetryUnit telemetryUnit : values()) {
            annotationUnitToTelemetryUnit.put(telemetryUnit.annotationUnit, telemetryUnit);
        }
    }

    TelemetryUnit(String verbose, Unit annotationUnit, int displayDigits, String... labels) {
        this.verbose = verbose;
        this.annotationUnit = annotationUnit;
        this.displayDigits = displayDigits;
        this.labels = labels;
    }

    public int getDisplayDigits() {
        return displayDigits;
    }

    public int getUnitLevels(double number) {
        for (int i = 0; i < labels.length; i++) {
            if (number < 10000) {
                return i;
            }
            number /= 1000;
        }
        return labels.length - 1;
    }

    public String getLabel(int magnitudes) {
        if (magnitudes >= 0 && magnitudes < labels.length) {
            return labels[magnitudes];
        } else {
            return "<unknown>";
        }
    }

    public String[] getLabels() {
        return labels;
    }

    public static TelemetryUnit fromAnnotationUnit(Unit unit) {
        TelemetryUnit telemetryUnit = annotationUnitToTelemetryUnit.get(unit);
        if (telemetryUnit == null) {
            throw new IllegalArgumentException("unknown annotation unit " + unit);
        }
        return telemetryUnit;
    }

    public Unit getAnnotationUnit() {
        return annotationUnit;
    }

    @Override
    public String toString() {
        return verbose;
    }

    public static boolean isExtentOfTime(TelemetryUnit telemetryUnit) {
        return telemetryUnit == MILLISECONDS || telemetryUnit == MICROSECONDS || telemetryUnit == NANOSECONDS;
    }
}
