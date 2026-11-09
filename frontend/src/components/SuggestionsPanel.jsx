import { useEffect, useMemo, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useStore, catDate, today } from '../lib/store.jsx';
import { buildSuggestions } from '../lib/suggestions.js';
import { IconArrow } from './Icons.jsx';
import { fetchAiState, generateAiPlan, untilText } from '../lib/aiSuggestions.js';

const PERCENTILES = [90, 95, 97, 98, 99, 99.5];
const PRIORITY = { high: { label: 'High priority', cls: 'bad' }, medium: { label: 'Medium', cls: 'warn' }, low: { label: 'Nice to do', cls: '' } };

export default function SuggestionsPanel({ open, onClose }) {
  const state = useStore();
  const nav = useNavigate();
  const [loading, setLoading] = useState(false);
  const [dismissed, setDismissed] = useState([]);
  // The AI study plan: opening the panel only loads the latest plan and the allowance; a new plan is made on request
  const [ai, setAi] = useState({ status: 'loading', data: null, error: '' }); // status: loading | ready | generating | error
  const [now, setNow] = useState(Date.now());
  useEffect(() => {
    if (!open) return undefined;
    let live = true;
    setAi({ status: 'loading', data: null, error: '' });
    fetchAiState().then(
      (data) => live && setAi({ status: 'ready', data, error: '' }),
      (e) => live && setAi({ status: 'error', data: null, error: e.message }),
    );
    const tick = setInterval(() => setNow(Date.now()), 60000); // keeps the countdown current
    return () => { live = false; clearInterval(tick); };
  }, [open]);

  const generate = async () => {
    setAi((a) => ({ ...a, status: 'generating', error: '' }));
    try {
      setAi({ status: 'ready', data: await generateAiPlan(state), error: '' });
    } catch (e) {
      // keep showing the last plan; refresh the allowance in case it changed
      const fresh = await fetchAiState().catch(() => null);
      setAi((a) => ({ status: 'ready', data: fresh || a.data, error: e.message }));
    }
  };

  // Recompute each time the panel opens
  const suggestions = useMemo(
    () => buildSuggestions(state),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [open, state.targetPercentile, state.targetYear],
  );

  useEffect(() => {
    if (!open) return;
    setLoading(true);
    const t = setTimeout(() => setLoading(false), 650);
    const onKey = (e) => e.key === 'Escape' && onClose();
    window.addEventListener('keydown', onKey);
    return () => { clearTimeout(t); window.removeEventListener('keydown', onKey); };
  }, [open, onClose]);

  if (!open) return null;
  // Next three CAT sittings that haven't happened yet
  const firstYear = new Date().getFullYear() + (catDate(new Date().getFullYear()) < today() ? 1 : 0);
  const years = [firstYear, firstYear + 1, firstYear + 2];
  const visible = suggestions.filter((s) => !dismissed.includes(s.id));
  const fulls = state.attempts.filter((a) => Object.keys(a.sections).length === 3).length;

  return (
    <div className="drawer-back" onClick={onClose}>
      <aside className="drawer" onClick={(e) => e.stopPropagation()} role="dialog" aria-label="Suggestions">
        <div className="drawer-head">
          <div>
            <h2>✦ Suggestions for you</h2>
            <p className="small faint" style={{ margin: '2px 0 0' }}>Aiming for {state.targetPercentile} %ile in CAT {state.targetYear} · based on {fulls} mocks, coverage, revision & study activity</p>
          </div>
          <button className="btn ghost sm" onClick={onClose} aria-label="Close">✕</button>
        </div>

        <div className="target-box">
          <div>
            <div className="form-label" style={{ marginBottom: 6 }}>Target percentile</div>
            <div className="tabs">
              {PERCENTILES.map((p) => (
                <button key={p} className={`tab ${state.targetPercentile === p ? 'active' : ''}`} onClick={() => state.updateSettings({ targetPercentile: p })}>{p}</button>
              ))}
            </div>
          </div>
          <div>
            <div className="form-label" style={{ marginBottom: 6 }}>Target year</div>
            <div className="tabs">
              {years.map((y) => (
                <button key={y} className={`tab ${state.targetYear === y ? 'active' : ''}`} onClick={() => state.updateSettings({ targetYear: y, examDate: catDate(y) })}>CAT {y}</button>
              ))}
            </div>
          </div>
        </div>

        <div className="drawer-body">
          <AiPlan ai={ai} now={now} target={state.targetPercentile} onGenerate={generate} onNavigate={onClose} />
          <h3 className="small faint" style={{ margin: '6px 0 0', textTransform: 'uppercase', letterSpacing: '.06em' }}>Quick checks</h3>
          {loading ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
              <div className="small muted" style={{ display: 'flex', gap: 8, alignItems: 'center' }}><span className="spinner" />Analysing your data…</div>
              {[0, 1, 2].map((i) => <div key={i} className="skeleton" />)}
            </div>
          ) : visible.length ? (
            visible.map((s, i) => (
              <div key={s.id} className="sugg" style={{ animationDelay: `${i * 50}ms` }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8, alignItems: 'center' }}>
                  <div style={{ display: 'flex', gap: 6 }}>
                    <span className={`chip ${PRIORITY[s.priority].cls}`}>{PRIORITY[s.priority].label}</span>
                    <span className="chip">{s.category}</span>
                  </div>
                  <button className="btn ghost sm" style={{ padding: '2px 6px' }} title="Dismiss" onClick={() => setDismissed([...dismissed, s.id])}>✕</button>
                </div>
                <h3 style={{ margin: '10px 0 4px', fontSize: 15 }}>{s.title}</h3>
                <p className="small muted" style={{ margin: 0, lineHeight: 1.55 }}>{s.body}</p>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 12, gap: 8 }}>
                  <span className="small faint num">{s.metric}</span>
                  <button className="btn sm primary" onClick={() => { onClose(); nav(s.action.to); }}>{s.action.label} <IconArrow /></button>
                </div>
              </div>
            ))
          ) : (
            <div style={{ textAlign: 'center', padding: '48px 0' }}>
              <div style={{ fontSize: 28 }}>🎯</div>
              <h3 style={{ marginTop: 6 }}>You’re on track</h3>
              <p className="small muted">No suggestions right now. Take a mock or log some study to get fresh ones.</p>
            </div>
          )}
        </div>

      </aside>
    </div>
  );
}

function AiPlan({ ai, now, target, onGenerate, onNavigate }) {
  if (ai.status === 'loading') {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }} role="status">
        <div className="small muted" style={{ display: 'flex', gap: 8, alignItems: 'center' }}><span className="spinner" />Loading your AI plan…</div>
        <div className="skeleton" style={{ height: 80 }} />
      </div>
    );
  }
  if (ai.status === 'error' && !ai.data) {
    return <div className="sugg"><p className="small muted" style={{ margin: 0 }}>The AI study plan isn’t available: {ai.error} The checks below still work.</p></div>;
  }
  const { data } = ai;
  const plan = data.plan;
  const q = data.quota;
  const generating = ai.status === 'generating';
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
      <div className="sugg">
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8, flexWrap: 'wrap' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}><span className="chip">✦ AI study plan</span>{plan && <span className="small faint">{plan.model}</span>}</div>
          {data.available && (
            <button className="btn sm primary" disabled={generating || q.remaining <= 0} onClick={onGenerate}>
              {generating ? 'Generating…' : plan ? 'Generate a new plan' : 'Generate my plan'}
            </button>
          )}
        </div>
        {data.available ? (
          <p className="small muted" style={{ margin: '8px 0 0' }} role="status">
            {generating ? 'Uses one of this week’s plans.'
              : q.remaining > 0 ? `${q.remaining} of ${q.limit} AI plans left this week.`
              : <>You’ve used all {q.limit} AI plans this week. <b>Next plan opens {untilText(q.nextAvailableAt, now)}.</b></>}
          </p>
        ) : (
          <p className="small muted" style={{ margin: '8px 0 0' }}>AI plans aren’t set up on this server yet (no AI key). The checks below still work.</p>
        )}
        {ai.error && <p className="small bad-text" style={{ margin: '8px 0 0' }} role="alert">{ai.error}</p>}
        {generating && <PlanBuilding />}
        {!generating && plan?.likelihood && <LikelihoodMeter key={plan.generatedAt} l={plan.likelihood} target={target} />}
        {!generating && plan?.summary && <p className="small" style={{ margin: '10px 0 0', lineHeight: 1.6 }}>{plan.summary}</p>}
        {!generating && plan?.generatedAt && <p className="small faint" style={{ margin: '6px 0 0' }}>Made {new Date(plan.generatedAt).toLocaleDateString(undefined, { day: 'numeric', month: 'short' })}</p>}
      </div>

      {!generating && plan?.topics?.length > 0 && (
        <div className="sugg">
          <h3 style={{ margin: '0 0 8px', fontSize: 15 }}>Topics to cover</h3>
          <div className="list">
            {plan.topics.map((t) => (
              <div className="row" key={t.topicId} style={{ alignItems: 'flex-start', flexWrap: 'wrap' }}>
                <div className="grow" style={{ minWidth: 180 }}>
                  <div style={{ display: 'flex', gap: 6, alignItems: 'center', flexWrap: 'wrap' }}>
                    <Link to={t.link} onClick={onNavigate} style={{ fontWeight: 600 }}>{t.name}</Link>
                    <span className={`chip ${PRIORITY[t.priority]?.cls || ''}`}>{PRIORITY[t.priority]?.label || t.priority}</span>
                  </div>
                  <div className="small faint">{t.section} · {t.chapter} · {t.status} · {t.solved}/{t.questionsInBank} solved</div>
                  {t.reason && <p className="small muted" style={{ margin: '4px 0 0', lineHeight: 1.5 }}>{t.reason}</p>}
                </div>
                <Link className="btn sm" to={t.link} onClick={onNavigate}>Practise <IconArrow /></Link>
              </div>
            ))}
          </div>
        </div>
      )}

      {!generating && plan?.tests?.length > 0 && (
        <div className="sugg">
          <h3 style={{ margin: '0 0 8px', fontSize: 15 }}>Tests to attempt</h3>
          <div className="list">
            {plan.tests.map((m) => (
              <div className="row" key={m.mockId} style={{ alignItems: 'flex-start', flexWrap: 'wrap' }}>
                <div className="grow" style={{ minWidth: 180 }}>
                  <div style={{ display: 'flex', gap: 6, alignItems: 'center', flexWrap: 'wrap' }}>
                    <Link to={m.link} onClick={onNavigate} style={{ fontWeight: 600 }}>{m.title}</Link>
                    <span className="chip">{m.attempted ? 'Retake' : 'New'}</span>
                  </div>
                  {m.reason && <p className="small muted" style={{ margin: '4px 0 0', lineHeight: 1.5 }}>{m.reason}</p>}
                </div>
                <Link className="btn sm primary" to={m.link} onClick={onNavigate}>Open test <IconArrow /></Link>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

const TONE = (p) => (p < 35 ? 'bad' : p < 60 ? 'warn' : 'good');

/** How likely the target is: a bar that fills to the AI's estimate while the number counts up. */
function LikelihoodMeter({ l, target }) {
  const [shown, setShown] = useState(0);
  const raf = useRef(0);
  useEffect(() => {
    if (window.matchMedia?.('(prefers-reduced-motion: reduce)').matches) { setShown(l.percent); return undefined; }
    const start = performance.now(), dur = 1100;
    const step = (t) => {
      const k = Math.min(1, (t - start) / dur);
      setShown(Math.round(l.percent * (1 - Math.pow(1 - k, 3)))); // ease-out
      if (k < 1) raf.current = requestAnimationFrame(step);
    };
    raf.current = requestAnimationFrame(step);
    return () => cancelAnimationFrame(raf.current);
  }, [l.percent]);
  const tone = TONE(l.percent);
  return (
    <div className="likelihood" role="meter" aria-valuemin={0} aria-valuemax={100} aria-valuenow={l.percent} aria-label="Chance of reaching your target">
      <div className="likelihood-head">
        <span className="small faint" style={{ textTransform: 'uppercase', letterSpacing: '.06em' }}>Chance of reaching your target</span>
        <span className={`chip ${tone}`}>{l.label}</span>
      </div>
      <div className="likelihood-row">
        <b className="num likelihood-num">{shown}%</b>
        <div className="likelihood-track"><span className={`likelihood-fill ${tone}`} style={{ width: `${shown}%` }} /></div>
      </div>
      {l.reason && <p className="small muted" style={{ margin: '6px 0 0', lineHeight: 1.5 }}>{l.reason}</p>}
      {l.realisticPercentile != null && l.realisticPercentile < target && (
        <p className="small" style={{ margin: '6px 0 0' }}>Realistic aim this CAT: <b className="num">{l.realisticPercentile} %ile</b> <span className="faint">(target {target})</span></p>
      )}
      <p className="small faint" style={{ margin: '4px 0 0' }}>
        AI estimate · {l.confidence} confidence{l.confidence === 'low' ? ': take a full mock for a firmer read' : ''}
      </p>
    </div>
  );
}

const BUILD_STEPS = ['Reading your mock scores', 'Measuring the gap to your target', 'Choosing the topics that matter most', 'Picking the papers to attempt', 'Writing your plan'];

/** Shown while the AI works: what it is doing, a progress bar that eases towards the end, and a preview of the plan's shape. */
function PlanBuilding() {
  const [step, setStep] = useState(0);
  useEffect(() => {
    const t = setInterval(() => setStep((i) => Math.min(i + 1, BUILD_STEPS.length - 1)), 1300);
    return () => clearInterval(t);
  }, []);
  return (
    <div className="plan-building" role="status" aria-live="polite">
      <div className="pb-head">
        <span className="pb-orb" aria-hidden="true"><i /><i /><i /></span>
        <div>
          <div className="pb-title">Building your study plan</div>
          <div className="small faint">Usually 3–10 seconds</div>
        </div>
      </div>
      <ol className="pb-steps">
        {BUILD_STEPS.map((label, i) => (
          <li key={label} className={i < step ? 'done' : i === step ? 'now' : ''}>
            <span className="pb-tick" aria-hidden="true">{i < step ? '✓' : ''}</span>{label}
          </li>
        ))}
      </ol>
      <div className="pb-bar"><span /></div>
      <div className="pb-ghost" aria-hidden="true">
        <div className="skeleton" style={{ height: 44 }} />
        <div className="skeleton" style={{ height: 30, width: '85%' }} />
        <div className="skeleton" style={{ height: 30, width: '70%' }} />
      </div>
    </div>
  );
}
