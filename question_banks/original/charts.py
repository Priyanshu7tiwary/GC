"""Tiny SVG chart writer for DILR sets (bar, stacked bar, line and pie charts), with no dependencies.

Charts are drawn on a white card with dark text so they read the same in the app's light and dark themes, and
every data point carries its value label, as CAT charts usually do, so no reading-off-the-gridline guesswork.
"""
import math
from html import escape

PALETTE = ['#2f6db5', '#e07b39', '#3f9a5a', '#8e5ea2', '#c49a1a', '#5f7385']
W, H = 680, 400
FONT = "font-family='Helvetica, Arial, sans-serif'"


def _svg(body, w=W, h=H):
    return (f"<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 {w} {h}' width='{w}' height='{h}' {FONT}>"
            f"<rect width='{w}' height='{h}' fill='#ffffff' rx='8'/>{body}</svg>\n")


def _text(x, y, s, size=13, anchor='middle', weight='normal', fill='#1d2430'):
    return f"<text x='{x:.1f}' y='{y:.1f}' font-size='{size}' text-anchor='{anchor}' font-weight='{weight}' fill='{fill}'>{escape(str(s))}</text>"


def _legend(names, x0, y):
    out, x = [], x0
    for i, n in enumerate(names):
        out.append(f"<rect x='{x}' y='{y - 10}' width='12' height='12' rx='2' fill='{PALETTE[i]}'/>")
        out.append(_text(x + 17, y, n, 12, 'start'))
        x += 30 + 7.5 * len(n)
    return ''.join(out)


def _axes(title, ylabel, ymax, step, left=70, top=58, bottom=340, right=650):
    out = [_text(W / 2, 26, title, 15, weight='bold')]
    v = 0
    while v <= ymax:
        y = bottom - (bottom - top) * v / ymax
        out.append(f"<line x1='{left}' y1='{y:.1f}' x2='{right}' y2='{y:.1f}' stroke='#e3e7ee'/>")
        out.append(_text(left - 8, y + 4, f'{v:g}', 11, 'end', fill='#5a6472'))
        v += step
    out.append(f"<line x1='{left}' y1='{bottom}' x2='{right}' y2='{bottom}' stroke='#8a94a3'/>")
    out.append(f"<text transform='translate(18 {(top + bottom) / 2}) rotate(-90)' font-size='12' text-anchor='middle' fill='#5a6472'>{escape(ylabel)}</text>")
    return out


def bar(title, categories, series, ylabel, ymax, step):
    """Grouped bars: `series` maps a name to one value per category."""
    left, right, top, bottom = 70, 650, 58, 340
    out = _axes(title, ylabel, ymax, step, left, top, bottom, right)
    names = list(series)
    gw = (right - left) / len(categories)
    bw = min(34, gw * 0.8 / len(names))
    for ci, cat in enumerate(categories):
        x0 = left + gw * ci + (gw - bw * len(names)) / 2
        for si, name in enumerate(names):
            v = series[name][ci]
            h = (bottom - top) * v / ymax
            x = x0 + si * bw
            out.append(f"<rect x='{x:.1f}' y='{bottom - h:.1f}' width='{bw - 2:.1f}' height='{h:.1f}' fill='{PALETTE[si]}'/>")
            out.append(_text(x + (bw - 2) / 2, bottom - h - 4, f'{v:g}', 10))
        out.append(_text(left + gw * ci + gw / 2, bottom + 18, cat, 12))
    out.append(_legend(names, left, bottom + 46))
    return _svg(''.join(out))


def stacked(title, categories, series, ylabel, ymax, step):
    """Stacked bars: each category's bar is split by series; segment values and the bar total are labelled."""
    left, right, top, bottom = 70, 650, 58, 340
    out = _axes(title, ylabel, ymax, step, left, top, bottom, right)
    names = list(series)
    gw = (right - left) / len(categories)
    bw = gw * 0.5
    for ci, cat in enumerate(categories):
        x = left + gw * ci + (gw - bw) / 2
        base = 0
        for si, name in enumerate(names):
            v = series[name][ci]
            y1 = bottom - (bottom - top) * (base + v) / ymax
            h = (bottom - top) * v / ymax
            out.append(f"<rect x='{x:.1f}' y='{y1:.1f}' width='{bw:.1f}' height='{h:.1f}' fill='{PALETTE[si]}' stroke='#ffffff'/>")
            if h >= 14:
                out.append(_text(x + bw / 2, y1 + h / 2 + 4, f'{v:g}', 11, fill='#ffffff', weight='bold'))
            base += v
        out.append(_text(x + bw / 2, bottom - (bottom - top) * base / ymax - 5, f'{base:g}', 11, weight='bold'))
        out.append(_text(x + bw / 2, bottom + 18, cat, 12))
    out.append(_legend(names, left, bottom + 46))
    return _svg(''.join(out))


def line(title, categories, series, ylabel, ymin, ymax, step):
    left, right, top, bottom = 70, 650, 58, 340
    out = [_text(W / 2, 26, title, 15, weight='bold')]
    sy = lambda v: bottom - (bottom - top) * (v - ymin) / (ymax - ymin)
    v = ymin
    while v <= ymax:
        out.append(f"<line x1='{left}' y1='{sy(v):.1f}' x2='{right}' y2='{sy(v):.1f}' stroke='#e3e7ee'/>")
        out.append(_text(left - 8, sy(v) + 4, f'{v:g}', 11, 'end', fill='#5a6472'))
        v += step
    out.append(f"<text transform='translate(18 {(top + bottom) / 2}) rotate(-90)' font-size='12' text-anchor='middle' fill='#5a6472'>{escape(ylabel)}</text>")
    gw = (right - left) / len(categories)
    sx = lambda i: left + gw * i + gw / 2
    for i, c in enumerate(categories):
        out.append(_text(sx(i), bottom + 18, c, 12))
    for si, (name, vals) in enumerate(series.items()):
        pts = ' '.join(f'{sx(i):.1f},{sy(v):.1f}' for i, v in enumerate(vals))
        out.append(f"<polyline points='{pts}' fill='none' stroke='{PALETTE[si]}' stroke-width='2.5'/>")
        for i, v in enumerate(vals):
            marker = (f"<circle cx='{sx(i):.1f}' cy='{sy(v):.1f}' r='4.5' fill='{PALETTE[si]}'/>" if si % 2 == 0 else
                      f"<rect x='{sx(i) - 4:.1f}' y='{sy(v) - 4:.1f}' width='8' height='8' fill='{PALETTE[si]}'/>")
            out.append(marker)
            # label above for the first series, below for the second, so labels of close points do not collide
            out.append(_text(sx(i), sy(v) - 9 if si % 2 == 0 else sy(v) + 19, f'{v:g}', 11, fill=PALETTE[si], weight='bold'))
    out.append(_legend(list(series), left, bottom + 46))
    return _svg(''.join(out))


def pies(title, charts):
    """One or two pie charts side by side. `charts` is a list of (subtitle, {label: percent})."""
    w = 340 * len(charts) + 20
    h = 400
    out = [_text(w / 2, 26, title, 15, weight='bold')]
    for k, (sub, parts) in enumerate(charts):
        cx, cy, r = 180 + 340 * k, 205, 105
        out.append(_text(cx, 58, sub, 13, weight='bold'))
        a = -math.pi / 2
        total = sum(parts.values())
        assert abs(total - 100) < 1e-9, f'{sub}: shares add up to {total}'
        for i, (label, pct) in enumerate(parts.items()):
            da = 2 * math.pi * pct / 100
            x1, y1 = cx + r * math.cos(a), cy + r * math.sin(a)
            x2, y2 = cx + r * math.cos(a + da), cy + r * math.sin(a + da)
            large = 1 if da > math.pi else 0
            out.append(f"<path d='M{cx},{cy} L{x1:.1f},{y1:.1f} A{r},{r} 0 {large} 1 {x2:.1f},{y2:.1f} Z' fill='{PALETTE[i]}' stroke='#ffffff' stroke-width='2'/>")
            mid = a + da / 2
            lx, ly = cx + (r + 18) * math.cos(mid), cy + (r + 18) * math.sin(mid)
            anchor = 'start' if math.cos(mid) > 0.15 else 'end' if math.cos(mid) < -0.15 else 'middle'
            out.append(_text(lx, ly + 4, f'{label} {pct:g}%', 12, anchor))
            a += da
    return _svg(''.join(out), w, h)
