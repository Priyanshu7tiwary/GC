import { useRef, useState } from 'react';

function niceMax(v) {
  if (v <= 0) return 10;
  const p = Math.pow(10, Math.floor(Math.log10(v)));
  const n = v / p;
  return (n <= 1 ? 1 : n <= 2 ? 2 : n <= 5 ? 5 : 10) * p;
}

/**
 * Multi-series line chart, one y-axis, crosshair + tooltip on hover.
 * data: [{ label, ...values }], series: [{ key, label, color }]
 */
export function LineChart({ data, series, height = 220, yMax, format = (v) => v }) {
  const ref = useRef(null);
  const [hover, setHover] = useState(null);
  const W = 640, H = height, pad = { l: 36, r: 12, t: 12, b: 26 };
  const max = yMax ?? niceMax(Math.max(...data.flatMap((d) => series.map((s) => d[s.key] ?? 0))) * 1.1);
  const x = (i) => pad.l + (data.length === 1 ? (W - pad.l - pad.r) / 2 : (i * (W - pad.l - pad.r)) / (data.length - 1));
  const y = (v) => pad.t + (1 - v / max) * (H - pad.t - pad.b);
  const ticks = [0, 0.25, 0.5, 0.75, 1].map((f) => Math.round(max * f));

  const onMove = (e) => {
    const r = ref.current.getBoundingClientRect();
    const px = ((e.clientX - r.left) / r.width) * W;
    let best = 0;
    data.forEach((_, i) => { if (Math.abs(x(i) - px) < Math.abs(x(best) - px)) best = i; });
    setHover(best);
  };

  return (
    <div className="chart">
      {series.length > 1 && (
        <div className="legend" style={{ marginBottom: 10 }}>
          {series.map((s) => (
            <span key={s.key}><i className="dot" style={{ background: s.color }} />{s.label}</span>
          ))}
        </div>
      )}
      <svg ref={ref} viewBox={`0 0 ${W} ${H}`} onMouseMove={onMove} onMouseLeave={() => setHover(null)} role="img">
        <g className="axis">
          {ticks.map((t) => (
            <g key={t}>
              <line className="gridline" x1={pad.l} x2={W - pad.r} y1={y(t)} y2={y(t)} />
              <text x={pad.l - 8} y={y(t) + 4} textAnchor="end">{t}</text>
            </g>
          ))}
          {data.map((d, i) => (
            <text key={i} x={x(i)} y={H - 6} textAnchor="middle">{d.label}</text>
          ))}
        </g>
        {hover != null && <line x1={x(hover)} x2={x(hover)} y1={pad.t} y2={H - pad.b} stroke="var(--text-3)" strokeDasharray="3 3" />}
        {series.map((s) => {
          const pts = data.map((d, i) => [x(i), y(d[s.key] ?? 0)]);
          return (
            <g key={s.key}>
              <path d={pts.map((p, i) => `${i ? 'L' : 'M'}${p[0]},${p[1]}`).join('')} fill="none" stroke={s.color} strokeWidth="2" strokeLinejoin="round" />
              {pts.map((p, i) => (
                <circle key={i} cx={p[0]} cy={p[1]} r={hover === i ? 5 : 4} fill={s.color} stroke="var(--surface)" strokeWidth="2" />
              ))}
            </g>
          );
        })}
        {/* direct labels on the last point, nudged apart so they never collide */}
        {series.length <= 4 && data.length > 0 && (() => {
          const last = data[data.length - 1];
          const labels = series.map((s) => ({ key: s.key, v: last[s.key] ?? 0, y: y(last[s.key] ?? 0) - 10 })).sort((a, b) => a.y - b.y);
          for (let i = 1; i < labels.length; i++) if (labels[i].y - labels[i - 1].y < 13) labels[i].y = labels[i - 1].y + 13;
          return labels.map((l) => (
            <text key={l.key} x={x(data.length - 1) - 8} y={l.y} textAnchor="end" fontSize="11" fontWeight="600" fill="var(--text)">{format(l.v)}</text>
          ));
        })()}
      </svg>
      {hover != null && (
        <div className="tooltip" style={{ left: `${(x(hover) / W) * 100}%`, top: `${(y(Math.max(...series.map((s) => data[hover][s.key] ?? 0))) / H) * 100}%` }}>
          <div style={{ fontWeight: 600, marginBottom: 4 }}>{data[hover].tip || data[hover].label}</div>
          {series.map((s) => (
            <div key={s.key} style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
              <i className="dot" style={{ background: s.color }} />
              <span className="muted">{s.label}</span>
              <b className="num" style={{ marginLeft: 'auto' }}>{format(data[hover][s.key])}</b>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

/** Progress ring for a single headline percentage */
export function Ring({ value, size = 96, stroke = 9, color = 'var(--accent)', label, sub }) {
  const r = (size - stroke) / 2;
  const c = 2 * Math.PI * r;
  return (
    <div style={{ position: 'relative', width: size, height: size, flex: 'none' }}>
      <svg width={size} height={size} style={{ transform: 'rotate(-90deg)' }}>
        <circle cx={size / 2} cy={size / 2} r={r} stroke="var(--surface-2)" strokeWidth={stroke} fill="none" />
        <circle cx={size / 2} cy={size / 2} r={r} stroke={color} strokeWidth={stroke} fill="none" strokeLinecap="round"
          strokeDasharray={c} strokeDashoffset={c * (1 - Math.min(100, value) / 100)} style={{ transition: 'stroke-dashoffset .5s' }} />
      </svg>
      <div style={{ position: 'absolute', inset: 0, display: 'grid', placeItems: 'center', textAlign: 'center' }}>
        <div>
          <div className="num" style={{ fontWeight: 700, fontSize: size > 80 ? 20 : 14 }}>{label ?? `${Math.round(value)}%`}</div>
          {sub && <div className="faint" style={{ fontSize: 11 }}>{sub}</div>}
        </div>
      </div>
    </div>
  );
}

/** GitHub-style activity heatmap (sequential single hue) */
export function Heatmap({ activity, weeks = 16 }) {
  const [hover, setHover] = useState(null);
  const DAY = 86400000;
  const t = new Date(); t.setHours(0, 0, 0, 0);
  const end = t.getTime();
  const first = end - ((weeks - 1) * 7 + t.getDay()) * DAY;
  const cells = [];
  for (let d = first; d <= end; d += DAY) cells.push(d);
  const shade = (m) => {
    if (!m) return 'var(--surface-2)';
    const steps = ['#cde2fb', '#86b6ef', '#3987e5', '#1c5cab'];
    return steps[m < 45 ? 0 : m < 100 ? 1 : m < 170 ? 2 : 3];
  };
  return (
    <div className="chart" style={{ overflowX: 'auto' }}>
      <div className="heatmap">
        {cells.map((d) => (
          <i key={d} style={{ background: shade(activity[d]) }}
            onMouseEnter={() => setHover(d)} onMouseLeave={() => setHover(null)}
            title={`${new Date(d).toDateString()} · ${activity[d] || 0} min`} />
        ))}
      </div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 10, gap: 12, flexWrap: 'wrap' }}>
        <span className="small faint">{hover ? `${new Date(hover).toDateString()} · ${activity[hover] || 0} min studied` : 'Last 16 weeks'}</span>
        <span className="legend small">Less {['var(--surface-2)', '#cde2fb', '#86b6ef', '#3987e5', '#1c5cab'].map((c) => <i key={c} className="dot" style={{ background: c, borderRadius: 3, width: 10, height: 10 }} />)} More</span>
      </div>
    </div>
  );
}

/** Horizontal segmented bar (stacked parts of a whole) */
export function SegBar({ parts, height = 8 }) {
  const total = parts.reduce((a, p) => a + p.value, 0) || 1;
  return (
    <div className="bar" style={{ height }}>
      {parts.filter((p) => p.value > 0).map((p, i) => (
        <span key={i} title={`${p.label}: ${p.value}`} style={{ width: `${(p.value / total) * 100}%`, background: p.color }} />
      ))}
    </div>
  );
}
