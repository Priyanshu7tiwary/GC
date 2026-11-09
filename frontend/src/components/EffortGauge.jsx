import { useEffect, useRef, useState } from 'react';
import { EFFORT_PARTS, effortTone, effortZone } from '../lib/effort.js';

// A large arc that fills to this week's effort, with one small dial per part beside it.
const CX = 120, CY = 120, R = 96;
const polar = (deg, r = R) => { const a = (deg * Math.PI) / 180; return [CX + r * Math.cos(a), CY - r * Math.sin(a)]; };
// score 0 → 180° (left), 100 → 0° (right)
const angleOf = (v) => 180 - v * 1.8;
const BIG = `M ${CX - R} ${CY} A ${R} ${R} 0 0 1 ${CX + R} ${CY}`;
const BIG_LEN = Math.PI * R;

export default function EffortGauge({ score, parts, delta }) {
  const [v, setV] = useState(0);       // needle position (animated by CSS)
  const [n, setN] = useState(0);       // counted-up number
  const raf = useRef(0);
  useEffect(() => {
    const reduce = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
    if (reduce) { setV(score); setN(score); return undefined; }
    const id = requestAnimationFrame(() => setV(score)); // after first paint, so the arc fills from empty
    const start = performance.now(), dur = 1400;
    const step = (t) => {
      const k = Math.min(1, (t - start) / dur);
      setN(Math.round(score * (1 - Math.pow(1 - k, 3))));
      if (k < 1) raf.current = requestAnimationFrame(step);
    };
    raf.current = requestAnimationFrame(step);
    return () => { cancelAnimationFrame(id); cancelAnimationFrame(raf.current); };
  }, [score]);
  const zone = effortZone(score);
  // the counted part furthest from full, weighted by how much it moves the score
  const weakestId = EFFORT_PARTS.filter((p) => parts[p.id].ratio != null && parts[p.id].ratio < 1)
    .sort((a, b) => (1 - parts[b.id].ratio) * b.weight - (1 - parts[a.id].ratio) * a.weight)[0]?.id;

  return (
    <div className="effort">
      <div className="effort-dial">
        <svg viewBox="0 0 240 136" role="img" aria-label={`Effort this week: ${score} out of 100, ${zone.label}`}>
          <path d={BIG} className="ea-track" />
          <path d={BIG} className="ea-fill" stroke={`var(--${zone.tone})`} style={{ strokeDasharray: BIG_LEN, strokeDashoffset: BIG_LEN * (1 - v / 100) }} />
          <text x={CX} y={CY - 26} className="ea-num">{n}</text>
          <text x={CX} y={CY - 6} className="ea-of">out of 100</text>
        </svg>
        <div className="effort-readout">
          <span className={`chip ${zone.tone}`}>{zone.label}</span>
          {delta != null && (
            <div className="small faint" style={{ marginTop: 6 }}>
              {delta === 0 ? 'Same as last week' : <span style={{ color: delta > 0 ? 'var(--good)' : 'var(--bad)', fontWeight: 600 }}>{delta > 0 ? '▲' : '▼'} {Math.abs(delta)} vs last week</span>}
            </div>
          )}
        </div>
      </div>
      <div className="effort-minis">
        {EFFORT_PARTS.map((p) => {
          const part = parts[p.id];
          const weakest = p.id === weakestId;
          return (
            <div key={p.id} className={`effort-mini ${weakest ? 'weakest' : ''} ${part.ratio == null ? 'off' : ''}`}>
              <MiniDial ratio={part.ratio} />
              <div className="em-text">
                <div className="em-label">{p.label}</div>
                <div className="small faint">{part.detail}</div>
                {weakest && <span className="chip bad em-flag">Needs the most work</span>}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

/** A small semicircle dial for one part of the effort score, coloured like the main gauge. */
function MiniDial({ ratio }) {
  const [v, setV] = useState(0);
  useEffect(() => {
    const id = requestAnimationFrame(() => setV(ratio ?? 0));
    return () => cancelAnimationFrame(id);
  }, [ratio]);
  const r = 26, cx = 32, cy = 32;
  const len = Math.PI * r;
  const tone = ratio == null ? 'var(--border)' : `var(--${effortTone(ratio * 100)})`;
  return (
    <svg viewBox="0 0 64 38" className="mini-dial" aria-hidden="true">
      <path d={`M ${cx - r} ${cy} A ${r} ${r} 0 0 1 ${cx + r} ${cy}`} className="md-track" />
      <path d={`M ${cx - r} ${cy} A ${r} ${r} 0 0 1 ${cx + r} ${cy}`} className="md-fill" stroke={tone}
        style={{ strokeDasharray: len, strokeDashoffset: len * (1 - v) }} />
      <text x={cx} y={cy - 3} className="md-num">{ratio == null ? '—' : `${Math.round((ratio) * 100)}%`}</text>
    </svg>
  );
}
