import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useStore, revisionBuckets, today, daysBetween, INTERVALS } from '../lib/store.jsx';
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
  const { revisions, reviseTopic, scheduleTopic, restartTopic, coverage, revisionsError } = useStore();
  const [busy, setBusy] = useState({});      // topicId -> true while a request is in flight
  const [notice, setNotice] = useState({});  // topicId -> { tone, text } from the server's answer
  const run = async (id, fn) => {
    if (busy[id]) return; // ignore double clicks while the first one is in flight
    setBusy((b) => ({ ...b, [id]: true }));
    try {
      const r = await fn();
      if (r?.message) setNotice((n) => ({ ...n, [id]: { tone: r.outcome === 'ADVANCED' ? 'good' : '', text: r.message } }));
    } catch (e) {
      setNotice((n) => ({ ...n, [id]: { tone: 'bad', text: e.message } }));
    } finally {
      setBusy((b) => ({ ...b, [id]: false }));
    }
  };
  const [tab, setTab] = useState('due');
  const [section, setSection] = useState('ALL');
  const [q, setQ] = useState('');
  const { overdue, due, upcoming } = revisionBuckets(revisions);
  const t0 = today();
  const mastered = Object.values(revisions).filter((r) => !r.paused && r.stage >= 4).length;
  const paused = Object.entries(revisions).filter(([, r]) => r.paused).map(([id, r]) => ({ id, ...r }));
  const thisWeek = upcoming.filter((r) => r.delta <= 7).length;

  const forecast = Array.from({ length: 14 }, (_, i) => {
    const day = t0 + i * DAY;
    const n = Object.values(revisions).filter((r) => !r.paused && (i === 0 ? r.delta <= 0 : r.delta === i)).length;
    return { day, n };
  });
  const fMax = Math.max(1, ...forecast.map((f) => f.n));

  const source = tab === 'due' ? [...overdue, ...due] : tab === 'upcoming' ? upcoming : tab === 'paused' ? paused : [...overdue, ...due, ...upcoming, ...paused];
  const rows = source
    .map((r) => ({ ...r, t: topic(r.id) }))
    .filter((r) => r.t && (section === 'ALL' || r.t.section === section) && r.t.name.toLowerCase().includes(q.toLowerCase()));

  const notCovered = ALL_TOPICS.filter((t) => coverage[t.id]?.status !== 2).length;

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1>Revision Tracker</h1>
          <p>Spaced repetition: each successful revision pushes the next one further out ({INTERVALS.join(' → ')} days). Only a due review moves the schedule, at most once a day; reviewing early counts as practice.</p>
        </div>
      </div>

      {revisionsError && <div className="card" style={{ marginBottom: 18 }}><p className="small" style={{ margin: 0, color: 'var(--bad)' }}>Couldn’t load your revision schedule: {revisionsError}</p></div>}
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
              {[['due', `Due (${overdue.length + due.length})`], ['upcoming', `Upcoming (${upcoming.length})`], ...(paused.length ? [['paused', `Paused (${paused.length})`]] : []), ['all', 'All']].map(([k, l]) => (
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
                  <div className="small faint">{r.t.section} · {r.t.chapter} · last revised {rel(daysBetween(t0, r.last)).toLowerCase()} · {r.reviewCount} review{r.reviewCount === 1 ? '' : 's'}</div>
                  {notice[r.id] && <div className={`small ${notice[r.id].tone === 'bad' ? 'bad-text' : notice[r.id].tone === 'good' ? 'good-text' : 'muted'}`} role="status">{notice[r.id].text}</div>}
                </div>
                <Ladder stage={r.stage} />
                <span className={`chip ${r.paused ? '' : r.delta < 0 ? 'bad' : r.delta === 0 ? 'warn' : ''}`} style={{ minWidth: 92, justifyContent: 'center' }}>
                  {r.paused ? 'Paused' : r.delta < 0 ? `${-r.delta}d overdue` : rel(r.delta)}
                </span>
                {r.paused ? (
                  <button className="btn sm" disabled={busy[r.id]} onClick={() => run(r.id, () => scheduleTopic(r.id))}>Resume</button>
                ) : (
                  <div style={{ display: 'flex', gap: 4, alignItems: 'center' }}>
                    {!r.canAdvanceToday && <span className="small faint" title={r.delta > 0 ? 'Not due yet: rating it counts as practice and does not move the schedule.' : 'Already reviewed today: further ratings count as practice.'}>Practice</span>}
                    <button className="btn sm" disabled={busy[r.id]} title="Struggled: back to 1 day" onClick={() => run(r.id, () => reviseTopic(r.id, 'hard'))}>Hard</button>
                    <button className="btn sm" disabled={busy[r.id]} title="Recalled with effort: next stage" onClick={() => run(r.id, () => reviseTopic(r.id, 'ok'))}>Good</button>
                    <button className="btn sm primary" disabled={busy[r.id]} title="Recalled easily: skip a stage" onClick={() => run(r.id, () => reviseTopic(r.id, 'easy'))}>Easy</button>
                    <button className="btn sm ghost" disabled={busy[r.id]} title="Start this topic's schedule again from day 1"
                      onClick={() => { if (confirm(`Restart ${r.t.name} from the first stage? Its history is kept.`)) run(r.id, () => restartTopic(r.id)); }}>Restart</button>
                  </div>
                )}
              </div>
            ))}
            {!rows.length && (
              <div style={{ textAlign: 'center', padding: '36px 0' }}>
                <div style={{ fontSize: 28 }}>🎉</div>
                <h3 style={{ marginTop: 6 }}>Nothing here</h3>
                <p className="muted small">{tab === 'due' ? 'You’re caught up. Topics join here when you mark them complete in Coverage.' : 'No topics match this filter.'}</p>
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
            <h2 style={{ marginBottom: 6 }}>What gets revised</h2>
            <p className="muted small" style={{ margin: 0 }}>Only topics you have marked complete in <Link to="/coverage">Syllabus Coverage</Link>. Each joins this schedule the day you complete it, with its first revision tomorrow. Topics you haven’t covered stay in Coverage and are never marked for revision. {notCovered} of {ALL_TOPICS.length} topics aren’t covered yet.</p>
            {notCovered > 0 && (
              <details className="uncovered" style={{ marginTop: 10 }}>
                <summary className="small"><b>Uncovered topics ({notCovered})</b></summary>
                {Object.keys(SECTIONS).map((sec) => {
                  const list = ALL_TOPICS.filter((t) => t.section === sec && coverage[t.id]?.status !== 2);
                  if (!list.length) return null;
                  return (
                    <div key={sec} style={{ marginTop: 10 }}>
                      <div className="small" style={{ fontWeight: 600, display: 'flex', alignItems: 'center', gap: 6 }}>
                        <i className="dot" style={{ background: SECTIONS[sec].color }} />{sec} · {list.length}
                      </div>
                      <ul className="small muted" style={{ margin: '4px 0 0', paddingLeft: 18 }}>
                        {list.map((t) => (
                          <li key={t.id}>{t.name} <span className="faint">· {t.chapter}{coverage[t.id]?.status === 1 ? ' · in progress' : ''}</span></li>
                        ))}
                      </ul>
                    </div>
                  );
                })}
              </details>
            )}
          </div>

          <div className="card">
            <h2 style={{ marginBottom: 10 }}>How rating works</h2>
            <div className="list small">
              <div className="row"><b style={{ width: 44 }}>Hard</b><span className="muted">Back to stage 1: revise again tomorrow.</span></div>
              <div className="row"><b style={{ width: 44 }}>Good</b><span className="muted">Move up one stage on the ladder.</span></div>
              <div className="row"><b style={{ width: 44 }}>Easy</b><span className="muted">Skip ahead two stages (up to 60 days).</span></div>
              <div className="row"><span className="muted">The next date counts from the day you review, so a late review isn’t overdue again straight away. Reviewing before a topic is due, or a second time the same day, is recorded as practice and leaves the schedule as it is.</span></div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
