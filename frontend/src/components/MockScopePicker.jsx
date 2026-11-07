import { useState } from 'react';
import { SECTIONS } from '../data/syllabus.js';
import { IconCheck } from './Icons.jsx';

const ALL = ['VARC', 'DILR', 'QA'];
const DEFAULT_COUNTS = { VARC: 24, DILR: 22, QA: 22 };

export function Check({ checked, onChange, children, sub, disabled }) {
  return (
    <label className={`check-card ${checked ? 'on' : ''} ${disabled ? 'disabled' : ''}`}>
      <input type="checkbox" checked={checked} disabled={disabled} onChange={(e) => onChange(e.target.checked)} />
      <span className="check-box">{checked && <IconCheck width="13" />}</span>
      <span><b>{children}</b>{sub && <span className="small faint" style={{ display: 'block' }}>{sub}</span>}</span>
    </label>
  );
}

/** Sections a scope covers, in CAT order. */
export const scopeSections = (scope) => (scope.type === 'full' ? ALL : ALL.filter((s) => scope.subjects.includes(s)));
export const scopeValid = (scope) => scope.type === 'full' || (scope.type === 'sectional' && scope.subjects.length > 0);

/**
 * Full mock / Sectional checkboxes. Choosing Sectional opens a subject picker;
 * ticking all three subjects switches the choice to Full mock.
 * `available` limits which sections can be chosen (e.g. a VARC-only sectional test).
 */
export default function MockScopePicker({ value, onChange, available = ALL, counts = DEFAULT_COUNTS }) {
  const [note, setNote] = useState('');
  const fullPossible = ALL.every((s) => available.includes(s));
  const total = ALL.reduce((a, s) => a + (counts[s] || 0), 0);

  const pickType = (t, on) => {
    setNote('');
    if (!on) return onChange({ type: null, subjects: [] });
    onChange({ type: t, subjects: t === 'full' ? [] : available.length === 1 ? [...available] : [] });
  };

  const toggleSubject = (s, on) => {
    const next = on ? [...value.subjects, s] : value.subjects.filter((x) => x !== s);
    if (fullPossible && next.length === ALL.length) {
      setNote('All three sections selected — switched to a Full mock.');
      return onChange({ type: 'full', subjects: [] });
    }
    setNote('');
    onChange({ type: 'sectional', subjects: next });
  };

  return (
    <div>
      <div className="check-row">
        <Check checked={value.type === 'full'} onChange={(on) => pickType('full', on)} disabled={!fullPossible}
          sub={fullPossible ? `VARC + DILR + QA · ${total} Qs` : 'Not available for this test'}>Full mock</Check>
        <Check checked={value.type === 'sectional'} onChange={(on) => pickType('sectional', on)}
          sub={fullPossible ? 'One or two sections' : `${available.join(' + ')} only`}>Sectional</Check>
      </div>
      {value.type === 'sectional' && (
        <div className="subject-pop">
          <div className="small muted" style={{ marginBottom: 8 }}>Choose subject(s)</div>
          <div className="check-row">
            {ALL.map((id) => {
              const s = SECTIONS[id];
              const ok = available.includes(id);
              return (
                <Check key={id} checked={value.subjects.includes(id)} onChange={(on) => toggleSubject(id, on)} disabled={!ok}
                  sub={ok ? `${counts[id] ?? '—'} Qs` : 'Not in this test'}>
                  <i className="dot" style={{ background: s.color, marginRight: 6 }} />{s.short}
                </Check>
              );
            })}
          </div>
        </div>
      )}
      {note && <div className="chip accent" style={{ marginTop: 10 }}>{note}</div>}
    </div>
  );
}
