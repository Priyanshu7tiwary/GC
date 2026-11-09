import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useStore } from '../lib/store.jsx';
import { PYQ_YEARS } from '../data/mocks.js';
import { useCatalog, usePapers } from '../lib/catalog.js';
import { SECTIONS } from '../data/syllabus.js';
import { generateMock, WORKFLOW, SECTION_QS, listServerMocks, deleteServerMock } from '../lib/mockGenerator.js';
import { IconClock, IconTest, IconArrow, IconCheck } from '../components/Icons.jsx';
import MockScopePicker, { Check, scopeSections, scopeValid } from '../components/MockScopePicker.jsx';

const TABS = [
  { id: 'pyq', label: 'Previous year mocks' },
  { id: 'sectional', label: 'Sectional mocks' },
  { id: 'custom', label: 'Custom mocks · AI' },
];

/* ---------------- shared mock card ---------------- */
function MockCard({ m, tries, onDelete }) {
  const best = tries && Math.max(...tries.map((t) => t.score));
  const lastTry = tries?.[tries.length - 1];
  return (
    <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8 }}>
        <div style={{ minWidth: 0 }}>
          <h2>{m.title}</h2>
          {m.subtitle && <div className="small muted" style={{ marginTop: 2 }}>{m.subtitle}</div>}
          <div className="small faint" style={{ marginTop: 4, display: 'flex', gap: 12 }}>
            <span style={{ display: 'inline-flex', gap: 4, alignItems: 'center' }}><IconClock width="13" />{m.minutes} min</span>
            <span>{m.questions} Qs</span>
          </div>
        </div>
        {tries ? <span className="chip good"><IconCheck width="12" />Attempted</span> : <span className="chip">{m.difficulty}</span>}
      </div>
      <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
        {m.sections.map((s) => (
          <span className="chip" key={s}><i className="dot" style={{ background: SECTIONS[s].color }} />{s}{m.sectionQs ? ` · ${m.sectionQs[s]}` : ''}</span>
        ))}
      </div>
      {tries ? (
        <div style={{ display: 'flex', gap: 20 }} className="small">
          <div><div className="faint">Best</div><b className="num" style={{ fontSize: 16 }}>{best}</b></div>
          <div><div className="faint">Attempts</div><b className="num" style={{ fontSize: 16 }}>{tries.length}</b></div>
          {lastTry.percentile && <div><div className="faint">%ile</div><b className="num" style={{ fontSize: 16 }}>{lastTry.percentile.toFixed(1)}</b></div>}
        </div>
      ) : <div className="small muted">Not attempted yet.</div>}
      <div style={{ display: 'flex', gap: 8, marginTop: 'auto' }}>
        <Link to={`/mocks/start/${m.id}`} className="btn primary" style={{ flex: 1 }}><IconTest />{tries ? 'Reattempt' : 'Start test'}</Link>
        {lastTry && <Link to={`/mocks/result/${lastTry.id}`} className="btn">Analysis</Link>}
        {onDelete && <button className="btn ghost" onClick={onDelete} title="Delete this mock">Delete</button>}
      </div>
    </div>
  );
}

/* ---------------- previous years ---------------- */
function PreviousYears({ byMock }) {
  const [year, setYear] = useState(PYQ_YEARS[0]);
  const { papers: all, error } = usePapers();
  if (!all) {
    return error
      ? <div className="card"><p className="muted" style={{ margin: 0 }}>Couldn’t load the papers: {error.message}</p></div>
      : <div className="loading-card" role="status"><span className="ring" /><span>Loading papers…</span></div>;
  }
  const papers = all.filter((m) => m.year === year);
  return (
    <>
      <div className="year-grid" style={{ marginBottom: 18 }}>
        {PYQ_YEARS.map((y) => {
          const ps = all.filter((m) => m.year === y);
          const done = ps.filter((p) => byMock[p.id]).length;
          return (
            <button key={y} className={`year-btn ${year === y ? 'active' : ''}`} onClick={() => setYear(y)}>
              <b>CAT {y}</b>
              <span>{ps.length ? `${done}/${ps.length} attempted` : 'No official paper'}</span>
            </button>
          );
        })}
      </div>
      {papers.length ? (
        <>
          <div className="card-head" style={{ marginBottom: 12 }}>
            <div><h2>CAT {year} papers</h2><p>{papers.length} official slots · {papers[0].questions} questions · {papers[0].sectionMinutes} min per section</p></div>
          </div>
          <div className="grid g-3">
            {papers.map((m) => <MockCard key={m.id} m={m} tries={byMock[m.id]} />)}
          </div>
        </>
      ) : (
        <div className="card">
          <h2>CAT {year} official paper unavailable</h2>
          <p className="muted" style={{ marginBottom: 0 }}>IIMs did not publicly release official CAT papers for 2011–2016. Official papers from 2017 onward are available as full tests; memory-based copies are not presented as official tests.</p>
        </div>
      )}
    </>
  );
}

/* ---------------- sectional ---------------- */
function Sectional({ byMock }) {
  const [params] = useSearchParams();
  const [sec, setSec] = useState(SECTIONS[params.get('sec')] ? params.get('sec') : 'VARC');
  const { list: all, error } = useCatalog('SECTIONAL');
  if (!all) {
    return error
      ? <div className="card"><p className="muted" style={{ margin: 0 }}>Couldn’t load the sectional tests: {error.message}</p></div>
      : <div className="loading-card" role="status"><span className="ring" /><span>Loading sectional tests…</span></div>;
  }
  const list = all.filter((m) => m.sections[0] === sec);
  return (
    <>
      <div className="subject-row" style={{ marginBottom: 18 }}>
        {Object.values(SECTIONS).map((s) => {
          const n = all.filter((m) => m.sections[0] === s.id);
          return (
            <button key={s.id} className={`subject-btn ${sec === s.id ? 'active' : ''}`} style={{ '--c': s.color }} onClick={() => setSec(s.id)}>
              <i className="dot" style={{ background: s.color, width: 10, height: 10 }} />
              <span style={{ textAlign: 'left' }}>
                <b>{s.short}</b>
                <span className="small faint" style={{ display: 'block' }}>{n.filter((m) => byMock[m.id]).length}/{n.length} attempted</span>
              </span>
            </button>
          );
        })}
      </div>
      <div className="grid g-3">
        {list.map((m) => <MockCard key={m.id} m={m} tries={byMock[m.id]} />)}
      </div>
    </>
  );
}

/* ---------------- custom / AI generator ---------------- */
const DIFFICULTIES = ['Easy', 'Moderate', 'Hard', 'CAT-level', 'Adaptive'];

function CustomMocks({ byMock }) {
  const { customMocks, addCustomMock, removeCustomMock, syncServerMocks, coverage } = useStore();
  const [scope, setScope] = useState({ type: null, subjects: [] });
  const [difficulty, setDifficulty] = useState('CAT-level');
  const [focusWeak, setFocusWeak] = useState(true);
  const [timed, setTimed] = useState(true);
  const [run, setRun] = useState(null); // { steps: {id: 'running'|'done'|'failed'}, info: {}, result }
  const [listError, setListError] = useState('');

  useEffect(() => {
    listServerMocks().then(syncServerMocks, () => setListError('Couldn’t refresh your generated mocks from the server.'));
  }, [syncServerMocks]);

  const { type, subjects } = scope;
  const valid = scopeValid(scope);
  const busy = run && !run.result && !run.error;
  const secs = scopeSections(scope);
  const qCount = secs.reduce((a, s) => a + SECTION_QS[s], 0);

  const start = async () => {
    let state = { steps: {}, info: {} };
    setRun({ ...state });
    try {
      const mock = await generateMock({ type, sections: subjects, difficulty, focusWeak, timed }, { coverage }, (serverSteps) => {
        const status = { PENDING: undefined, RUNNING: 'running', DONE: 'done', FAILED: 'failed' };
        state = {
          steps: Object.fromEntries(serverSteps.map((st) => [st.id, status[st.status]])),
          info: Object.fromEntries(serverSteps.filter((st) => st.info).map((st) => [st.id, st.info])),
        };
        setRun({ ...state });
      });
      addCustomMock(mock);
      setRun({ ...state, result: mock });
    } catch (e) {
      setRun({ ...state, error: e.message || 'Generation failed' });
    }
  };

  const remove = async (m) => {
    if (m.source === 'server') {
      try { await deleteServerMock(m.id); } catch (e) { if (e.status !== 404) { setListError(e.message); return; } }
    }
    removeCustomMock(m.id);
  };

  return (
    <>
      <div className="grid g-main" style={{ marginBottom: 24 }}>
        <div className="card">
          <div className="card-head"><div><h2>Generate a custom mock</h2><p>Pick what you want to practise — the AI builds a test around it.</p></div></div>

          <div className="form-block">
            <div className="form-label">1 · Mock type</div>
            <MockScopePicker value={scope} onChange={setScope} counts={SECTION_QS} />
          </div>

          <div className="form-block">
            <div className="form-label">2 · Difficulty</div>
            <div className="tabs">
              {DIFFICULTIES.map((d) => (
                <button key={d} className={`tab ${difficulty === d ? 'active' : ''}`} onClick={() => setDifficulty(d)}>{d}</button>
              ))}
            </div>
            <div className="small faint" style={{ marginTop: 6 }}>
              {difficulty === 'Adaptive' ? 'Questions get harder or easier based on your past accuracy per topic.' : difficulty === 'CAT-level' ? 'Mirrors the difficulty spread of recent CAT papers.' : `Every question pitched at ${difficulty.toLowerCase()} level.`}
            </div>
          </div>

          <div className="form-block">
            <div className="form-label">3 · Options</div>
            <div className="check-row">
              <Check checked={focusWeak} onChange={setFocusWeak} sub="Weighted toward low-coverage topics">Prioritise weak topics</Check>
              <Check checked={timed} onChange={setTimed} sub={timed ? '40 min per section' : '60 min per section'}>Strict CAT timing</Check>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12, flexWrap: 'wrap', borderTop: '1px solid var(--border)', paddingTop: 16 }}>
            <span className="small muted">
              {valid ? `${type === 'full' ? 'Full mock' : `${secs.join(' + ')} sectional`} · ${qCount} questions · ${difficulty}` : 'Select a mock type to continue'}
            </span>
            <button className="btn primary" disabled={!valid || busy} onClick={start}>{busy ? 'Generating…' : 'Generate mock ✦'}</button>
          </div>
        </div>

        <div className="card">
          <div className="card-head"><div><h2>Generation workflow</h2><p>{run ? (run.result ? 'Done' : run.error ? 'Failed' : 'Running…') : 'Starts when you click Generate'}</p></div></div>
          <ol className="workflow">
            {WORKFLOW.map((w) => {
              const st = run?.steps[w.id];
              return (
                <li key={w.id} className={st || 'pending'}>
                  <span className="wf-dot">{st === 'done' ? <IconCheck width="12" /> : st === 'running' ? <span className="spinner" /> : null}</span>
                  <div>
                    <div style={{ fontWeight: 500 }}>{w.label}</div>
                    <div className="small faint">{run?.info[w.id] || w.detail}</div>
                  </div>
                </li>
              );
            })}
          </ol>
          {run?.error && <div className="chip bad" style={{ marginTop: 12 }}>{run.error}</div>}
          {run?.result && (
            <div style={{ marginTop: 14, padding: 14, borderRadius: 12, background: 'var(--accent-soft)' }}>
              <div style={{ fontWeight: 600 }}>{run.result.title} is ready</div>
              <div className="small muted" style={{ margin: '2px 0 10px' }}>{run.result.questions} Qs · {run.result.minutes} min{run.result.focusTopics?.length ? ` · focus: ${run.result.focusTopics.slice(0, 3).join(', ')}` : ''}</div>
              <Link to={`/mocks/start/${run.result.id}`} className="btn primary sm">Start now <IconArrow /></Link>
            </div>
          )}
        </div>
      </div>

      <div className="card-head" style={{ marginBottom: 12 }}><div><h2>Your generated mocks</h2><p>{customMocks.length} saved{listError ? ` · ${listError}` : ''}</p></div></div>
      {customMocks.length ? (
        <div className="grid g-3">
          {customMocks.map((m) => <MockCard key={m.id} m={m} tries={byMock[m.id]} onDelete={() => remove(m)} />)}
        </div>
      ) : <div className="card muted">No custom mocks yet — generate your first one above.</div>}
    </>
  );
}

/* ---------------- page ---------------- */
export default function MockTests() {
  const { attempts } = useStore();
  const [params, setParams] = useSearchParams();
  const tab = TABS.some((t) => t.id === params.get('tab')) ? params.get('tab') : 'pyq';

  const byMock = {};
  attempts.forEach((a) => { (byMock[a.mockId] ||= []).push(a); });

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1>Mock Tests</h1>
          <p>Timed, section-locked tests. Previous-year mocks are the official CAT 2017–2025 papers, scored with CAT marking.</p>
        </div>
      </div>

      <div className="tabs" style={{ marginBottom: 20 }}>
        {TABS.map((t) => <button key={t.id} className={`tab ${tab === t.id ? 'active' : ''}`} onClick={() => setParams({ tab: t.id })}>{t.label}</button>)}
      </div>

      {tab === 'pyq' && <PreviousYears byMock={byMock} />}
      {tab === 'sectional' && <Sectional byMock={byMock} />}
      {tab === 'custom' && <CustomMocks byMock={byMock} />}

      <div className="card" style={{ marginTop: 28 }}>
        <div className="card-head"><div><h2>Attempt history</h2><p>Every mock you’ve taken</p></div></div>
        <div className="table-wrap">
          <table className="table">
            <thead><tr><th>Test</th><th>Date</th><th>VARC</th><th>DILR</th><th>QA</th><th>Total</th><th>%ile</th><th /></tr></thead>
            <tbody>
              {[...attempts].reverse().map((a) => (
                <tr key={a.id}>
                  <td style={{ fontWeight: 500 }}>{a.title}</td>
                  <td className="muted">{new Date(a.date).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' })}</td>
                  {['VARC', 'DILR', 'QA'].map((s) => <td key={s} className="num">{a.sections[s]?.score ?? '—'}</td>)}
                  <td className="num" style={{ fontWeight: 700 }}>{a.score}<span className="faint">/{a.max}</span></td>
                  <td className="num">{a.percentile ? a.percentile.toFixed(1) : '—'}</td>
                  <td><Link to={`/mocks/result/${a.id}`} className="btn sm ghost">View <IconArrow /></Link></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
