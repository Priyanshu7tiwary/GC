import { Fragment, useCallback, useEffect, useState } from 'react';
import { api } from '../lib/api.js';
import { SECTIONS } from '../data/syllabus.js';
import RichText from './RichText.jsx';

// Admin: the question bank of every syllabus topic. Each question carries a label (section › chapter › topic)
// and can be moved to another topic in its section.
const ORIGIN_LABEL = { PYQ: 'CAT paper', BANK: 'Practice', IMPORTED: 'Imported', AI: 'AI' };
const PAGE = 50;

function QuestionList({ bank, banks, onMoved }) {
  const [rows, setRows] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [error, setError] = useState('');
  const sameSection = banks.filter((b) => b.section === bank.section && b.topicId);

  const load = useCallback(async (p) => {
    const q = bank.topicId ? `topic=${encodeURIComponent(bank.topicId)}` : `topic=unassigned&section=${bank.section}`;
    try {
      const r = await api(`/api/admin/questions?${q}&page=${p}&size=${PAGE}`);
      setRows((prev) => (p === 0 ? r.items : [...prev, ...r.items]));
      setTotal(r.total);
      setPage(p);
    } catch (e) {
      setError(e.message);
    }
  }, [bank.topicId, bank.section]);
  useEffect(() => { load(0); }, [load]);

  const move = async (row, topicId) => {
    try {
      await api(`/api/admin/questions/${encodeURIComponent(row.id)}/topic`, { method: 'PUT', body: { topicId } });
      setRows((prev) => prev.filter((r) => r.id !== row.id));
      setTotal((t) => t - 1);
      onMoved();
    } catch (e) {
      setError(e.message);
    }
  };

  if (error) return <p className="small" style={{ color: 'var(--bad)' }}>{error}</p>;
  return (
    <div className="bank-questions">
      {!rows.length && <p className="small muted" style={{ margin: 0 }}>{total === 0 ? 'No questions in this bank yet.' : 'Loading…'}</p>}
      {rows.map((r) => (
        <div key={r.id} className="bank-q">
          <div className="bank-q-meta">
            <span className="chip"><i className="dot" style={{ background: SECTIONS[r.section].color }} />{r.label}</span>
            <span className="chip">{ORIGIN_LABEL[r.origin] || r.origin}</span>
            <span className="chip">{r.type}</span>
            {!r.keyed && <span className="chip warn">No key</span>}
            {r.paperId && <span className="small faint">{r.paperId.replace('pyq-', 'CAT ').replace(/-(\d)$/, ' · Slot $1')} · Q{r.questionNumber}</span>}
          </div>
          <RichText className="small" text={r.prompt} />
          <label className="small faint bank-move">Move to
            <select className="input" value="" onChange={(e) => e.target.value && move(r, e.target.value)}>
              <option value="">Choose a {r.section} topic…</option>
              {sameSection.filter((b) => b.topicId !== r.topicId).map((b) => <option key={b.topicId} value={b.topicId}>{b.chapter} › {b.topic}</option>)}
            </select>
          </label>
        </div>
      ))}
      {rows.length < total && <button className="btn sm" onClick={() => load(page + 1)}>Show more ({total - rows.length} left)</button>}
    </div>
  );
}

export default function TopicBanks({ refreshKey }) {
  const [banks, setBanks] = useState(null);
  const [error, setError] = useState('');
  const [section, setSection] = useState('QA');
  const [open, setOpen] = useState(null);
  const [tick, setTick] = useState(0);

  useEffect(() => {
    api('/api/admin/questions/topics').then(setBanks, (e) => setError(e.message));
  }, [refreshKey, tick]);

  if (error) return <p className="small" style={{ color: 'var(--bad)' }}>{error}</p>;
  if (!banks) return <p className="muted">Loading…</p>;
  const shown = banks.filter((b) => b.section === section);
  const max = Math.max(1, ...shown.map((b) => b.questions));
  const chapters = [...new Set(shown.map((b) => b.chapter))];
  const keyOf = (b) => b.topicId || `unassigned:${b.section}`;

  return (
    <>
      <div className="tabs" role="group" aria-label="Section" style={{ marginBottom: 14 }}>
        {Object.keys(SECTIONS).map((k) => {
          const n = banks.filter((b) => b.section === k).reduce((t, b) => t + b.questions, 0);
          return (
            <button key={k} className={`tab ${section === k ? 'active' : ''}`} aria-pressed={section === k} onClick={() => { setSection(k); setOpen(null); }}>
              <i className="dot" style={{ background: SECTIONS[k].color, marginRight: 6 }} />{k} · {n}
            </button>
          );
        })}
      </div>
      <div className="table-wrap">
        <table className="table bank-table">
          <thead><tr><th>Topic</th><th style={{ width: '30%' }}>Questions</th><th>Keyed</th><th>From</th><th /></tr></thead>
          <tbody>
            {chapters.map((ch) => (
              <Fragment key={ch}>
                <tr className="bank-chapter"><td colSpan={5}>{section} › {ch}</td></tr>
                {shown.filter((b) => b.chapter === ch).map((b) => {
                  const k = keyOf(b);
                  const isOpen = open === k;
                  return (
                    <Fragment key={k}>
                      <tr className={isOpen ? 'open' : ''}>
                        <td style={{ fontWeight: 600 }}>{b.topic}{b.highYield && <span className="chip accent" style={{ marginLeft: 8, padding: '0 6px' }}>High yield</span>}</td>
                        <td>
                          <div className="bank-bar"><span style={{ width: `${(b.questions / max) * 100}%`, background: SECTIONS[b.section].color }} /></div>
                          <span className="num small">{b.questions}</span>
                        </td>
                        <td className="num">{b.keyed}</td>
                        <td className="small faint">{Object.entries(b.byOrigin || {}).map(([o, n]) => `${ORIGIN_LABEL[o] || o} ${n}`).join(' · ') || '—'}</td>
                        <td><button className="btn sm ghost" aria-expanded={isOpen} onClick={() => setOpen(isOpen ? null : k)}>{isOpen ? 'Hide' : 'Questions'}</button></td>
                      </tr>
                      {isOpen && (
                        <tr className="bank-open"><td colSpan={5}>
                          <QuestionList bank={b} banks={banks} onMoved={() => setTick((t) => t + 1)} />
                        </td></tr>
                      )}
                    </Fragment>
                  );
                })}
              </Fragment>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}
