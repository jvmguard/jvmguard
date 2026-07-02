import {css, html, LitElement, nothing, type PropertyValues, type TemplateResult} from 'lit';
import {customElement, property, query} from 'lit/decorators.js';
import * as echarts from 'echarts/core';
import {LineChart} from 'echarts/charts';
import {
    DataZoomComponent,
    GridComponent,
    LegendComponent,
    MarkAreaComponent,
    MarkLineComponent,
    MarkPointComponent,
    TitleComponent,
    TooltipComponent,
} from 'echarts/components';
import {CanvasRenderer} from 'echarts/renderers';
import {
    buildTelemetryOption,
    isDarkScheme,
    maxPointCount,
    symbolSizeFor,
    type TelemetryChartModel,
} from './echart-telemetry-option';

echarts.use([
    LineChart,
    TitleComponent,
    TooltipComponent,
    GridComponent,
    LegendComponent,
    DataZoomComponent,
    MarkLineComponent,
    MarkAreaComponent,
    MarkPointComponent,
    CanvasRenderer,
]);

const AXIS_ANIM_MS = 450;

@customElement('jvmguard-echart')
export class JvmGuardEchart extends LitElement {

    static styles = css`
        :host {
            display: block;
            position: relative;
            width: 100%;
            height: 100%;
        }

        #chart {
            width: 100%;
            height: 100%;
        }

        .nav {
            position: absolute;
            top: 40px;
            bottom: 30px;
            width: 48px;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 0;
            border: none;
            border-radius: 4px;
            background: transparent;
            color: var(--vaadin-text-color, #555);
            font-size: 24px;
            line-height: 1;
            cursor: pointer;
            opacity: 0;
            transition: opacity 0.15s, background 0.15s;
        }

        :host(:hover) .nav {
            opacity: 0.35;
        }

        .nav:hover,
        .nav:focus-visible {
            opacity: 1;
            background: color-mix(in srgb, currentColor 14%, transparent);
            outline: none;
        }

        /* Positioned at the actual plot edges (set from JS), so they never overlap wide y-axis labels. */
        .nav-backward {
            left: var(--nav-left, 44px);
        }

        .nav-forward {
            right: var(--nav-right, 10px);
        }
    `;

    @property({attribute: false})
    model: TelemetryChartModel | null = null;

    @query('#chart')
    private chartElement!: HTMLDivElement;

    private chart?: echarts.ECharts;
    private resizeObserver?: ResizeObserver;
    private schemeObserver?: MutationObserver;
    private mediaQuery?: MediaQueryList;
    private readonly onSchemeChange = (): void => this.updateChart();

    private lastSignature = '';
    private lastSymbolSize = -1;
    private lastLogarithmic = false;
    private axisAnimFrame = 0;

    render(): TemplateResult {
        const model = this.model;
        return html`
          <div id="chart"></div>
          ${model?.canBackward
            ? html`<button class="nav nav-backward" title="Show earlier (Ctrl: full interval)"
                           @click=${(e: MouseEvent) => this.fire('jvmguard-nav-backward', {ctrl: e.ctrlKey})}>‹</button>`
            : nothing}
          ${model?.canForward
            ? html`<button class="nav nav-forward" title="Show later (Ctrl: full interval)"
                           @click=${(e: MouseEvent) => this.fire('jvmguard-nav-forward', {ctrl: e.ctrlKey})}>›</button>`
            : nothing}`;
    }

    firstUpdated(): void {
        this.chart = echarts.init(this.chartElement, undefined, {renderer: 'canvas'});
        this.resizeObserver = new ResizeObserver(() => {
            this.chart?.resize();
            this.refreshSymbolSize();
            this.positionNavOverlays();
        });
        this.resizeObserver.observe(this.chartElement);

        this.chart.getZr().on('dblclick', (event) => {
            const time = this.timeAtPixel(event.offsetX, event.offsetY);
            if (time !== null) {
                this.fire('jvmguard-zoom-in', {time});
            }
        });
        // Positional pixel→time (not point snapping) so clicks work across the whole plot, including
        // per-transaction line charts with no fill, and never round to a neighbouring data point.
        this.chart.getZr().on('click', (event) => {
            const time = this.timeAtPixel(event.offsetX, event.offsetY);
            if (time !== null) {
                this.fire('jvmguard-point-click', {time, seriesName: ''});
            }
        });
        this.chart.getZr().on('contextmenu', (event) => {
            const time = this.timeAtPixel(event.offsetX, event.offsetY);
            if (time !== null) {
                this.fire('jvmguard-context-time', {time});
            }
        });
        this.schemeObserver = new MutationObserver(this.onSchemeChange);
        this.schemeObserver.observe(document.documentElement, {
            attributes: true,
            attributeFilter: ['style', 'class', 'theme'],
        });
        this.mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
        this.mediaQuery.addEventListener('change', this.onSchemeChange);

        this.updateChart();
    }

    updated(changed: PropertyValues): void {
        if (changed.has('model')) {
            this.updateChart();
        }
    }

    disconnectedCallback(): void {
        super.disconnectedCallback();
        cancelAnimationFrame(this.axisAnimFrame);
        this.resizeObserver?.disconnect();
        this.schemeObserver?.disconnect();
        this.mediaQuery?.removeEventListener('change', this.onSchemeChange);
        this.chart?.dispose();
        this.chart = undefined;
    }

    private updateChart(): void {
        if (!this.chart) {
            return;
        }
        cancelAnimationFrame(this.axisAnimFrame);

        // Full replace when the set of series changes, so switching telemetry type doesn't morph
        // unrelated series into each other.
        const signature = this.seriesSignature();
        const replace = signature !== this.lastSignature;
        this.lastSignature = signature;
        const model = this.model;
        const logToggled = !replace && model != null && model.logarithmic !== this.lastLogarithmic;
        this.lastLogarithmic = model?.logarithmic ?? false;

        const density = symbolSizeFor(maxPointCount(model), this.chart.getWidth());
        this.lastSymbolSize = density;
        const fromExtent = this.currentYExtent();
        const option = buildTelemetryOption(model, isDarkScheme(), density);

        if (logToggled) {
            this.chart.setOption(
                {...option, animation: true, animationDurationUpdate: 600, animationEasingUpdate: 'cubicInOut'},
                false,
            );
        } else {
            this.chart.setOption(option, replace);
        }

        // Reflect the rendered series and time extent to the DOM so tests can wait for a specific
        // telemetry type / interval to actually be drawn (the chart itself is a canvas).
        this.setAttribute('data-series', model?.series.map((s) => s.name).join('|') ?? '');
        this.setAttribute('data-xextent', String(model ? model.xMax - model.xMin : 0));

        const toExtent = this.currentYExtent();
        if (toExtent) {
            this.fire('jvmguard-yaxis-extent', {min: toExtent[0], max: toExtent[1]});
        }

        const fromMax = fromExtent?.[1] ?? 0;
        const toMax = toExtent?.[1] ?? 0;
        const animate = model?.animate === true // only paging backward/forward in the same telemetry
            && !replace
            && !logToggled
            && model.yMax == null // not frozen
            && model.zeroBase
            && fromMax > 0
            && toMax > 0
            && Math.abs(fromMax - toMax) > 1e-6;
        if (animate) {
            this.animateRescale(fromMax, toMax);
        }
        this.positionNavOverlays();
    }

    private positionNavOverlays(): void {
        const model = this.model;
        if (!this.chart || !model || model.xMin <= 0 || model.xMax <= 0) {
            return;
        }
        try {
            const leftPx = this.chart.convertToPixel({xAxisIndex: 0}, model.xMin);
            const rightPx = this.chart.convertToPixel({xAxisIndex: 0}, model.xMax);
            if (typeof leftPx === 'number' && typeof rightPx === 'number') {
                this.style.setProperty('--nav-left', `${Math.max(0, Math.round(leftPx))}px`);
                this.style.setProperty('--nav-right', `${Math.max(0, Math.round(this.chart.getWidth() - rightPx))}px`);
            }
        } catch (e) {
            // Axis not laid out yet; keep the previous position.
        }
    }

    private currentYExtent(): [number, number] | null {
        if (!this.chart) {
            return null;
        }
        try {
            const axis = (this.chart as any).getModel().getComponent('yAxis', 0).axis;
            const [min, max] = axis.scale.getExtent();
            if (typeof min === 'number' && typeof max === 'number' && isFinite(min) && isFinite(max)) {
                return [min, max];
            }
        } catch (e) {
            // Internal model shape changed; skip the rescale animation.
        }
        return null;
    }

    private animateRescale(fromMax: number, toMax: number): void {
        const model = this.model;
        if (!this.chart || !model) {
            return;
        }
        const base = model.series.map((s) => s.points);
        const scaled = (m: number) =>
            base.map((points) => ({data: points.map((p) => [p.t, p.v == null ? null : p.v * m])}));
        const m0 = fromMax / toMax;
        // Apply the start (old-scale) state synchronously so the first paint isn't already at the target.
        this.chart.setOption({yAxis: {min: 0, max: toMax}, series: scaled(m0)}, false);
        const start = performance.now();
        const tick = (now: number): void => {
            const t = Math.min(1, (now - start) / AXIS_ANIM_MS);
            const e = t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2; // easeInOutQuad
            const m = m0 + (1 - m0) * e;
            this.chart?.setOption({yAxis: {min: 0, max: toMax}, series: scaled(m)}, false);
            if (t < 1) {
                this.axisAnimFrame = requestAnimationFrame(tick);
            } else {
                // Restore the true data and re-enable axis auto-scaling for the next update.
                this.chart?.setOption({yAxis: {min: 0, max: null}, series: scaled(1)}, false);
            }
        };
        this.axisAnimFrame = requestAnimationFrame(tick);
    }

    private seriesSignature(): string {
        const model = this.model;
        if (!model) {
            return '';
        }
        return `${model.stacked}|${model.series.map((s) => s.name).join(' ')}`;
    }

    private refreshSymbolSize(): void {
        if (!this.chart || !this.model) {
            return;
        }
        const size = symbolSizeFor(maxPointCount(this.model), this.chart.getWidth());
        if (size === this.lastSymbolSize) {
            return;
        }
        this.lastSymbolSize = size;
        this.chart.setOption({
            series: this.model.series.map(() => ({symbolSize: Math.max(size, 5), showSymbol: size > 0})),
        }, false);
    }

    private timeAtPixel(offsetX: number, offsetY: number): number | null {
        if (!this.chart) {
            return null;
        }
        if (!this.chart.containPixel('grid', [offsetX, offsetY])) {
            return null;
        }
        const value = this.chart.convertFromPixel('grid', [offsetX, offsetY]);
        if (Array.isArray(value) && typeof value[0] === 'number') {
            return value[0];
        }
        return null;
    }

    private fire(type: string, detail: Record<string, unknown>): void {
        this.dispatchEvent(new CustomEvent(type, {detail}));
    }
}
