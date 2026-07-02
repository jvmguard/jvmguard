import type {EChartsCoreOption} from 'echarts/core';

// The domain model pushed from the server (see TelemetryChartModel.kt). This module turns it into an
// Apache ECharts `option`. It holds no chart state and touches no element

export interface TelemetryPoint {
    t: number;
    v: number | null;
}

export interface TelemetrySeriesModel {
    name: string;
    colorKey?: string | null;
    points: TelemetryPoint[];
}

export interface TelemetryChartModel {
    series: TelemetrySeriesModel[];
    xMin: number;
    xMax: number;
    stacked: boolean;
    logarithmic: boolean;
    zeroBase: boolean;
    unitLabel: string;
    valueDecimals: number;
    bucketMillis?: number;
    yMin?: number | null;
    yMax?: number | null;
    canBackward?: boolean;
    canForward?: boolean;
    animate?: boolean;
    markTime?: number | null;
    markBandStart?: number | null;
    markBandEnd?: number | null;
}

const TRANSACTION_COLORS = {
    light: {normal: '#98df58', slow: '#f9dd51', verySlow: '#ffba42', error: '#ec6464'},
    dark: {normal: '#61ab21', slow: '#c5a507', verySlow: '#cc8100', error: '#b61616'},
} as const;

const CURRENT_MARKER_COLOR = {light: '#d32f2f', dark: '#ff6e6e'} as const;
const CURRENT_BAND_COLOR = {light: 'rgba(25,118,210,0.16)', dark: 'rgba(105,193,252,0.20)'} as const;

const PALETTE = {
    light: ['#1976d2', '#43a047', '#e8843c', '#00897b', '#8e44ad', '#c62828', '#c9a227', '#5d6d7e'],
    dark: ['#5aa9f0', '#7cc14e', '#f0a04b', '#4db6ac', '#b39ddb', '#ef5350', '#d4c24a', '#90a4ae'],
} as const;

const AXIS_COLORS = {
    light: {text: '#444', axis: '#999', split: 'rgba(0,0,0,0.08)'},
    dark: {text: '#c8c8c8', axis: '#888', split: 'rgba(255,255,255,0.12)'},
} as const;

// Marker sizes (largest first) and how much horizontal room (x diameter) a marker needs to be shown
// at that size. Below the smallest, markers are hidden.
const SYMBOL_SIZES = [9, 6, 3] as const;
const SYMBOL_SPACING_FACTOR = 2.5;
const Y_AXIS_GUTTER = 70;

const AREA_OPACITY = 0.65;
// How much the hovered "current point" grows relative to its resting size.
const HOVER_SCALE = 1.5;

/** The effective light/dark color scheme */
export function isDarkScheme(): boolean {
    const scheme = getComputedStyle(document.documentElement).colorScheme ?? '';
    if (scheme.includes('dark') && !scheme.includes('light')) {
        return true;
    }
    if (scheme.includes('light') && !scheme.includes('dark')) {
        return false;
    }
    return window.matchMedia('(prefers-color-scheme: dark)').matches;
}

/** The largest number of points across the model's series. */
export function maxPointCount(model: TelemetryChartModel | null): number {
    return model?.series.reduce((max, s) => Math.max(max, s.points.length), 0) ?? 0;
}

/**
 * Marker size in discrete steps chosen by point density: the largest size that
 * still leaves clear gaps between adjacent points.
 */
export function symbolSizeFor(pointCount: number, chartWidth: number): number {
    if (pointCount <= 1) {
        return 9;
    }
    const plotWidth = Math.max(50, chartWidth - Y_AXIS_GUTTER);
    const spacing = plotWidth / (pointCount - 1);
    for (const size of SYMBOL_SIZES) {
        if (spacing >= size * SYMBOL_SPACING_FACTOR) {
            return size;
        }
    }
    return 0;
}

function yAxisMin(model: TelemetryChartModel): number | undefined {
    if (model.yMin !== null && model.yMin !== undefined) {
        return model.yMin;
    }
    if (model.logarithmic) {
        return undefined;
    }
    return model.zeroBase ? 0 : undefined;
}

/** The resolved light/dark colors used throughout the option. */
interface SchemeColors {
    axisText: string;
    axisLine: string;
    splitLine: string;
    palette: readonly string[];
    transaction: Record<string, string>;
    border: string;
    marker: string;
    band: string;
}

function schemeColors(dark: boolean): SchemeColors {
    const axis = dark ? AXIS_COLORS.dark : AXIS_COLORS.light;
    return {
        axisText: axis.text,
        axisLine: axis.axis,
        splitLine: axis.split,
        palette: dark ? PALETTE.dark : PALETTE.light,
        transaction: dark ? TRANSACTION_COLORS.dark : TRANSACTION_COLORS.light,
        border: dark ? '#2b2b2b' : '#ffffff',
        marker: dark ? CURRENT_MARKER_COLOR.dark : CURRENT_MARKER_COLOR.light,
        band: dark ? CURRENT_BAND_COLOR.dark : CURRENT_BAND_COLOR.light,
    };
}

/** The axis-only option used when there is no data to plot. */
function emptyOption(colors: SchemeColors): EChartsCoreOption {
    return {
        xAxis: {type: 'time', axisLine: {lineStyle: {color: colors.axisLine}}},
        yAxis: {type: 'value', axisLine: {lineStyle: {color: colors.axisLine}}},
        series: [],
    };
}

/** Formats a value with the model's unit and decimals (used by the tooltip / y-axis labels). */
function valueFormatter(model: TelemetryChartModel): (value: number | null | undefined) => string {
    const unit = model.unitLabel ?? '';
    const decimals = model.valueDecimals ?? 0;
    return (value) => {
        if (value === null || value === undefined) {
            return '-';
        }
        const text = decimals > 0 ? value.toFixed(decimals) : `${value}`;
        return unit ? `${text} ${unit}` : text;
    };
}

function pad2(n: number): string {
    return n < 10 ? `0${n}` : `${n}`;
}

function escapeHtml(text: string): string {
    return text.replace(/[&<>]/g, (c) => (c === '&' ? '&amp;' : c === '<' ? '&lt;' : '&gt;'));
}

function bucketHeader(t: number, bucketMillis: number | undefined): string {
    const start = new Date(t);
    const date = (d: Date) => `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;
    if (!bucketMillis || bucketMillis <= 0) {
        return `${date(start)} ${pad2(start.getHours())}:${pad2(start.getMinutes())}:${pad2(start.getSeconds())}`;
    }
    const end = new Date(t + bucketMillis);
    const withSeconds = bucketMillis < 60_000;
    const clock = (d: Date) =>
        `${pad2(d.getHours())}:${pad2(d.getMinutes())}${withSeconds ? `:${pad2(d.getSeconds())}` : ''}`;
    // Show the end date too only when the bucket crosses midnight.
    const endLabel = start.toDateString() === end.toDateString() ? clock(end) : `${date(end)} ${clock(end)}`;
    return `${date(start)} ${clock(start)} – ${endLabel}`;
}

/** The axis tooltip: a bucket-range header followed by one row per series (marker · name · value). */
function tooltipFormatter(model: TelemetryChartModel): (params: unknown) => string {
    const format = valueFormatter(model);
    return (params) => {
        const points = (Array.isArray(params) ? params : [params]) as Array<Record<string, any>>;
        if (points.length === 0) {
            return '';
        }
        const value = points[0].value as [number, number | null] | undefined;
        const t = value ? value[0] : points[0].axisValue;
        const header = `<div style="margin-bottom:4px;font-weight:600">${bucketHeader(t, model.bucketMillis)}</div>`;
        const rows = points.map((p) => {
            const v = (p.value as [number, number | null] | undefined)?.[1];
            return `${p.marker ?? ''}${escapeHtml(String(p.seriesName ?? ''))}: <b>${format(v)}</b>`;
        });
        return header + rows.join('<br/>');
    };
}

/** One ECharts line series for a telemetry series, in the resolved [color]. */
function seriesItem(
    s: TelemetrySeriesModel, model: TelemetryChartModel, color: string,
    showSymbol: boolean, symbolSize: number, border: string,
): Record<string, unknown> {
    const areaStyle = model.stacked ? {opacity: AREA_OPACITY} : undefined;
    const lineWidth = model.stacked ? 1 : 2;
    return {
        name: s.name,
        type: 'line',
        stack: model.stacked ? 'total' : undefined,
        areaStyle,
        showSymbol,
        symbol: 'circle',
        symbolSize,
        connectNulls: false,
        lineStyle: {width: lineWidth, color},
        itemStyle: {color, borderColor: border, borderWidth: 1.5},
        // Hover enlarges only the "current point". Every color/width/opacity is pinned to its
        // resting value in both the emphasis and blur states, so ECharts never brightens the hovered
        // series or dims the others.
        emphasis: {
            focus: 'none',
            scale: HOVER_SCALE,
            itemStyle: {color, borderColor: border, borderWidth: 2},
            lineStyle: {width: lineWidth, color, opacity: 1},
            areaStyle: areaStyle && {color, opacity: AREA_OPACITY},
        },
        blur: {
            itemStyle: {color, borderColor: border, borderWidth: 1.5, opacity: 1},
            lineStyle: {width: lineWidth, color, opacity: 1},
            areaStyle: areaStyle && {color, opacity: AREA_OPACITY},
        },
        data: s.points.map((p) => [p.t, p.v]),
    };
}

/** All line series; non-transaction series cycle through the palette in order. */
function buildSeries(
    model: TelemetryChartModel, colors: SchemeColors, density: number,
): Record<string, unknown>[] {
    const showSymbol = density > 0;
    // Keep a usable base size even when resting markers are hidden, so the hovered "current point"
    // (which appears on hover regardless of showSymbol) still grows to a clearly visible size.
    const symbolSize = Math.max(density, 5);
    let paletteIndex = 0;
    return model.series.map((s) => {
        const color = s.colorKey
            ? colors.transaction[s.colorKey]
            : colors.palette[paletteIndex++ % colors.palette.length];
        return seriesItem(s, model, color, showSymbol, symbolSize, colors.border);
    });
}

/** markArea data for the current-interval band, only when actual data falls inside it. */
function bandMarkData(model: TelemetryChartModel): unknown[] {
    const bandStart = model.markBandStart;
    const bandEnd = model.markBandEnd;
    if (bandStart == null || bandEnd == null) {
        return [];
    }
    const hasData = model.series.some(
        (s) => s.points.some((p) => p.v != null && p.t >= bandStart && p.t <= bandEnd));
    return hasData ? [[{xAxis: bandStart}, {xAxis: bandEnd}]] : [];
}

/** markPoint data for the single current-interval point, snapped to the nearest data bucket. */
function pointMarkData(model: TelemetryChartModel): unknown[] {
    if (model.markTime == null) {
        return [];
    }
    const reference = model.series[0].points;
    let markX: number | null = null;
    let best = Infinity;
    for (const p of reference) {
        const distance = Math.abs(p.t - model.markTime);
        if (distance < best) {
            best = distance;
            markX = p.t;
        }
    }
    const spacing = reference.length >= 2 ? Math.abs(reference[1].t - reference[0].t) : 0;
    // Only mark when the current interval actually lands on a data point (within one bucket).
    if (markX === null || spacing <= 0 || best > spacing) {
        return [];
    }
    let markY: number | null = null;
    for (const s of model.series) {
        const v = s.points.find((p) => p.t === markX)?.v ?? null;
        if (v !== null) {
            markY = model.stacked ? (markY ?? 0) + v : Math.max(markY ?? v, v);
        }
    }
    return markY === null ? [] : [{coord: [markX, markY]}];
}

/**
 * Pins the current-interval highlight onto the first series: either a translucent band spanning the
 * tree's "Show" interval or a single point marker. Both markArea and markPoint are always assigned,
 * with empty data when not applicable, so a no-data/cleared state actually removes the old
 * mark on merge. The band takes precedence, the point is only used when no band is requested.
 */
function applyCurrentIntervalMarks(
    target: Record<string, unknown>, model: TelemetryChartModel, colors: SchemeColors, symbolSize: number,
): void {
    const bandRequested = model.markBandStart != null && model.markBandEnd != null;
    target.markArea = {
        silent: true,
        itemStyle: {color: colors.band},
        data: bandRequested ? bandMarkData(model) : [],
    };
    target.markPoint = {
        silent: true,
        symbol: 'circle',
        symbolSize: Math.max(symbolSize * 1.6, 10),
        itemStyle: {color: colors.marker, borderColor: colors.border, borderWidth: 2},
        label: {show: false},
        emphasis: {disabled: true},
        data: bandRequested ? [] : pointMarkData(model),
    };
}

function buildXAxis(model: TelemetryChartModel, colors: SchemeColors): Record<string, unknown> {
    return {
        type: 'time',
        min: model.xMin > 0 ? model.xMin : undefined,
        max: model.xMax > 0 ? model.xMax : undefined,
        axisLine: {lineStyle: {color: colors.axisLine}},
        axisLabel: {color: colors.axisText, hideOverlap: true},
        splitLine: {show: false},
    };
}

function buildYAxis(model: TelemetryChartModel, colors: SchemeColors): Record<string, unknown> {
    const unit = model.unitLabel ?? '';
    return {
        type: model.logarithmic ? 'log' : 'value',
        min: yAxisMin(model),
        max: model.yMax ?? undefined,
        scale: !model.zeroBase,
        axisLine: {lineStyle: {color: colors.axisLine}},
        axisLabel: {
            color: colors.axisText,
            formatter: unit ? (value: number) => `${value} ${unit}` : undefined,
        },
        splitLine: {lineStyle: {color: colors.splitLine}},
        // On a log axis, minor split lines at 2..9×10ⁿ give the uneven logarithmic spacing.
        minorTick: {show: model.logarithmic},
        minorSplitLine: {show: model.logarithmic, lineStyle: {color: colors.splitLine, type: 'dashed'}},
    };
}

/**
 * Builds the ECharts option for [model]. [density] is the resting marker size (from [symbolSizeFor]);
 * the caller computes it so it can also track changes across resizes.
 */
export function buildTelemetryOption(
    model: TelemetryChartModel | null, dark: boolean, density: number,
): EChartsCoreOption {
    const colors = schemeColors(dark);
    if (!model || model.series.length === 0) {
        return emptyOption(colors);
    }

    const series = buildSeries(model, colors, density);
    if (series.length > 0) {
        applyCurrentIntervalMarks(series[0], model, colors, Math.max(density, 5));
    }

    return {
        // Series don't morph. The y-axis rescale is animated by the element (animateRescale) so the
        // data appears instantly at the new values while the scale glides to the new target.
        animation: false,
        color: [...colors.palette],
        grid: {left: 8, right: 16, top: 48, bottom: 8, containLabel: true},
        legend: {
            type: 'scroll',
            top: 14,
            textStyle: {color: colors.axisText},
            inactiveColor: colors.axisLine,
        },
        tooltip: {
            trigger: 'axis',
            axisPointer: {type: 'none'},
            formatter: tooltipFormatter(model),
        },
        xAxis: buildXAxis(model, colors),
        yAxis: buildYAxis(model, colors),
        series,
    };
}
