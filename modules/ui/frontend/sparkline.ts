import {css, html, LitElement, nothing, svg, type SVGTemplateResult, type TemplateResult} from 'lit';
import {customElement, property} from 'lit/decorators.js';

interface SparklineState {
    data?: number[];
    graphWidth?: number;
    graphHeight?: number;
    displayRangeMin?: number;
    displayRangeMax?: number;
    pathWidth?: number;
    valueDotVisible?: boolean;
    minMaxDotsVisible?: boolean;
    minMaxLabelsVisible?: boolean;
    valueLabelVisible?: boolean;
    averageVisible?: boolean;
    normalRangeVisible?: boolean;
    normalRangeMin?: number;
    normalRangeMax?: number;
    explicitMinLabelValue?: string;
    explicitMaxLabelValue?: string;
    explicitCurrentLabelValue?: string;
    unitLabel?: string;
    currentValueOnly?: boolean;
    hideCurrentValue?: boolean;
}

const PAD = 4;
const OFFSET = PAD / 2;

@customElement('jvmguard-sparkline')
export class JvmGuardSparkline extends LitElement {

    static styles = css`
        :host {
            display: inline-flex;
            align-items: center;
            gap: 0.35em;
            font-size: 11px;
            line-height: 1.1;
            color: var(--jvmguard-sparkline-label, #999);
        }
        svg {
            display: block;
        }
        .path {
            fill: none;
            stroke: var(--jvmguard-sparkline-path, #999);
        }
        .average {
            stroke: var(--jvmguard-sparkline-average, #aaa);
        }
        .normal {
            fill: var(--jvmguard-sparkline-normal, #ddd);
        }
        .value-dot {
            fill: var(--jvmguard-sparkline-value-dot, #69f);
        }
        .minmax-dot {
            fill: var(--jvmguard-sparkline-path, #999);
        }
        .value {
            color: var(--jvmguard-sparkline-value, #69f);
            white-space: nowrap;
            font-size: 1.45em;
            font-weight: 500;
        }
        .unit {
            color: var(--jvmguard-sparkline-value, #69f);
            white-space: nowrap;
            margin-left: 0.2em;
        }
        .minmax {
            display: flex;
            flex-direction: column;
            font-size: 0.85em;
            text-align: right;
        }
    `;

    @property({attribute: false})
    state: SparklineState = {};

    render(): TemplateResult {
        const state = this.state ?? {};
        const currentLabel = state.explicitCurrentLabelValue ?? '';
        const unit = state.unitLabel ?? '';
        const valuePart = state.hideCurrentValue
            ? nothing
            : html`<span class="value">${currentLabel}</span>${unit ? html`<span class="unit">${unit}</span>` : nothing}`;
        if (state.currentValueOnly) {
            return html`${valuePart}`;
        }
        return html`${this.renderGraph(state)}${valuePart}${this.renderMinMax(state)}`;
    }

    private renderGraph(state: SparklineState): TemplateResult {
        const width = state.graphWidth ?? 100;
        const height = state.graphHeight ?? 20;
        const data = state.data ?? [];
        if (data.length === 0) {
            return html`<svg width=${width} height=${height}></svg>`;
        }

        let min = Number.MAX_VALUE;
        let max = -Number.MAX_VALUE;
        let minIndex = 0;
        let maxIndex = 0;
        let sum = 0;
        for (let i = 0; i < data.length; i++) {
            const v = data[i];
            if (v > max) {
                max = v;
                maxIndex = i;
            }
            if (v < min) {
                min = v;
                minIndex = i;
            }
            sum += v;
        }

        const effMin = state.displayRangeMin ?? 0;
        let effMax = state.displayRangeMax ?? max;
        if (effMax <= effMin) {
            effMax = effMin + 1;
        }
        const vScale = (height - PAD) / (effMax - effMin);
        const hScale = data.length > 1 ? (width - PAD) / (data.length - 1) : 0;
        const x = (i: number): number => i * hScale + OFFSET;
        const y = (v: number): number => height - (v - effMin) * vScale - OFFSET;

        const layers: (SVGTemplateResult | typeof nothing)[] = [];

        if (state.normalRangeVisible && state.normalRangeMin !== undefined && state.normalRangeMax !== undefined) {
            const top = y(state.normalRangeMax);
            const bottom = y(state.normalRangeMin);
            layers.push(svg`<rect class="normal" x="0" y=${top} width=${width} height=${Math.max(0, bottom - top)}></rect>`);
        }
        if (state.averageVisible) {
            const avgY = y(sum / data.length);
            layers.push(svg`<line class="average" x1="0" y1=${avgY} x2=${width} y2=${avgY}></line>`);
        }

        const points = data.map((v, i) => `${x(i)},${y(v)}`).join(' ');
        layers.push(svg`<polyline class="path" stroke-width=${state.pathWidth ?? 1} points=${points}></polyline>`);

        if (state.valueDotVisible !== false) {
            layers.push(svg`<circle class="value-dot" cx=${x(data.length - 1)} cy=${y(data[data.length - 1])} r="1.5"></circle>`);
        }
        if (state.minMaxDotsVisible) {
            layers.push(svg`<circle class="minmax-dot" cx=${x(minIndex)} cy=${y(min)} r="1.5"></circle>`);
            layers.push(svg`<circle class="minmax-dot" cx=${x(maxIndex)} cy=${y(max)} r="1.5"></circle>`);
        }

        return html`<svg width=${width} height=${height}>${layers}</svg>`;
    }

    private renderMinMax(state: SparklineState): TemplateResult | typeof nothing {
        if (state.minMaxLabelsVisible === false) {
            return nothing;
        }
        const maxLabel = state.explicitMaxLabelValue ?? '';
        const minLabel = state.explicitMinLabelValue ?? '';
        return html`<span class="minmax"><span>${maxLabel}</span><span>${minLabel}</span></span>`;
    }
}
