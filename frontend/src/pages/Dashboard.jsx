import { Link } from 'react-router-dom';
import { useAuth } from '../lib/auth.jsx';
import { useStore, coverageStats, revisionBuckets, streak, today, daysBetween } from '../lib/store.jsx';
import { ALL_TOPICS, SECTIONS } from '../data/syllabus.js';
import { usePapers } from '../lib/catalog.js';
import { LineChart, Ring, Heatmap, SegBar } from '../components/Charts.jsx';
import EffortGauge from '../components/EffortGauge.jsx';
import { effortFor } from '../lib/effort.js';
import { IconFlame, IconTest, IconRevise, IconCoverage, IconArrow, IconTarget, IconAlert } from '../components/Icons.jsx';

const fmtDate = (t) => new Date(t).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' });

export default function Dashboard() {
  const { attempts, coverage, revisions, activity, examDate, targetPercentile } = useStore();
  const { user } = useAuth();
  const cov = coverageStats(coverage);
  const { overdue, due } = revisionBuckets(revisions);
  const s = streak(activity);
  const fulls = attempts.filter((a) => a.sections && Object.keys(a.sections).length === 3);
  const last = fulls[fulls.length - 1];
  const prev = fulls[fulls.length - 2];
  const bestPct = Math.max(0, ...fulls.map((a) => a.percentile || 0));
  const days = daysBetween(today(), examDate);
  const effortIn = { activity, attempts, overdue: overdue.length, scheduled: Object.keys(revisions).length };
  const effort = effortFor(effortIn, today());
  const lastWeekEffort = effortFor(effortIn, today() - 7 * 86400000);
  const weekMin = Array.from({ length: 7 }, (_, i) => activity[today() - i * 86400000] || 0).reduce((a, b) => a + b, 0);

  const trend = fulls.slice(-8).map((a, i) => ({
    label: `M${i + 1}`,
    tip: `${a.title} · ${fmtDate(a.date)}`,
    ...Object.fromEntries(['VARC', 'DILR', 'QA'].map((k) => {
      const sec = a.sections[k];
      const max = (sec.correct + sec.wrong + sec.unattempted) * 3 || 1;
      return [k, Math.max(0, Math.round((sec.score / max) * 100))];
    })),
  }));

  const attemptedIds = new Set(attempts.map((a) => a.mockId));
  const { papers } = usePapers();
  const nextMock = papers?.find((m) => !attemptedIds.has(m.id));

  // Weakest topics: in-progress or not-started topics with low practice in sections where scores lag
  const weak = ALL_TOPICS
    .filter((t) => (coverage[t.id]?.status ?? 0) < 2)
    .sort((a, b) => (coverage[a.id]?.practiced ?? 0) - (coverage[b.id]?.practiced ?? 0))
    .slice(0, 5);

  const hour = new Date().getHours();
  const greet = hour < 12 ? 'Good morning' : hour < 17 ? 'Good afternoon' : 'Good evening';

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1>{greet}, {user?.name?.split(' ')[0]}</h1>
          <p>{days > 0 ? `${days} days to CAT. ` : ''}{overdue.length + due.length > 0 ? `${overdue.length + due.length} topics are due for revision today.` : 'You’re all caught up on revision.'}</p>
        </div>
        <div style={{ display: 'flex', gap: 10 }}>
          <Link to="/revision" className="btn"><IconRevise />Revise now</Link>
          {nextMock && <Link to={`/mocks/start/${nextMock.id}`} className="btn primary"><IconTest />Start {nextMock.title}</Link>}
        </div>
      </div>

      {/* KPI tiles */}
      <div className="grid g-4" style={{ marginBottom: 18 }}>
        <div className="card">
          <div className="stat-label"><IconTarget width="14" />Latest mock score</div>
          <div className="stat-value">{last?.score ?? '—'}<span className="faint" style={{ fontSize: 15, fontWeight: 500 }}> / {last?.max ?? 204}</span></div>
          <div className="stat-sub">
            {last && prev ? <span style={{ color: last.percentile >= prev.percentile ? 'var(--good)' : 'var(--bad)', fontWeight: 600 }}>{last.percentile >= prev.percentile ? '▲' : '▼'} {Math.abs(last.percentile - prev.percentile).toFixed(1)} percentile vs previous</span> : 'Take a mock to start'}
          </div>
        </div>
        <div className="card">
          <div className="stat-label"><IconTest width="14" />Est. percentile (best)</div>
          <div className="stat-value">{bestPct ? bestPct.toFixed(1) : '—'}</div>
          <div className="stat-sub">Target {targetPercentile}+ · {fulls.length} full mocks taken</div>
        </div>
        <div className="card">
          <div className="stat-label"><IconCoverage width="14" />Syllabus coverage</div>
          <div className="stat-value">{cov.pct}%</div>
          <div className="stat-sub">{cov.done} done · {cov.prog} in progress · {cov.total - cov.done - cov.prog} left</div>
        </div>
        <div className="card">
          <div className="stat-label"><IconFlame width="14" />Study streak</div>
          <div className="stat-value">{s} <span className="faint" style={{ fontSize: 15, fontWeight: 500 }}>days</span></div>
          <div className="stat-sub">{Math.round(weekMin / 60)} h studied in the last 7 days</div>
        </div>
      </div>

      <div className="card" style={{ marginBottom: 18 }}>
        <div className="card-head">
          <div><h2>Effort this week</h2><p>Study time, consistency, mocks and revision over the last 7 days</p></div>
        </div>
        <EffortGauge score={effort.score} parts={effort.parts} delta={effort.score - lastWeekEffort.score} />
      </div>

      <div className="grid g-main" style={{ marginBottom: 18 }}>
        <div className="card">
          <div className="card-head">
            <div><h2>Section scores across mocks</h2><p>Score as % of section maximum</p></div>
            <Link to="/analytics" className="btn sm ghost">Analytics <IconArrow /></Link>
          </div>
          {trend.length ? (
            <LineChart data={trend} yMax={100} format={(v) => `${v}%`} series={['VARC', 'DILR', 'QA'].map((k) => ({ key: k, label: k, color: SECTIONS[k].color }))} />
          ) : <p className="muted">No mocks yet.</p>}
        </div>

        <div className="card">
          <div className="card-head"><div><h2>Today’s plan</h2><p>{new Date().toLocaleDateString('en-IN', { weekday: 'long', day: 'numeric', month: 'long' })}</p></div></div>
          <div className="list">
            {[...overdue, ...due].slice(0, 4).map((r) => {
              const t = ALL_TOPICS.find((x) => x.id === r.id);
              return t && (
                <div className="row" key={r.id}>
                  <i className="dot" style={{ background: SECTIONS[t.section].color }} />
                  <div className="grow">
                    <div className="ellipsis" style={{ fontWeight: 500 }}>Revise · {t.name}</div>
                    <div className="small faint">{t.section} · {t.chapter}</div>
                  </div>
                  {r.delta < 0 ? <span className="chip bad">{-r.delta}d overdue</span> : <span className="chip warn">Due today</span>}
                </div>
              );
            })}
            {nextMock && (
              <div className="row">
                <i className="dot" style={{ background: 'var(--accent)' }} />
                <div className="grow"><div style={{ fontWeight: 500 }}>Attempt {nextMock.title}</div><div className="small faint">{nextMock.minutes} min · {nextMock.questions} questions</div></div>
                <Link className="btn sm" to={`/mocks/start/${nextMock.id}`}>Start</Link>
              </div>
            )}
            {weak[0] && (
              <div className="row">
                <i className="dot" style={{ background: SECTIONS[weak[0].section].color }} />
                <div className="grow"><div className="ellipsis" style={{ fontWeight: 500 }}>Learn · {weak[0].name}</div><div className="small faint">{weak[0].section} · new topic</div></div>
                <span className="chip">45 min</span>
              </div>
            )}
          </div>
          <Link to="/revision" className="btn sm" style={{ width: '100%', marginTop: 12 }}>Open revision tracker</Link>
        </div>
      </div>

      <div className="grid g-3" style={{ marginBottom: 18 }}>
        <div className="card">
          <div className="card-head"><div><h2>Coverage by section</h2><p>Completed + half credit for in-progress</p></div></div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            {Object.values(SECTIONS).map((sec) => {
              const st = coverageStats(coverage, ALL_TOPICS.filter((t) => t.section === sec.id));
              return (
                <div key={sec.id} style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
                  <Ring value={st.pct} size={56} stroke={6} color={sec.color} />
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontWeight: 600 }}>{sec.short}</div>
                    <div className="small faint">{st.done}/{st.total} topics complete</div>
                    <div style={{ marginTop: 6 }}>
                      <SegBar parts={[{ label: 'Done', value: st.done, color: sec.color }, { label: 'In progress', value: st.prog, color: 'var(--text-3)' }, { label: 'Not started', value: st.total - st.done - st.prog, color: 'transparent' }]} height={6} />
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        <div className="card">
          <div className="card-head"><div><h2>Focus areas</h2><p>Open topics with the fewest solved questions</p></div><Link to="/coverage" className="btn sm ghost">All <IconArrow /></Link></div>
          <div className="list">
            {weak.map((t) => (
              <div className="row" key={t.id}>
                <IconAlert width="16" style={{ color: 'var(--warn)', flex: 'none' }} />
                <div className="grow"><div className="ellipsis" style={{ fontWeight: 500 }}>{t.name}</div><div className="small faint">{t.section} · {coverage[t.id]?.practiced ?? 0} Qs solved</div></div>
                <span className={`chip ${coverage[t.id]?.status === 1 ? 'warn' : ''}`}>{coverage[t.id]?.status === 1 ? 'In progress' : 'Not started'}</span>
              </div>
            ))}
          </div>
        </div>

        <div className="card">
          <div className="card-head"><div><h2>Recent mocks</h2><p>Last 4 attempts</p></div><Link to="/mocks" className="btn sm ghost">All <IconArrow /></Link></div>
          <div className="list">
            {[...attempts].reverse().slice(0, 4).map((a) => (
              <Link to={`/mocks/result/${a.id}`} className="row" key={a.id}>
                <div className="grow"><div className="ellipsis" style={{ fontWeight: 500 }}>{a.title}</div><div className="small faint">{fmtDate(a.date)}</div></div>
                <div style={{ textAlign: 'right' }}>
                  <div className="num" style={{ fontWeight: 700 }}>{a.score}<span className="faint small">/{a.max}</span></div>
                  {a.percentile && <div className="small faint num">{a.percentile.toFixed(1)} %ile</div>}
                </div>
              </Link>
            ))}
          </div>
        </div>
      </div>

      <div className="card">
        <div className="card-head"><div><h2>Study activity</h2><p>Minutes logged per day</p></div><span className="chip"><IconFlame width="14" />{s}-day streak</span></div>
        <Heatmap activity={activity} />
      </div>
    </div>
  );
}
