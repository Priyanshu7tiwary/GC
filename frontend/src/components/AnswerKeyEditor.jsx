import { useState } from 'react';
import { saveAnswerKey, isScored, toKey } from '../lib/pyq.js';

const letter = (i) => String.fromCharCode(65 + i);

/**
 * Admin control: shows a question's current answer key and lets an admin replace it.
 * `startOpen` skips the summary and goes straight to editing (used by "Correct the answer key").
 */
export default function AnswerKeyEditor({ q, onSaved, startOpen = false, onClose }) {
  const [editing, setEditing] = useState(startOpen);
  const [value, setValue] = useState(isScored(q) ? q.answer : q.type === 'mcq' ? null : '');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const current = !isScored(q) ? 'No key yet (not scored)' : q.type === 'mcq' ? `Option ${letter(q.answer)}` : q.answer;
  const close = () => { setEditing(false); setError(''); onClose?.(); };
  const save = async () => {
    setSaving(true); setError('');
    try {
      const saved = await saveAnswerKey(q.id, value);
      setEditing(false);
      onSaved?.(q.id, toKey(q.type, saved.answer));
      onClose?.();
    } catch (e) {
      setError(e.message || 'Could not save the answer key.');
    } finally {
      setSaving(false);
    }
  };
  const unchanged = isScored(q) && String(value) === String(q.answer);
  const empty = value === null || value === '';

  return (
    <div className="admin-key">
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
        <span className="chip accent">Admin</span>
        <span className="small">Answer key: <b>{current}</b>{q.corrected && <span className="faint"> · corrected</span>}</span>
        {!editing && <button className="btn sm" style={{ marginLeft: 'auto' }} onClick={() => setEditing(true)}>Change answer</button>}
      </div>
      {editing && (
        <div style={{ display: 'grid', gap: 10, marginTop: 10 }}>
          {q.type === 'mcq' ? (
            <div role="radiogroup" aria-label="Correct option" style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
              {q.options.map((o, i) => (
                <button key={i} type="button" role="radio" aria-checked={value === i} title={o}
                  className={`btn sm ${value === i ? 'primary' : ''}`} onClick={() => setValue(i)}>
                  {letter(i)}
                </button>
              ))}
            </div>
          ) : (
            <label className="field" style={{ maxWidth: 240 }}>Correct answer
              <input className="input num" inputMode="decimal" value={value ?? ''} onChange={(e) => setValue(e.target.value.replace(/[^0-9.-]/g, ''))} placeholder="e.g. 42 or 2413" />
            </label>
          )}
          {error && <p className="small" style={{ color: 'var(--bad)', margin: 0 }}>{error}</p>}
          <div style={{ display: 'flex', gap: 8 }}>
            <button className="btn sm primary" disabled={saving || empty || unchanged} onClick={save}>{saving ? 'Saving…' : 'Save answer key'}</button>
            <button className="btn sm ghost" disabled={saving} onClick={close}>Cancel</button>
          </div>
          <p className="small faint" style={{ margin: 0 }}>The new key applies to everyone taking this paper and to future scoring.</p>
        </div>
      )}
    </div>
  );
}
