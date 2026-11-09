import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useStore, revisionBuckets, today, daysBetween } from '../lib/store.jsx';
import { SECTIONS, ALL_TOPICS } from '../data/syllabus.js';
import { findMock } from '../data/mocks.js';
import { useCatalog, usePapers } from '../lib/catalog.js';
import { LineChart, SegBar } from '../components/Charts.jsx';
import {
  SECTION_KEYS, sectionStats, isFullLength, movingAverage, fmtDate, pct, mins,
  useAttemptQuestions, topicReview, nextActions,
} from '../lib/analytics.js';

const RANGES = [['5', 'Last 5'], ['10', 'Last 10'], ['all', 'All']];
const plain = (t) => (t || '').replace(/\[\[img:[^\]]+\]\]/g, '[figure]').replace(/\\\(|\\\)/g, '').replace(/\s+/g, ' ').trim();

/** A card whose chart can be swapped for its table: every value stays readable without hovering. */
function ChartCard({ title, sub, table, children, actions }) {
  const [view, setView] = useState('chart');
  return (
    <div className="card">
      <div className="card-head wrap">
        <div><h2>{title}</h2>{sub && <p>{sub}</p>}</div>
        <div className="card-actions">
          {actions}
          {table && (
            <div className="tabs" role="group" aria-label={`${title} view`}>
              <button className={`tab ${view === 'chart' ? 'active' : ''}`} aria-pressed={view === 'chart'} onClick={() => setView('chart')}>Chart</button>
              <button className={`tab ${view === 'table' ? 'active' : ''}`} aria-pressed={view === 'table'} onClick={() => setView('table')}>Table</button>
            </div>
          )}
        </div>
      </div>
      {view === 'chart' || !table ? children : table}
    </div>
  );
}

function Delta({ value, unit = '' }) {
  if (value == null) return <span className="stat-sub">first mock</span>;
  if (value === 0) return <span className="stat-sub">no change vs previous</span>;
  const up = value > 0;
  return <span className={`chip ${up ? 'good' : 'bad'}`} style={{ padding: '1px 7px' }}>{up ? '▲' : '▼'} {up ? '+' : '−'}{Math.abs(value)}{unit} vs previous</span>;
}

function Empty({ children }) {
  return <p className="muted" style={{ margin: 0 }}>{children}</p>;
}

export default function Analytics() {
  const { attempts, revisions, revisionLog, customMocks, targetPercentile, examDate, updateSettings } = useStore();
  const { papers } = usePapers();
  const { list: sectionals } = useCatalog('SECTIONAL');
  const [range, setRange] = useState('10');
  const [section, setSection] = useState('all');
  const [scoreMode, setScoreMode] = useState('raw');
  const [openTopic, setOpenTopic] = useState(null);
  const [allTopics, setAllTopics] = useState(false);

  const sorted = useMemo(() => [...attempts].sort((a, b) => a.date - b.date), [attempts]);
  const inRange = range === 'all' ? sorted : sorted.slice(-Number(range));
  const fulls = inRange.filter(isFullLength);
  const shownSections = section === 'all' ? SECTION_KEYS : [section];

  /* ---------- 1. score trend ---------- */
  const scoreRows = fulls.map((a, i) => ({
    label: `M${i + 1}`,
    tip: `${a.title} · ${fmtDate(a.date)}`,
    value: scoreMode === 'raw' ? a.score : Math.round((a.score / a.max) * 100),
    a,
  }));
  const avg = movingAverage(scoreRows.map((r) => r.value));
  scoreRows.forEach((r, i) => { r.avg = avg[i]; });
  const latest = fulls[fulls.length - 1];
  const prev = fulls[fulls.length - 2];

  // each section's history across every attempt that included it (sectionals too)
  const sectionTrend = Object.fromEntries(SECTION_KEYS.map((k) => [k, inRange.filter((a) => a.sections[k]).map((a) => ({ a, ...sectionStats(a.sections[k]) }))]));
  const sectionDelta = (k) => {
    const h = sectionTrend[k];
    return h.length < 2 ? null : h[h.length - 1].score - h[h.length - 2].score;
  };

  // section scores: full mocks share one x-axis; a single chosen section uses every attempt that has it
  const sectionScoreRows = section === 'all'
    ? fulls.map((a, i) => ({ label: `M${i + 1}`, tip: `${a.title} · ${fmtDate(a.date)}`, ...Object.fromEntries(SECTION_KEYS.map((k) => {
      const s = sectionStats(a.sections[k]);
      return [k, scoreMode === 'raw' ? s.score : Math.round((s.score / s.max) * 100)];
    })) }))
    : sectionTrend[section].map((s, i) => ({ label: `${i + 1}`, tip: `${s.a.title} · ${fmtDate(s.a.date)}`, [section]: scoreMode === 'raw' ? s.score : Math.round((s.score / s.max) * 100) }));

  /* ---------- 3. topic review ---------- */
  const withQuestions = inRange;
  const loaded = useAttemptQuestions(withQuestions);
  const topics = topicReview(withQuestions, loaded.byAttempt);
  const shownTopics = topics.filter((t) => section === 'all' || t.section === section);

  /* ---------- 4. revision ---------- */
  const buckets = revisionBuckets(revisions);
  const t0 = today();
  const nextWeek = buckets.upcoming.filter((r) => r.delta <= 7).length;
  const topicName = (id) => ALL_TOPICS.find((t) => t.id === id);
  const overdueShown = buckets.overdue.filter((r) => section === 'all' || topicName(r.id)?.section === section);
  const missedTopicIds = new Set(topics.map((t) => t.topicId).filter(Boolean));
  const logRows = Object.entries(revisionLog || {}).map(([d, v]) => ({ t: Number(d), ...v })).sort((a, b) => a.t - b.t).slice(-30)
    .map((r) => ({ t: r.t, label: fmtDate(r.t), tip: fmtDate(r.t), overdue: r.overdue, due: r.due }));
  const revisionDueIds = new Set([...buckets.overdue, ...buckets.due].map((r) => r.id));

  const actions = nextActions({
    topics: shownTopics,
    sectionTrend,
    revision: { overdue: buckets.overdue.length, oldestDays: buckets.overdue.length ? -buckets.overdue[0].delta : 0 },
    revisionDueIds,
  });

  const goToTopic = (key) => {
    setOpenTopic(key);
    setAllTopics(true);
    requestAnimationFrame(() => document.getElementById(`topic-${encodeURIComponent(key)}`)?.scrollIntoView({ behavior: 'smooth', block: 'center' }));
  };

  const sectionLimit = (a) => (findMock(a.mockId, customMocks) || papers?.find((m) => m.id === a.mockId) || sectionals?.find((m) => m.id === a.mockId))?.sectionMinutes ?? null;

  return (
    <div className="page">
      <div className="page-head">
        <div><h1>Performance Analytics</h1><p>What to practise next, and why, from your {inRange.length} most recent attempt{inRange.length === 1 ? '' : 's'}.</p></div>
      </div>

      {/* one filter row scopes everything below */}
      <div className="filter-row">
        <div className="tabs" role="group" aria-label="Range">
          {RANGES.map(([k, l]) => <button key={k} className={`tab ${range === k ? 'active' : ''}`} aria-pressed={range === k} onClick={() => setRange(k)}>{l}</button>)}
        </div>
        <div className="tabs" role="group" aria-label="Section">
          {[['all', 'All sections'], ...SECTION_KEYS.map((k) => [k, k])].map(([k, l]) => (
            <button key={k} className={`tab ${section === k ? 'active' : ''}`} aria-pressed={section === k} onClick={() => setSection(k)}>
              {k !== 'all' && <i className="dot" style={{ background: SECTIONS[k].color, marginRight: 6 }} />}{l}
            </button>
          ))}
        </div>
      </div>

      {/* what next */}
      <div className="card next-card" style={{ marginBottom: 18 }}>
        <div className="card-head"><div><h2>What to practise next</h2><p>Ranked from your recent misses, section accuracy and revision backlog</p></div></div>
        {actions.length ? (
          <ol className="next-list">
            {actions.map((x) => (
              <li key={x.title}>
                <div>
                  <b>{x.title}</b>
                  <div className="small muted">{x.why}</div>
                </div>
                {x.to.startsWith('#topic-')
                  ? <button className="btn sm" onClick={() => goToTopic(decodeURIComponent(x.to.slice(7)))}>{x.label}</button>
                  : <Link className="btn sm" to={x.to}>{x.label}</Link>}
              </li>
            ))}
          </ol>
        ) : <Empty>Take a mock to get recommendations. They come from the questions you miss, so they appear after your first attempt.</Empty>}
      </div>

      {/* 1. score trend */}
      <div className="grid g-4" style={{ marginBottom: 18 }}>
        <div className="card">
          <div className="stat-label">Latest full mock</div>
          <div className="stat-value">{latest ? <>{latest.score}<span className="stat-sub"> / {latest.max}</span></> : '—'}</div>
          {latest && <Delta value={prev ? latest.score - prev.score : null} />}
        </div>
        {SECTION_KEYS.map((k) => {
          const h = sectionTrend[k];
          const last = h[h.length - 1];
          return (
            <div className="card" key={k}>
              <div className="stat-label"><i className="dot" style={{ background: SECTIONS[k].color, marginRight: 6 }} />{k} latest</div>
              <div className="stat-value">{last ? <>{last.score}<span className="stat-sub"> / {last.max}</span></> : '—'}</div>
              {last && <Delta value={sectionDelta(k)} />}
            </div>
          );
        })}
      </div>

      <div className="grid g-2" style={{ marginBottom: 18 }}>
        <ChartCard
          title="Score by mock"
          sub={latest?.percentile != null
            ? `Full-length mocks. Est. percentile ${latest.percentile.toFixed(0)} (target ${targetPercentile}): a rough estimate, not CAT's normalised percentile.`
            : 'Full-length mocks'}
          actions={(
            <div className="tabs" role="group" aria-label="Score scale">
              <button className={`tab ${scoreMode === 'raw' ? 'active' : ''}`} onClick={() => setScoreMode('raw')}>Raw</button>
              <button className={`tab ${scoreMode === 'pct' ? 'active' : ''}`} onClick={() => setScoreMode('pct')} title="Papers have different lengths; % of max compares them fairly">% of max</button>
            </div>
          )}
          table={(
            <div className="table-wrap"><table className="table">
              <thead><tr><th>Mock</th><th>Date</th><th>Score</th><th>Change</th><th>3-mock avg</th><th>Est. %ile</th></tr></thead>
              <tbody>{scoreRows.map((r, i) => (
                <tr key={r.a.id}><td><Link to={`/mocks/result/${r.a.id}`}>{r.a.title}</Link></td><td>{fmtDate(r.a.date)}</td>
                  <td className="num">{r.a.score} / {r.a.max}</td><td className="num">{i ? (r.a.score - scoreRows[i - 1].a.score > 0 ? '+' : '') + (r.a.score - scoreRows[i - 1].a.score) : '—'}</td>
                  <td className="num">{r.avg}{scoreMode === 'pct' ? '%' : ''}</td><td className="num">{r.a.percentile != null ? `~${r.a.percentile.toFixed(0)}` : '—'}</td></tr>
              ))}</tbody>
            </table></div>
          )}
        >
          {scoreRows.length ? (
            <LineChart data={scoreRows} format={(v) => (v == null ? '—' : scoreMode === 'pct' ? `${v}%` : v)} yMax={scoreMode === 'pct' ? 100 : undefined}
              series={[{ key: 'value', label: scoreMode === 'pct' ? 'Score (% of max)' : 'Score', color: 'var(--text)' }, { key: 'avg', label: '3-mock average', color: 'var(--text-3)' }]} />
          ) : <Empty>No full-length mocks in this range yet.</Empty>}
        </ChartCard>

        <ChartCard
          title="Section scores"
          sub={section === 'all' ? 'Per full-length mock' : `Every ${section} attempt, sectionals included`}
          table={(
            <div className="table-wrap"><table className="table">
              <thead><tr><th>Attempt</th>{shownSections.map((k) => <th key={k}>{k}</th>)}</tr></thead>
              <tbody>{sectionScoreRows.map((r, i) => <tr key={i}><td>{r.tip}</td>{shownSections.map((k) => <td key={k} className="num">{r[k] ?? '—'}{scoreMode === 'pct' && r[k] != null ? '%' : ''}</td>)}</tr>)}</tbody>
            </table></div>
          )}
        >
          {sectionScoreRows.length ? (
            <LineChart data={sectionScoreRows} yMax={scoreMode === 'pct' ? 100 : undefined} format={(v) => (scoreMode === 'pct' ? `${v}%` : v)}
              series={shownSections.map((k) => ({ key: k, label: k, color: SECTIONS[k].color }))} />
          ) : <Empty>No attempts with this section in range.</Empty>}
        </ChartCard>
      </div>

      {/* 2. accuracy and attempt rate */}
      <div className="card" style={{ marginBottom: 18 }}>
        <div className="card-head"><div><h2>Accuracy and attempt rate</h2><p>Accuracy is correct ÷ attempted; attempt rate is attempted ÷ all scored questions. Read them together.</p></div></div>
        <div className={`grid ${shownSections.length === 1 ? '' : 'g-3'}`} style={{ marginBottom: 16 }}>
          {shownSections.map((k) => {
            const rows = sectionTrend[k].map((s, i) => ({ label: `${i + 1}`, tip: `${s.a.title} · ${fmtDate(s.a.date)}`, acc: s.accuracy == null ? 0 : Math.round(s.accuracy * 100), rate: s.attemptRate == null ? 0 : Math.round(s.attemptRate * 100) }));
            return (
              <div key={k} className="small-multiple">
                <div style={{ fontWeight: 600, marginBottom: 6 }}><i className="dot" style={{ background: SECTIONS[k].color, marginRight: 6 }} />{SECTIONS[k].name}</div>
                {rows.length ? <LineChart data={rows} height={170} yMax={100} format={(v) => `${v}%`}
                  series={[{ key: 'acc', label: 'Accuracy', color: SECTIONS[k].color }, { key: 'rate', label: 'Attempt rate', color: 'var(--text-3)' }]} />
                  : <Empty>No {k} attempts in range.</Empty>}
              </div>
            );
          })}
        </div>
        <div className="table-wrap">
          <table className="table">
            <thead><tr><th>Section</th><th>Latest: correct</th><th>Wrong</th><th>Skipped</th><th>Accuracy</th><th>Attempt rate</th><th>Range accuracy</th><th>Range attempt rate</th><th>Reading</th></tr></thead>
            <tbody>
              {shownSections.map((k) => {
                const h = sectionTrend[k];
                const last = h[h.length - 1];
                const att = h.filter((s) => s.accuracy != null);
                const accAvg = att.length ? att.reduce((t, s) => t + s.accuracy, 0) / att.length : null;
                const rateAvg = h.length ? h.reduce((t, s) => t + (s.attemptRate ?? 0), 0) / h.length : null;
                const reading = accAvg == null ? '—' : accAvg < 0.6 ? 'Attempt fewer, surer questions' : accAvg >= 0.8 && rateAvg < 0.5 ? 'Attempt more questions' : 'Keep the balance';
                return (
                  <tr key={k}>
                    <td style={{ fontWeight: 600 }}><i className="dot" style={{ background: SECTIONS[k].color, marginRight: 8 }} />{k}</td>
                    <td className="num">{last?.correct ?? '—'}</td><td className="num">{last?.wrong ?? '—'}</td><td className="num">{last?.skipped ?? '—'}</td>
                    <td className="num">{pct(last?.accuracy)}</td><td className="num">{pct(last?.attemptRate)}</td>
                    <td className="num">{pct(accAvg)}</td><td className="num">{pct(rateAvg)}</td><td>{reading}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {/* 3. topic review */}
      <div className="card" style={{ marginBottom: 18 }}>
        <div className="card-head">
          <div>
            <h2>Topics to review</h2>
            <p>Wrong and skipped questions by topic, recent mocks weighted most. Open a topic to go to each question’s solution.</p>
          </div>
        </div>
        {loaded.loading && !shownTopics.length ? <Empty>Loading your answers…</Empty>
          : !withQuestions.length ? <Empty>Topic review needs question-level answers. It fills in from the mocks you take in this app.</Empty>
          : !shownTopics.length ? <Empty>No wrong or skipped questions in this range{section === 'all' ? '' : ` for ${section}`}. </Empty>
          : (
            <div className="table-wrap">
              <table className="table topic-table">
                <thead><tr><th>Topic</th><th>Section</th><th>Wrong</th><th>Skipped</th><th>Accuracy</th><th style={{ width: '22%' }}>Seen</th><th>Revision</th><th /></tr></thead>
                <tbody>
                  {shownTopics.slice(0, allTopics ? shownTopics.length : 12).map((t) => {
                    const key = `${t.section}-${t.topic}`;
                    const open = openTopic === key;
                    const rev = t.topicId && revisions[t.topicId];
                    const revState = !rev ? 'Not scheduled' : rev.paused ? 'Paused' : rev.delta < 0 ? `Overdue ${-rev.delta}d` : rev.delta === 0 ? 'Due today' : `In ${rev.delta}d`;
                    return [
                      <tr key={key} id={`topic-${encodeURIComponent(key)}`} className={open ? 'open' : ''}>
                        <td style={{ fontWeight: 600 }}>{t.topic}</td>
                        <td><i className="dot" style={{ background: SECTIONS[t.section].color, marginRight: 6 }} />{t.section}</td>
                        <td className="num">{t.wrong}</td><td className="num">{t.skipped}</td><td className="num">{pct(t.accuracy)}</td>
                        <td><SegBar parts={[{ label: 'Correct', value: t.correct, color: 'var(--good)' }, { label: 'Wrong', value: t.wrong, color: 'var(--bad)' }, { label: 'Skipped', value: t.skipped, color: 'transparent' }]} /></td>
                        <td className="small">{revState}</td>
                        <td><button className="btn sm ghost" aria-expanded={open} onClick={() => setOpenTopic(open ? null : key)}>{open ? 'Hide' : `${t.items.length} question${t.items.length === 1 ? '' : 's'}`}</button></td>
                      </tr>,
                      open && (
                        <tr key={`${key}-items`} className="topic-items"><td colSpan={8}>
                          <ul>
                            {t.items.map(({ attempt, question, result }) => (
                              <li key={`${attempt.id}-${question.id}`}>
                                <span className={`chip ${result === 'wrong' ? 'bad' : ''}`}>{result === 'wrong' ? 'Wrong' : 'Skipped'}</span>
                                <Link to={`/mocks/result/${attempt.id}#q-${encodeURIComponent(question.id)}`}>{plain(question.q).slice(0, 140)}{plain(question.q).length > 140 ? '…' : ''}</Link>
                                <span className="small faint">{attempt.title} · {fmtDate(attempt.date)}</span>
                              </li>
                            ))}
                          </ul>
                        </td></tr>
                      ),
                    ];
                  })}
                </tbody>
              </table>
            </div>
          )}
        {shownTopics.length > 12 && (
          <button className="btn sm ghost" style={{ marginTop: 10 }} onClick={() => setAllTopics(!allTopics)}>
            {allTopics ? 'Show the top 12' : `Show all ${shownTopics.length} topics`}
          </button>
        )}
        {loaded.failed > 0 && <p className="small faint" style={{ marginBottom: 0 }}>{loaded.failed} attempt{loaded.failed === 1 ? '' : 's'} couldn’t be loaded and {loaded.failed === 1 ? 'is' : 'are'} left out.</p>}
      </div>

      <div style={{ marginBottom: 18 }}>
        {/* 4. revision */}
        <ChartCard
          title="Revision backlog"
          sub="Overdue topics should shrink over time"
          actions={<Link className="btn sm" to="/revision">Open revision</Link>}
          table={logRows.length ? (
            <div className="table-wrap"><table className="table">
              <thead><tr><th>Day</th><th>Overdue</th><th>Due that day</th></tr></thead>
              <tbody>{[...logRows].reverse().map((r) => <tr key={r.label}><td>{r.label}</td><td className="num">{r.overdue}</td><td className="num">{r.due}</td></tr>)}</tbody>
            </table></div>
          ) : null}
        >
          <div className="grid g-3" style={{ marginBottom: 14 }}>
            <div><div className="stat-label">Overdue</div><div className="stat-value">{buckets.overdue.length}</div></div>
            <div><div className="stat-label">Due today</div><div className="stat-value">{buckets.due.length}</div></div>
            <div><div className="stat-label">Next 7 days</div><div className="stat-value">{nextWeek}</div></div>
          </div>
          {logRows.length >= 2
            ? <LineChart data={logRows} height={160} series={[{ key: 'overdue', label: 'Overdue', color: 'var(--bad)' }, { key: 'due', label: 'Due that day', color: 'var(--text-3)' }]} />
            : <p className="small faint">Daily tracking started {logRows.length ? fmtDate(logRows[0].t) : 'today'}; the trend appears after a second day.</p>}
          {overdueShown.length > 0 && (
            <div style={{ marginTop: 12 }}>
              <div className="small faint" style={{ marginBottom: 6 }}>Most overdue{section === 'all' ? '' : ` in ${section}`}</div>
              <ul className="plain-list">
                {overdueShown.slice(0, 5).map((r) => {
                  const tp = topicName(r.id);
                  return (
                    <li key={r.id}>
                      <i className="dot" style={{ background: SECTIONS[tp?.section]?.color, marginRight: 6 }} />{tp?.name ?? r.id}
                      <span className="small faint"> · {-r.delta}d overdue</span>
                      {missedTopicIds.has(r.id) && <span className="chip bad" style={{ marginLeft: 6, padding: '0 6px' }}>also missed in mocks</span>}
                    </li>
                  );
                })}
              </ul>
            </div>
          )}
        </ChartCard>

      </div>

      {/* 5. section time */}
      <div style={{ marginBottom: 18 }}>
        <div className="card">
          <div className="card-head"><div><h2>Section time use</h2><p>Time is recorded per section, not per question.</p></div></div>
          {inRange.some((a) => Object.values(a.sections).some((s) => s.time != null)) ? (
            <div className="table-wrap">
              <table className="table">
                <thead><tr><th>Attempt</th>{shownSections.map((k) => <th key={k}>{k}: time · score · acc.</th>)}</tr></thead>
                <tbody>
                  {[...inRange].reverse().filter((a) => shownSections.some((k) => a.sections[k])).slice(0, 10).map((a) => {
                    const limit = sectionLimit(a);
                    return (
                      <tr key={a.id}>
                        <td><Link to={`/mocks/result/${a.id}`}>{a.title}</Link><div className="small faint">{fmtDate(a.date)}</div></td>
                        {shownSections.map((k) => {
                          if (!a.sections[k]) return <td key={k} className="faint">—</td>;
                          const s = sectionStats(a.sections[k]);
                          return (
                            <td key={k}>
                              <div className="num">{mins(s.seconds)}{limit && s.seconds != null ? <span className="faint"> / {limit}m</span> : ''}</div>
                              {limit && s.seconds != null && <SegBar parts={[{ label: 'Used', value: Math.min(s.seconds, limit * 60), color: SECTIONS[k].color }, { label: 'Left', value: Math.max(0, limit * 60 - s.seconds), color: 'transparent' }]} />}
                              <div className="small faint num">{s.score} / {s.max} · {pct(s.accuracy)}</div>
                            </td>
                          );
                        })}
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          ) : <Empty>Section times are recorded for mocks you take in this app.</Empty>}
        </div>
      </div>

      <div className="card">
        <div className="card-head"><div><h2>Goals & settings</h2></div></div>
        <div className="grid g-3" style={{ alignItems: 'end' }}>
          <label className="field">Exam date
            <input className="input" type="date" value={new Date(examDate - new Date(examDate).getTimezoneOffset() * 60000).toISOString().slice(0, 10)}
              onChange={(e) => e.target.value && updateSettings({ examDate: new Date(`${e.target.value}T00:00:00`).getTime(), targetYear: Number(e.target.value.slice(0, 4)) })} />
          </label>
          <label className="field">Target percentile
            <select className="input" value={targetPercentile} onChange={(e) => updateSettings({ targetPercentile: Number(e.target.value) })}>
              {[90, 95, 97, 98, 99, 99.5].map((p) => <option key={p} value={p}>{p}</option>)}
            </select>
          </label>
        </div>
      </div>
    </div>
  );
}

