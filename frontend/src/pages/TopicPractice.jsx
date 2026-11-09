import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useStore } from '../lib/store.jsx';
import { ALL_TOPICS, SECTIONS } from '../data/syllabus.js';
import { topicQuestions, checkAnswer, markQuestion } from '../lib/practice.js';
import RichText from '../components/RichText.jsx';

const PAGE = 20;
const FILTERS = [['all', 'All'], ['unsolved', 'Unsolved'], ['solved', 'Solved'], ['marked', 'Marked for revision']];
const ORIGIN = { PYQ: 'CAT paper', BANK: 'Practice', IMPORTED: 'Imported', AI: 'AI' };
const source = (q) => (q.paperId ? `${q.paperId.replace('pyq-', 'CAT ').replace(/-(\d)$/, ' · Slot $1')}${q.questionNumber ? ` · Q${q.questionNumber}` : ''}` : ORIGIN[q.origin] || q.origin);

function PracticeCard({ q, index, onChange }) {
  const [draft, setDraft] = useState(q.state.lastAnswer == null ? null : q.type === 'MCQ' ? Number(q.state.lastAnswer) : q.state.lastAnswer);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const revealed = q.correctAnswer != null;
  const key = revealed && q.type === 'MCQ' ? Number(q.correctAnswer) : q.correctAnswer;

  const act = async (fn) => {
    setBusy(true);
    setError('');
    try { await fn(); } catch (e) { setError(e.message); } finally { setBusy(false); }
  };
  const check = () => act(async () => {
    const r = await checkAnswer(q.id, draft);
    onChange(q, { ...q, state: r.state, correctAnswer: r.correctAnswer, explanation: r.explanation });
  });
  const mark = (patch) => act(async () => onChange(q, await markQuestion(q.id, patch)));

  const optionClass = (i) => {
    if (!revealed) return draft === i ? 'selected' : '';
    if (i === key) return 'correct';
    return draft === i ? 'wrong' : '';
  };

  return (
    <div className="card practice-q" id={`q-${q.id}`}>
      <div className="practice-q-head">
        <b className="num">Q{index}</b>
        <span className="chip">{q.type === 'MCQ' ? 'MCQ' : 'TITA'}</span>
        <span className="small faint">{source(q)}</span>
        <span className="grow" />
        {q.state.solved && <span className="chip good">Solved</span>}
        {q.state.marked && <span className="chip warn">Marked for revision</span>}
      </div>
      {q.hasFigure && <p className="small faint" style={{ margin: 0 }}>This question refers to a chart, grid or figure in the original paper that may not be reproduced here.</p>}
      <RichText className="q-text" text={q.prompt} />
      {q.type === 'MCQ' ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          {q.options.map((o, i) => (
            <button key={i} className={`option ${optionClass(i)}`} onClick={() => setDraft(i)} disabled={busy}>
              <span className="key">{String.fromCharCode(65 + i)}</span><RichText as="span" text={o} />
            </button>
          ))}
        </div>
      ) : (
        <label className="field" style={{ maxWidth: 280 }}>Your answer
          <input className="input num" inputMode="decimal" value={draft ?? ''} disabled={busy}
            onChange={(e) => setDraft(e.target.value.replace(/[^0-9.\-/]/g, ''))} placeholder="Type your answer" />
        </label>
      )}
      {revealed && q.state.attempts > 0 && q.state.lastCorrect != null && (
        <p className={`small ${q.state.lastCorrect ? 'good-text' : 'bad-text'}`} role="status" style={{ margin: 0 }}>
          {q.state.lastCorrect ? 'Correct. Marked solved.' : `Not quite.${q.type === 'TITA' ? ` The answer is ${q.correctAnswer}.` : ` The answer is ${String.fromCharCode(65 + key)}.`}`}
        </p>
      )}
      {revealed && q.type === 'TITA' && q.state.attempts === 0 && <p className="small muted" style={{ margin: 0 }}>Answer: <b className="num">{q.correctAnswer}</b></p>}
      {revealed && q.explanation && (
        <details className="practice-solution">
          <summary className="small">Solution</summary>
          <RichText className="small" text={q.explanation} />
        </details>
      )}
      {error && <p className="small bad-text" style={{ margin: 0 }}>{error}</p>}
      <div className="practice-actions">
        {q.keyed && (
          <button className="btn sm primary" disabled={busy || draft == null || draft === ''} onClick={check}>Check answer</button>
        )}
        <button className={`btn sm ${q.state.solved ? '' : 'ghost'}`} disabled={busy} aria-pressed={q.state.solved}
          onClick={() => mark({ solved: !q.state.solved })}>{q.state.solved ? '✓ Solved' : 'Mark solved'}</button>
        <button className={`btn sm ${q.state.marked ? '' : 'ghost'}`} disabled={busy} aria-pressed={q.state.marked}
          onClick={() => mark({ marked: !q.state.marked })}>{q.state.marked ? '★ Marked for revision' : 'Mark for revision'}</button>
        {!q.keyed && <span className="small faint">No answer key yet</span>}
      </div>
    </div>
  );
}

export default function TopicPractice() {
  const { topicId } = useParams();
  const { coverage, setCoverage, refreshPractice, logMinutes } = useStore();
  const t = ALL_TOPICS.find((x) => x.id === topicId);
  const [filter, setFilter] = useState('all');
  const [data, setData] = useState(null);
  const [items, setItems] = useState([]);
  const [error, setError] = useState('');
  const [loadingMore, setLoadingMore] = useState(false);
  const [idx, setIdx] = useState(0); // the question being shown

  const load = useCallback(async (page) => {
    try {
      const r = await topicQuestions(topicId, filter, page, PAGE);
      setData(r);
      setItems((prev) => (page === 0 ? r.items : [...prev, ...r.items]));
      setError('');
    } catch (e) {
      setError(e.message);
    }
  }, [topicId, filter]);
  useEffect(() => { setItems([]); setData(null); setIdx(0); load(0); }, [load]);

  // Keeps the page's counts and the topic's practised-questions counter in step with each change.
  const onChange = (before, after) => {
    setItems((list) => list.map((x) => (x.id === after.id ? after : x)));
    const dSolved = (after.state.solved ? 1 : 0) - (before.state.solved ? 1 : 0);
    const dMarked = (after.state.marked ? 1 : 0) - (before.state.marked ? 1 : 0);
    if (dSolved || dMarked) {
      setData((d) => d && { ...d, counts: { ...d.counts, solved: d.counts.solved + dSolved, marked: d.counts.marked + dMarked } });
    }
    if (dSolved) {
      // the topic's counter is the number of solved questions, which the server holds
      if (dSolved > 0 && (coverage[topicId]?.status ?? 0) === 0) setCoverage(topicId, { status: 1 });
      if (dSolved > 0) logMinutes(4); // a solved question counts as about 4 minutes of study
      refreshPractice();
    }
  };

  // Moves to another question, fetching the next batch from the server when the end of the loaded ones is reached.
  const go = async (to) => {
    if (to < 0 || !data || to >= data.total) return;
    if (to >= items.length) {
      setLoadingMore(true);
      await load(data.page + 1);
      setLoadingMore(false);
    }
    setIdx(to);
    window.scrollTo({ top: 0 });
  };

  const counts = data?.counts;
  const pct = counts?.questions ? Math.round((counts.solved / counts.questions) * 100) : 0;
  const section = t?.section || data?.section;

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <Link to="/coverage" className="small faint">← Syllabus coverage</Link>
          <h1 style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            {section && <i className="dot" style={{ background: SECTIONS[section].color }} />}
            {t?.name || data?.topic || 'Practice'}
          </h1>
          <p>{section} · {t?.chapter || data?.chapter}. Answer a question and check it, or mark it solved yourself. Only questions marked solved count towards this topic’s total.</p>
        </div>
      </div>

      {counts && (
        <div className="grid g-4 practice-stats" style={{ marginBottom: 18 }}>
          <div className="card"><div className="stat-label">Solved</div><div className="stat-value num">{counts.solved}<span className="stat-sub"> / {counts.questions}</span></div>
            <div className="bar" style={{ marginTop: 8 }}><span style={{ width: `${pct}%`, background: section ? SECTIONS[section].color : 'var(--accent)' }} /></div></div>
          <div className="card"><div className="stat-label">Unsolved</div><div className="stat-value num">{counts.questions - counts.solved}</div><div className="stat-sub">{counts.keyed} of {counts.questions} can be checked</div></div>
          <div className="card"><div className="stat-label">Marked for revision</div><div className="stat-value num">{counts.marked}</div><div className="stat-sub">Come back to these</div></div>
          <div className="card"><div className="stat-label">Counted on Coverage</div><div className="stat-value num">{coverage[topicId]?.practiced ?? 0}</div><div className="stat-sub">Solved questions only</div></div>
        </div>
      )}

      <div className="tabs" style={{ marginBottom: 14, flexWrap: 'wrap' }}>
        {FILTERS.map(([k, l]) => <button key={k} className={`tab ${filter === k ? 'active' : ''}`} onClick={() => setFilter(k)}>{l}</button>)}
      </div>

      {error && <div className="card"><p className="small bad-text" style={{ margin: 0 }}>{error}</p></div>}
      {!error && !data && <div className="loading-card" role="status"><span className="ring" /><span>Loading questions…</span></div>}
      {data && !items.length && (
        <div className="card" style={{ textAlign: 'center', padding: '36px 0' }}>
          <h3>Nothing here</h3>
          <p className="muted small">{filter === 'all' ? 'This topic’s question bank is empty for now.' : 'No questions match this filter.'}</p>
        </div>
      )}

      {data && items[idx] && (() => {
        const q = items[idx];
        const total = data.total;
        return (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
            <p className="num small muted" style={{ margin: 0 }} aria-live="polite">Question {idx + 1} of {total}</p>
            {q.passage && (
              <details className="card practice-passage" open>
                <summary className="small"><b>Passage / set</b> for this question</summary>
                <RichText text={q.passage} />
              </details>
            )}
            <PracticeCard key={q.id} q={q} index={idx + 1} onChange={onChange} />
            <div className="practice-nav">
              <button className="btn sm" disabled={idx === 0 || loadingMore} onClick={() => go(idx - 1)}>← Previous</button>
              <span className="small faint">{q.state.solved ? 'Solved' : 'Not solved yet'}</span>
              <button className="btn sm primary" disabled={idx + 1 >= total || loadingMore} onClick={() => go(idx + 1)}>{loadingMore ? 'Loading…' : 'Next →'}</button>
            </div>
          </div>
        );
      })()}
    </div>
  );
}
