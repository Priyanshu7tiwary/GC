import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useStore } from '../lib/store.jsx';
import { QUESTIONS } from '../data/questions.js';
import { SECTIONS } from '../data/syllabus.js';
import { scoreQuestion } from './TestPlayer.jsx';
import { Ring, SegBar } from '../components/Charts.jsx';
import { IconArrow } from '../components/Icons.jsx';

export default function Result() {
  const { attemptId } = useParams();
  const { attempts } = useStore();
  const a = attempts.find((x) => x.id === attemptId);
  const [filter, setFilter] = useState('all');
  if (!a) return <div className="page"><h1>Attempt not found</h1><Link to="/mocks" className="btn" style={{ marginTop: 16 }}>Back to mocks</Link></div>;

  const secs = Object.entries(a.sections);
  const tot = secs.reduce((t, [, s]) => ({ c: t.c + s.correct, w: t.w + s.wrong, u: t.u + s.unattempted }), { c: 0, w: 0, u: 0 });
  const acc = tot.c + tot.w ? Math.round((tot.c / (tot.c + tot.w)) * 100) : 0;
  const qs = !a.answers ? []
    : a.questionIds ? a.questionIds.map((id) => QUESTIONS.find((q) => q.id === id)).filter(Boolean)
    : QUESTIONS.filter((q) => a.sections[q.section]);
  const shown = qs.filter((q) => filter === 'all' || scoreQuestion(q, a.answers[q.id]).result === filter);

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <Link to="/mocks" className="small faint">← Mock Tests</Link>
          <h1 style={{ marginTop: 6 }}>{a.title} · Analysis</h1>
          <p>{new Date(a.date).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })}</p>
        </div>
        <Link to={`/mocks/start/${a.mockId}`} className="btn primary">Reattempt <IconArrow /></Link>
      </div>

      <div className="grid g-4" style={{ marginBottom: 18 }}>
        <div className="card" style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
          <Ring value={(Math.max(0, a.score) / a.max) * 100} label={a.score} sub={`of ${a.max}`} />
          <div><div className="stat-label">Total score</div><div className="stat-sub">{Math.round((a.score / a.max) * 100)}% of max</div></div>
        </div>
        <div className="card"><div className="stat-label">Est. percentile</div><div className="stat-value">{a.percentile ? a.percentile.toFixed(1) : '—'}</div><div className="stat-sub">{a.percentile ? 'Based on score-to-percentile model' : 'Sectional — not estimated'}</div></div>
        <div className="card"><div className="stat-label">Accuracy</div><div className="stat-value">{acc}%</div><div className="stat-sub">{tot.c} correct of {tot.c + tot.w} attempted</div></div>
        <div className="card"><div className="stat-label">Attempt rate</div><div className="stat-value">{Math.round(((tot.c + tot.w) / (tot.c + tot.w + tot.u)) * 100)}%</div><div className="stat-sub">{tot.u} left unattempted</div></div>
      </div>

      <div className="card" style={{ marginBottom: 18 }}>
        <div className="card-head"><div><h2>Section breakdown</h2></div>
          <div className="legend"><span><i className="dot" style={{ background: 'var(--good)' }} />Correct</span><span><i className="dot" style={{ background: 'var(--bad)' }} />Wrong</span><span><i className="dot" style={{ background: 'var(--surface-2)', border: '1px solid var(--border)' }} />Skipped</span></div>
        </div>
        <div className="table-wrap">
          <table className="table">
            <thead><tr><th>Section</th><th style={{ width: '35%' }}>Attempt split</th><th>Correct</th><th>Wrong</th><th>Skipped</th><th>Accuracy</th>{secs[0][1].time != null && <th>Time</th>}<th>Score</th></tr></thead>
            <tbody>
              {secs.map(([k, s]) => (
                <tr key={k}>
                  <td style={{ fontWeight: 600 }}><i className="dot" style={{ background: SECTIONS[k].color, marginRight: 8 }} />{k}</td>
                  <td><SegBar parts={[{ label: 'Correct', value: s.correct, color: 'var(--good)' }, { label: 'Wrong', value: s.wrong, color: 'var(--bad)' }, { label: 'Skipped', value: s.unattempted, color: 'transparent' }]} /></td>
                  <td className="num">{s.correct}</td><td className="num">{s.wrong}</td><td className="num">{s.unattempted}</td>
                  <td className="num">{s.correct + s.wrong ? Math.round((s.correct / (s.correct + s.wrong)) * 100) : 0}%</td>
                  {s.time != null && <td className="num">{Math.floor(s.time / 60)}m {s.time % 60}s</td>}
                  <td className="num" style={{ fontWeight: 700 }}>{s.score}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {a.answers ? (
        <div className="card">
          <div className="card-head">
            <div><h2>Solutions</h2><p>Review every question with explanations</p></div>
            <div className="tabs">
              {[['all', 'All'], ['correct', 'Correct'], ['wrong', 'Wrong'], ['skip', 'Skipped']].map(([k, l]) => (
                <button key={k} className={`tab ${filter === k ? 'active' : ''}`} onClick={() => setFilter(k)}>{l}</button>
              ))}
            </div>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
            {shown.map((q) => {
              const ans = a.answers[q.id];
              const r = scoreQuestion(q, ans);
              return (
                <div key={q.id} className="card" style={{ boxShadow: 'none', background: 'var(--bg)' }}>
                  <div style={{ display: 'flex', gap: 8, marginBottom: 10, flexWrap: 'wrap' }}>
                    <span className="chip"><i className="dot" style={{ background: SECTIONS[q.section].color }} />{q.section}</span>
                    <span className="chip">{q.topic}</span>
                    <span className={`chip ${r.result === 'correct' ? 'good' : r.result === 'wrong' ? 'bad' : ''}`}>
                      {r.result === 'correct' ? '✓ Correct' : r.result === 'wrong' ? '✗ Wrong' : 'Skipped'} · {r.marks > 0 ? '+' : ''}{r.marks}
                    </span>
                  </div>
                  <div className="q-text" style={{ marginBottom: 12 }}>{q.q}</div>
                  {q.type === 'mcq' ? (
                    <div style={{ display: 'grid', gap: 8, marginBottom: 12 }}>
                      {q.options.map((o, i) => (
                        <div key={i} className={`option ${i === q.answer ? 'correct' : i === ans ? 'wrong' : ''}`}>
                          <span className="key">{String.fromCharCode(65 + i)}</span><span>{o}</span>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <p className="small" style={{ margin: '0 0 12px' }}>Your answer: <b>{ans ?? '—'}</b> · Correct answer: <b style={{ color: 'var(--good)' }}>{q.answer}</b></p>
                  )}
                  <div className="small" style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 10, padding: 12 }}>
                    <b>Explanation. </b><span className="muted">{q.explanation}</span>
                  </div>
                </div>
              );
            })}
            {!shown.length && <p className="muted">Nothing in this filter.</p>}
          </div>
        </div>
      ) : (
        <div className="card"><p className="muted" style={{ margin: 0 }}>Question-level solutions are available for attempts taken on this device.</p></div>
      )}
    </div>
  );
}
