import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useStore, catDate, today } from '../lib/store.jsx';
import { buildSuggestions } from '../lib/suggestions.js';
import { IconArrow } from './Icons.jsx';

const PERCENTILES = [90, 95, 97, 98, 99, 99.5];
const PRIORITY = { high: { label: 'High priority', cls: 'bad' }, medium: { label: 'Medium', cls: 'warn' }, low: { label: 'Nice to do', cls: '' } };

export default function SuggestionsPanel({ open, onClose }) {
  const state = useStore();
  const nav = useNavigate();
  const [loading, setLoading] = useState(false);
  const [version, setVersion] = useState(0);
  const [dismissed, setDismissed] = useState([]);

  // Recompute each time the panel opens or "Refresh" is pressed
  const suggestions = useMemo(
    () => buildSuggestions(state),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [version, open, state.targetPercentile, state.targetYear],
  );

  useEffect(() => {
    if (!open) return;
    setLoading(true);
    const t = setTimeout(() => setLoading(false), 650);
    const onKey = (e) => e.key === 'Escape' && onClose();
    window.addEventListener('keydown', onKey);
    return () => { clearTimeout(t); window.removeEventListener('keydown', onKey); };
  }, [open, version, onClose]);

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

        <div className="drawer-foot">
          <button className="btn" style={{ flex: 1 }} onClick={() => { setDismissed([]); setVersion((v) => v + 1); }}>Refresh suggestions</button>
        </div>
      </aside>
    </div>
  );
}
