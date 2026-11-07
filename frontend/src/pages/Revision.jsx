import { useState } from 'react';
import { useStore, revisionBuckets, today, INTERVALS } from '../lib/store.jsx';
import { ALL_TOPICS, SECTIONS } from '../data/syllabus.js';
import { IconSearch } from '../components/Icons.jsx';

const DAY = 86400000;
const rel = (d) => (d === 0 ? 'Today' : d === 1 ? 'Tomorrow' : d === -1 ? 'Yesterday' : d < 0 ? `${-d} days ago` : `in ${d} days`);
const topic = (id) => ALL_TOPICS.find((t) => t.id === id);

function Ladder({ stage }) {
  return (
    <div style={{ display: 'flex', gap: 3 }} title={`Stage ${stage + 1} of ${INTERVALS.length} · interval ${INTERVALS[stage]}d`}>
      {INTERVALS.map((_, i) => (
        <span key={i} style={{ width: 14, height: 6, borderRadius: 3, background: i <= stage ? 'var(--accent)' : 'var(--surface-2)' }} />
      ))}
    </div>
  );
}

export default function Revision() {
  const { revisions, reviseTopic, coverage } = useStore();
  const [tab, setTab] = useState('due');
  const [section, setSection] = useState('ALL');
  const [q, setQ] = useState('');
  const [adding, setAdding] = useState('');
  const { overdue, due, upcoming } = revisionBuckets(revisions);
  const t0 = today();
  const mastered = Object.values(revisions).filter((r) => r.stage >= 4).length;
  const thisWeek = upcoming.filter((r) => r.delta <= 7).length;

  const forecast = Array.from({ length: 14 }, (_, i) => {
    const day = t0 + i * DAY;
    const n = Object.values(revisions).filter((r) => (i === 0 ? r.next <= day : r.next === day)).length;
    return { day, n };
  });
  const fMax = Math.max(1, ...forecast.map((f) => f.n));

  const source = tab === 'due' ? [...overdue, ...due] : tab === 'upcoming' ? upcoming : [...overdue, ...due, ...upcoming];
  const rows = source
    .map((r) => ({ ...r, t: topic(r.id) }))
    .filter((r) => r.t && (section === 'ALL' || r.t.section === section) && r.t.name.toLowerCase().includes(q.toLowerCase()));

  const notEnrolled = ALL_TOPICS.filter((t) => !revisions[t.id]);

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1>Revision Tracker</h1>
          <p>Spaced repetition: each successful revision pushes the next one further out ({INTERVALS.join(' → ')} days).</p>
        </div>
      </div>

      <div className="grid g-4" style={{ marginBottom: 18 }}>
        <div className="card"><div className="stat-label">Overdue</div><div className="stat-value" style={{ color: overdue.length ? 'var(--bad)' : undefined }}>{overdue.length}</div><div className="stat-sub">Revise these first</div></div>
        <div className="card"><div className="stat-label">Due today</div><div className="stat-value">{due.length}</div><div className="stat-sub">≈ {(overdue.length + due.length) * 15} min of work</div></div>
        <div className="card"><div className="stat-label">Coming up this week</div><div className="stat-value">{thisWeek}</div><div className="stat-sub">{upcoming.length} scheduled in total</div></div>
        <div className="card"><div className="stat-label">Long-term retained</div><div className="stat-value">{mastered}</div><div className="stat-sub">Reached 30-day interval or beyond</div></div>
      </div>

      <div className="grid g-main">
        <div className="card">
          <div className="card-head" style={{ flexWrap: 'wrap' }}>
            <div className="tabs">
              {[['due', `Due (${overdue.length + due.length})`], ['upcoming', `Upcoming (${upcoming.length})`], ['all', 'All']].map(([k, l]) => (
                <button key={k} className={`tab ${tab === k ? 'active' : ''}`} onClick={() => setTab(k)}>{l}</button>
              ))}
            </div>
            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
              <div style={{ position: 'relative' }}>
                <IconSearch width="15" style={{ position: 'absolute', left: 10, top: 10, color: 'var(--text-3)' }} />
                <input className="input" style={{ paddingLeft: 32, width: 180, padding: '7px 10px 7px 32px' }} placeholder="Search topics" value={q} onChange={(e) => setQ(e.target.value)} />
              </div>
              <select className="input" style={{ width: 'auto', padding: '7px 10px' }} value={section} onChange={(e) => setSection(e.target.value)}>
                <option value="ALL">All sections</option>
                {Object.keys(SECTIONS).map((s) => <option key={s}>{s}</option>)}
              </select>
            </div>
          </div>

          <div className="list">
            {rows.map((r) => (
              <div className="row" key={r.id} style={{ flexWrap: 'wrap' }}>
                <i className="dot" style={{ background: SECTIONS[r.t.section].color }} />
                <div className="grow" style={{ minWidth: 180 }}>
                  <div style={{ fontWeight: 500 }}>{r.t.name}</div>
                  <div className="small faint">{r.t.section} · {r.t.chapter} · last revised {rel(Math.round((r.last - t0) / DAY)).toLowerCase()} · {r.history.length}×</div>
                </div>
                <Ladder stage={r.stage} />
                <span className={`chip ${r.delta < 0 ? 'bad' : r.delta === 0 ? 'warn' : ''}`} style={{ minWidth: 92, justifyContent: 'center' }}>
                  {r.delta < 0 ? `${-r.delta}d overdue` : rel(r.delta)}
                </span>
                <div style={{ display: 'flex', gap: 4 }}>
                  <button className="btn sm" title="Struggled — reset to 1 day" onClick={() => reviseTopic(r.id, 'hard')}>Hard</button>
                  <button className="btn sm" title="Recalled with effort — next stage" onClick={() => reviseTopic(r.id, 'ok')}>Good</button>
                  <button className="btn sm primary" title="Recalled easily — skip a stage" onClick={() => reviseTopic(r.id, 'easy')}>Easy</button>
                </div>
              </div>
            ))}
            {!rows.length && (
              <div style={{ textAlign: 'center', padding: '36px 0' }}>
                <div style={{ fontSize: 28 }}>🎉</div>
                <h3 style={{ marginTop: 6 }}>Nothing here</h3>
                <p className="muted small">{tab === 'due' ? 'You’re caught up. Check upcoming revisions or add a topic.' : 'No topics match this filter.'}</p>
              </div>
            )}
          </div>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
          <div className="card">
            <div className="card-head"><div><h2>14-day forecast</h2><p>Topics due per day (today includes overdue)</p></div></div>
            <div style={{ display: 'flex', alignItems: 'flex-end', gap: 4, height: 120 }}>
              {forecast.map((f, i) => (
                <div key={f.day} style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4, height: '100%', justifyContent: 'flex-end' }}
                  title={`${new Date(f.day).toDateString()}: ${f.n} topics`}>
                  {f.n > 0 && <span className="small num faint" style={{ fontSize: 10 }}>{f.n}</span>}
                  <span style={{ width: '100%', maxWidth: 18, height: `${(f.n / fMax) * 85}%`, minHeight: f.n ? 3 : 0, background: i === 0 ? 'var(--accent)' : 'var(--s-varc)', borderRadius: '4px 4px 0 0', opacity: i === 0 ? 1 : 0.75 }} />
                </div>
              ))}
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 6 }} className="small faint">
              <span>Today</span><span>+7d</span><span>+13d</span>
            </div>
          </div>

          <div className="card">
            <div className="card-head"><div><h2>Add to schedule</h2><p>Studied something today? Start its revision cycle.</p></div></div>
            <div style={{ display: 'flex', gap: 8 }}>
              <select className="input" value={adding} onChange={(e) => setAdding(e.target.value)}>
                <option value="">Choose a topic…</option>
                {Object.keys(SECTIONS).map((s) => (
                  <optgroup key={s} label={s}>
                    {notEnrolled.filter((t) => t.section === s).map((t) => (
                      <option key={t.id} value={t.id}>{t.name}{coverage[t.id]?.status === 1 ? ' (in progress)' : ''}</option>
                    ))}
                  </optgroup>
                ))}
              </select>
              <button className="btn primary" disabled={!adding} onClick={() => { reviseTopic(adding, 'hard'); setAdding(''); }}>Add</button>
            </div>
          </div>

          <div className="card">
            <h2 style={{ marginBottom: 10 }}>How rating works</h2>
            <div className="list small">
              <div className="row"><b style={{ width: 44 }}>Hard</b><span className="muted">Back to stage 1 — revise again tomorrow.</span></div>
              <div className="row"><b style={{ width: 44 }}>Good</b><span className="muted">Move up one stage on the ladder.</span></div>
              <div className="row"><b style={{ width: 44 }}>Easy</b><span className="muted">Skip ahead two stages.</span></div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
