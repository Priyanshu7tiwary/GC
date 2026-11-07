import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useStore } from '../lib/store.jsx';
import { findMock, SECTION_MINUTES } from '../data/mocks.js';
import { SECTIONS } from '../data/syllabus.js';
import MockScopePicker, { Check, scopeSections, scopeValid } from '../components/MockScopePicker.jsx';
import { IconClock, IconTest } from '../components/Icons.jsx';

const ORDER = ['VARC', 'DILR', 'QA'];
const STANDARD_QS = { VARC: 24, DILR: 22, QA: 22 };

export default function MockSetup() {
  const { mockId } = useParams();
  const nav = useNavigate();
  const { customMocks, attempts } = useStore();
  const mock = findMock(mockId, customMocks);

  const available = mock ? ORDER.filter((s) => mock.sections.includes(s)) : [];
  const [scope, setScope] = useState(() =>
    available.length === 3 ? { type: 'full', subjects: [] } : { type: 'sectional', subjects: available });
  const [strict, setStrict] = useState(true);

  if (!mock) {
    return (
      <div className="page">
        <h1>Test not found</h1>
        <Link to="/mocks" className="btn" style={{ marginTop: 16 }}>Back to mock tests</Link>
      </div>
    );
  }

  const counts = mock.sectionQs || STANDARD_QS;
  const secMin = mock.sectionMinutes || SECTION_MINUTES;
  const minutes = strict ? secMin : Math.round(secMin * 1.5);
  const chosen = scopeSections(scope).filter((s) => available.includes(s));
  const valid = scopeValid(scope) && chosen.length > 0;
  const qCount = chosen.reduce((a, s) => a + (counts[s] || 0), 0);
  const tries = attempts.filter((a) => a.mockId === mock.id);
  const best = tries.length ? Math.max(...tries.map((t) => t.score)) : null;

  const start = () => {
    const q = new URLSearchParams();
    if (chosen.length < mock.sections.length) q.set('sections', chosen.join(','));
    if (minutes !== secMin) q.set('min', String(minutes));
    const qs = q.toString();
    nav(`/test/${mock.id}${qs ? `?${qs}` : ''}`);
  };

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <Link to="/mocks" className="small faint">← Mock Tests</Link>
          <h1 style={{ marginTop: 6 }}>{mock.title}</h1>
          <p>
            {mock.subtitle ? `${mock.subtitle} · ` : ''}{mock.questions} questions · {mock.minutes} min · {mock.difficulty}
          </p>
        </div>
      </div>

      <div className="grid g-main">
        <div className="card">
          <div className="card-head"><div><h2>Set up your test</h2><p>Take the whole paper, or only the sections you want to practise.</p></div></div>

          <div className="form-block">
            <div className="form-label">1 · What to attempt</div>
            <MockScopePicker value={scope} onChange={setScope} available={available} counts={counts} />
          </div>

          <div className="form-block">
            <div className="form-label">2 · Timing</div>
            <div className="check-row">
              <Check checked={strict} onChange={setStrict} sub={strict ? `${secMin} min per section, as in the exam` : `${minutes} min per section (relaxed)`}>
                Strict CAT timing
              </Check>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12, flexWrap: 'wrap', borderTop: '1px solid var(--border)', paddingTop: 16 }}>
            <span className="small muted">
              {valid
                ? `${scope.type === 'full' ? 'Full paper' : `${chosen.join(' + ')} only`} · ${qCount} questions · ${minutes * chosen.length} min`
                : 'Choose what to attempt to continue'}
            </span>
            <button className="btn primary" disabled={!valid} onClick={start}><IconTest />Start test</button>
          </div>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <div className="card">
            <div className="card-head"><div><h2>Paper at a glance</h2></div></div>
            <div className="list">
              {available.map((s) => {
                const on = chosen.includes(s);
                return (
                  <div className="row" key={s} style={{ opacity: on ? 1 : 0.45 }}>
                    <i className="dot" style={{ background: SECTIONS[s].color }} />
                    <div className="grow">
                      <div style={{ fontWeight: 500 }}>{SECTIONS[s].name}</div>
                      <div className="small faint">{counts[s] ?? '—'} questions</div>
                    </div>
                    <span className="chip"><IconClock width="12" />{minutes} min</span>
                    <span className={`chip ${on ? 'good' : ''}`}>{on ? 'Included' : 'Skipped'}</span>
                  </div>
                );
              })}
            </div>
          </div>

          <div className="card">
            <div className="card-head"><div><h2>Before you begin</h2></div></div>
            <div className="list small">
              <div className="row"><b style={{ width: 90 }}>Order</b><span className="muted">Sections run in order: {chosen.join(' → ') || '—'}. You can’t return to a section after submitting it.</span></div>
              <div className="row"><b style={{ width: 90 }}>Marking</b><span className="muted">+3 for a correct answer, −1 for a wrong MCQ, no penalty on TITA.</span></div>
              <div className="row"><b style={{ width: 90 }}>Percentile</b><span className="muted">{scope.type === 'full' ? 'Estimated after a full paper.' : 'Only estimated for full papers; sectionals get section scores.'}</span></div>
              {tries.length > 0 && (
                <div className="row"><b style={{ width: 90 }}>Your record</b><span className="muted">{tries.length} attempt{tries.length > 1 ? 's' : ''} · best score {best}</span></div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
