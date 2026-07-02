// Sizes a <vaadin-select> to its widest option. Vaadin renders the options in a detached overlay, so the
// field cannot size to them on its own (unlike a native <select>). We measure the labels with the field's
// actual proportional font via a canvas and add the field's real chrome (padding + border + the toggle
// chevron), so the widest selected value is never clipped by the chevron.

type Measurable
    = HTMLElement & { shadowRoot: ShadowRoot | null };

let canvas: HTMLCanvasElement | undefined;

function num(value: string): number {
    return parseFloat(value) || 0;
}

function measure(el: Measurable, labels: string[]): void {
    // Parts are space-separated tokens (the toggle is part="field-button toggle-button"), so match with ~=.
    const field = el.shadowRoot?.querySelector('[part~="input-field"]') as HTMLElement | null;
    const cs = getComputedStyle(field ?? el);
    const ctx = (canvas ??= document.createElement('canvas')).getContext('2d')!;
    ctx.font = cs.font && cs.font.trim() ? cs.font : `${cs.fontWeight} ${cs.fontSize} ${cs.fontFamily}`;
    try {
        if ('letterSpacing' in ctx) {
            (ctx as unknown as { letterSpacing: string }).letterSpacing = cs.letterSpacing;
        }
    } catch {
        // letterSpacing on canvas is unsupported on older browsers; ignore.
    }

    let maxText = 0;
    for (const label of labels) {
        maxText = Math.max(maxText, ctx.measureText(label).width);
    }

    let chrome = 24;
    if (field) {
        const f = getComputedStyle(field);
        chrome = num(f.paddingLeft) + num(f.paddingRight) + num(f.borderLeftWidth) + num(f.borderRightWidth)
            + num(f.columnGap);
        const toggle = el.shadowRoot?.querySelector('[part~="toggle-button"]') as HTMLElement | null;
        if (toggle) {
            // The toggle width is var(--vaadin-icon-size, 1lh) and --vaadin-icon-size is unset, so it falls
            // back to 1lh (the toggle's own line-height). getComputedStyle(width) may report "1lh" unresolved
            // (parses to 1) and getBoundingClientRect is 0 before the field is laid out, so resolve from
            // line-height with a font-size fallback for "normal".
            const t = getComputedStyle(toggle);
            const lineHeight = num(t.lineHeight) || num(t.fontSize) * 1.2 || num(cs.fontSize) * 1.2;
            const iconSize = Math.max(toggle.getBoundingClientRect().width, num(t.width), lineHeight);
            chrome += iconSize + num(t.paddingLeft) + num(t.paddingRight)
                + num(t.borderLeftWidth) + num(t.borderRightWidth) + num(t.marginLeft) + num(t.marginRight);
        }
    }

    el.style.width = `${Math.ceil(maxText + chrome + 4)}px`;
}

export function autoWidthSelect(el: Measurable, joinedLabels: string, separator: string): void {
    const labels = (joinedLabels || '').split(separator);
    const run = () => measure(el, labels);
    if (document.fonts?.ready) {
        document.fonts.ready.then(() => requestAnimationFrame(run));
    } else {
        requestAnimationFrame(run);
    }
}

(window as unknown as { JvmGuard: Record<string, unknown> }).JvmGuard ??= {};
(window as unknown as { JvmGuard: Record<string, unknown> }).JvmGuard.autoWidthSelect = autoWidthSelect;
