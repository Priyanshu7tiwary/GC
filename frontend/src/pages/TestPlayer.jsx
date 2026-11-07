import { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { findMock, SECTION_MINUTES } from '../data/mocks.js';
import { QUESTIONS } from '../data/questions.js';
import { SECTIONS } from '../data/syllabus.js';
import { useStore } from '../lib/store.jsx';
import { estPercentile } from '../lib/percentile.js';

const fmt = (s) => `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`;

export function scoreQuestion(q, ans) {
  if (ans === undefined || ans === '' || ans === null) return { result: 'skip', marks: 0 };
  const ok = q.type === 'mcq' ? ans === q.answer : String(ans).trim() === String(q.answer);
  return { result: ok ? 'correct' : 'wrong', marks: ok ? 3 : q.type === 'mcq' ? -1 : 0 };
}


export default function TestPlayer() {
  const { mockId } = useParams();
  const nav = useNavigate();
  const { addAttempt, customMocks } = useStore();
  const mock = findMock(mockId, customMocks);
  const [params] = useSearchParams();
  // Scope chosen on the setup page: ?sections=VARC,QA&min=40 (defaults to the whole mock)
  const pickedParam = params.get('sections');
  const sections = useMemo(() => {
    const all = mock?.sections || [];
    const picked = pickedParam ? pickedParam.split(',') : null;
    const chosen = picked ? all.filter((x) => picked.includes(x)) : all;
    return chosen.length ? chosen : all;
  }, [mock, pickedParam]);
  const SEC_MIN = Number(params.get('min')) || mock?.sectionMinutes || SECTION_MINUTES;
  const partial = mock && sections.length < mock.sections.length;
  const title = mock ? (partial ? `${mock.title} · ${sections.join(' + ')}` : mock.title) : '';

  const bank = useMemo(() => Object.fromEntries(sections.map((s) => {
    const pool = mock.questionIds ? mock.questionIds.map((id) => QUESTIONS.find((q) => q.id === id)).filter(Boolean) : QUESTIONS;
    return [s, pool.filter((q) => q.section === s)];
  })), [mock, sections]);
  const [secIdx, setSecIdx] = useState(0);
  const [qIdx, setQIdx] = useState(0);
  const [answers, setAnswers] = useState({});      // saved answers
  const [draft, setDraft] = useState(undefined);   // current unsaved selection
  const [visited, setVisited] = useState({});
  const [review, setReview] = useState({});
  const [left, setLeft] = useState(SEC_MIN * 60);
  const [confirm, setConfirm] = useState(null);    // 'section' | 'exit'
  const timeUsed = useRef({});

  const secId = sections[secIdx];
  const qs = bank[secId] || [];
  const q = qs[qIdx];

  useEffect(() => {
    if (!q) return;
    setVisited((v) => ({ ...v, [q.id]: true }));
    setDraft(answers[q.id]);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [q?.id]);

  useEffect(() => {
    const t = setInterval(() => {
      setLeft((l) => l - 1);
      timeUsed.current[secId] = (timeUsed.current[secId] || 0) + 1;
    }, 1000);
    return () => clearInterval(t);
  }, [secId]);

  const finish = (finalAnswers) => {
    const results = {};
    let score = 0, max = 0;
    sections.forEach((s) => {
      const st = { correct: 0, wrong: 0, unattempted: 0, score: 0, time: timeUsed.current[s] || 0 };
      bank[s].forEach((qq) => {
        const r = scoreQuestion(qq, finalAnswers[qq.id]);
        st.score += r.marks;
        st[r.result === 'skip' ? 'unattempted' : r.result]++;
      });
      results[s] = st;
      score += st.score;
      max += bank[s].length * 3;
    });
    const id = `a-${Date.now()}`;
    addAttempt({
      id, mockId: mock.id, title, date: Date.now(), score, max,
      percentile: sections.length === 3 ? estPercentile(score / max) : undefined,
      sections: results, answers: finalAnswers, questionIds: sections.flatMap((s) => bank[s].map((qq) => qq.id)), timeSpent: Object.values(timeUsed.current).reduce((a, b) => a + b, 0),
    });
    nav(`/mocks/result/${id}`, { replace: true });
  };

  const nextSection = (finalAnswers = answers) => {
    setConfirm(null);
    if (secIdx < sections.length - 1) {
      setSecIdx(secIdx + 1);
      setQIdx(0);
      setLeft(SEC_MIN * 60);
    } else finish(finalAnswers);
  };

  useEffect(() => {
    if (left <= 0 && mock) nextSection();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [left]);

  if (!mock) return <div className="page"><h1>Test not found</h1></div>;
  if (mock.kind === 'pyq') {
    return (
      <div className="page">
        <div className="card">
          <h1>{mock.title}</h1>
          <p className="muted">This verified CAT paper is available as a PDF. It is not yet an interactive question set, so this page will not score or record an attempt.</p>
          {mock.paperUrl && <a className="btn primary" href={mock.paperUrl} target="_blank" rel="noreferrer">Open official paper PDF</a>}
        </div>
      </div>
    );
  }
  if (!q) return null;

  const commit = (markReview) => {
    const next = { ...answers };
    if (draft === undefined || draft === '') delete next[q.id];
    else next[q.id] = draft;
    setAnswers(next);
    setReview((r) => ({ ...r, [q.id]: markReview }));
    if (qIdx < qs.length - 1) setQIdx(qIdx + 1);
    return next;
  };

  const statusOf = (qq) => {
    const has = answers[qq.id] !== undefined;
    if (review[qq.id]) return `review${has ? ' has-ans' : ''}`;
    if (has) return 'answered';
    if (visited[qq.id]) return 'skipped';
    return '';
  };
  const counts = qs.reduce((c, qq) => {
    const s = statusOf(qq).split(' ')[0] || 'notvisited';
    c[s] = (c[s] || 0) + 1;
    return c;
  }, {});

  return (
    <div className="player">
      <div className="player-top">
        <div>
          <div style={{ fontWeight: 700 }}>{title}</div>
          <div className="small faint">Section {secIdx + 1} of {sections.length} · sections are time-locked as in CAT</div>
        </div>
        <div className="tabs">
          {sections.map((s, i) => (
            <span key={s} className={`tab ${i === secIdx ? 'active' : ''}`} style={{ opacity: i < secIdx ? 0.5 : 1, display: 'inline-flex', gap: 6, alignItems: 'center' }}>
              <i className="dot" style={{ background: SECTIONS[s].color }} />{s}
            </span>
          ))}
        </div>
        <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
          <span className="small faint">Time left</span>
          <span className={`timer ${left < 300 ? 'low' : ''}`}>{fmt(Math.max(0, left))}</span>
          <button className="btn sm ghost" onClick={() => setConfirm('exit')}>Exit</button>
        </div>
      </div>

      <div className="player-body">
        <div style={{ display: 'flex', flexDirection: 'column', minWidth: 0 }}>
          <div className={`q-area ${q.passage ? 'split' : ''}`} style={{ flex: 1 }}>
            {q.passage && (
              <div className="card passage">
                <div className="small faint" style={{ marginBottom: 8, fontWeight: 600 }}>{q.section === 'VARC' ? 'Read the passage and answer the questions' : 'Read the set and answer the questions'}</div>
                {q.passage}
              </div>
            )}
            <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 8 }}>
                <h2>Question {qIdx + 1}</h2>
                <div style={{ display: 'flex', gap: 6 }}>
                  <span className="chip">{q.type === 'mcq' ? 'MCQ' : 'TITA'}</span>
                  <span className="chip good">+3</span>
                  <span className="chip bad">{q.type === 'mcq' ? '−1' : '0'}</span>
                </div>
              </div>
              <div className="q-text">{q.q}</div>
              {q.type === 'mcq' ? (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                  {q.options.map((o, i) => (
                    <button key={i} className={`option ${draft === i ? 'selected' : ''}`} onClick={() => setDraft(i)}>
                      <span className="key">{String.fromCharCode(65 + i)}</span><span>{o}</span>
                    </button>
                  ))}
                </div>
              ) : (
                <label className="field" style={{ maxWidth: 280 }}>Your answer
                  <input className="input num" inputMode="numeric" value={draft ?? ''} onChange={(e) => setDraft(e.target.value.replace(/[^0-9.\-]/g, ''))} placeholder="Type your answer" />
                </label>
              )}
            </div>
          </div>
          <div className="player-actions">
            <button className="btn" onClick={() => commit(true)}>Mark for review & next</button>
            <button className="btn" onClick={() => setDraft(undefined)}>Clear response</button>
            <div style={{ flex: 1 }} />
            <button className="btn" disabled={qIdx === 0} onClick={() => setQIdx(qIdx - 1)}>Previous</button>
            <button className="btn primary" onClick={() => commit(false)}>Save & next</button>
          </div>
        </div>

        <aside className="palette-panel">
          <div className="legend" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
            <span><span className="pal-btn answered" style={{ width: 18, height: 18, display: 'inline-block' }} />Answered ({counts.answered || 0})</span>
            <span><span className="pal-btn skipped" style={{ width: 18, height: 18, display: 'inline-block' }} />Not answered ({counts.skipped || 0})</span>
            <span><span className="pal-btn review" style={{ width: 18, height: 18, display: 'inline-block' }} />For review ({counts.review || 0})</span>
            <span><span className="pal-btn" style={{ width: 18, height: 18, display: 'inline-block' }} />Not visited ({counts.notvisited || 0})</span>
          </div>
          <div>
            <h3 style={{ marginBottom: 10 }}>{SECTIONS[secId].name}</h3>
            <div className="palette">
              {qs.map((qq, i) => (
                <button key={qq.id} className={`pal-btn ${statusOf(qq)} ${i === qIdx ? 'current' : ''}`} onClick={() => setQIdx(i)}>{i + 1}</button>
              ))}
            </div>
          </div>
          <p className="small faint" style={{ margin: 0 }}>Demo question set — a live deployment would load the full {mock.questions}-question paper from the API.</p>
          <button className="btn primary" style={{ marginTop: 'auto' }} onClick={() => setConfirm('section')}>
            {secIdx < sections.length - 1 ? `Submit ${secId} & continue` : 'Submit test'}
          </button>
        </aside>
      </div>

      {confirm && (
        <div className="modal-back" onClick={() => setConfirm(null)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            {confirm === 'exit' ? (
              <>
                <h2>Exit this test?</h2>
                <p className="muted">Your progress in this attempt will be lost.</p>
                <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end' }}>
                  <button className="btn" onClick={() => setConfirm(null)}>Keep going</button>
                  <button className="btn primary" style={{ background: 'var(--bad)', borderColor: 'var(--bad)' }} onClick={() => nav('/mocks')}>Exit test</button>
                </div>
              </>
            ) : (
              <>
                <h2>Submit {secIdx < sections.length - 1 ? `the ${secId} section` : 'the test'}?</h2>
                <p className="muted">
                  {counts.answered || 0} answered · {counts.review || 0} marked for review · {(counts.skipped || 0) + (counts.notvisited || 0)} unanswered.
                  {secIdx < sections.length - 1 && ' You cannot return to this section.'}
                </p>
                <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end' }}>
                  <button className="btn" onClick={() => setConfirm(null)}>Cancel</button>
                  <button className="btn primary" onClick={() => nextSection()}>Submit</button>
                </div>
              </>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
