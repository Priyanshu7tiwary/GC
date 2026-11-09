import { useEffect, useMemo, useRef, useState } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { SECTION_MINUTES } from '../data/mocks.js';
import { useMock } from '../lib/catalog.js';
import { SECTIONS } from '../data/syllabus.js';
import { useStore } from '../lib/store.jsx';
import { isScored, loadAnswerKeys, toKey } from '../lib/pyq.js';
import { scoreQuestion } from '../lib/scoring.js';
import { AdminModeSwitch, useAdminMode } from '../lib/adminMode.jsx';
import AnswerKeyEditor from '../components/AnswerKeyEditor.jsx';
import RichText from '../components/RichText.jsx';
import { loadServerPaper, submitServerAttempt } from '../lib/mockGenerator.js';

const fmt = (s) => `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`;

export { scoreQuestion };


export default function TestPlayer() {
  const { mockId } = useParams();
  return <MockGate mockId={mockId}>{(mock) => <Player key={mock.id} mock={mock} />}</MockGate>;
}

/** Waits for the mock (previous-year papers come from the API), then mounts the page with it. */
function MockGate({ mockId, children }) {
  const { customMocks } = useStore();
  const { mock, loading, error } = useMock(mockId, customMocks);
  if (!mock) {
    return (
      <div className="page"><div className="card">
        {loading ? <div className="loading-card" role="status"><span className="ring" /><span>Loading the test…</span></div>
          : <><h1>Test not found</h1>{error && <p className="muted">Couldn’t load the list of tests: {error.message}</p>}
            <Link to="/mocks" className="btn" style={{ marginTop: 16 }}>Back to mock tests</Link></>}
      </div></div>
    );
  }
  return children(mock);
}

function Player({ mock }) {
  const nav = useNavigate();
  const { addAttempt } = useStore();
  const { adminMode } = useAdminMode();
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
  const isPyq = mock?.kind === 'pyq';
  // previous-year papers and generated mocks are served (without keys) and scored by the API
  const isServer = mock?.source === 'server';
  const [server, setServer] = useState({ questions: null, error: null });
  useEffect(() => {
    if (!isServer) return undefined;
    let live = true;
    loadServerPaper(mock.id).then((questions) => live && setServer({ questions, error: null }),
      (error) => live && setServer({ questions: null, error }));
    return () => { live = false; };
  }, [isServer, mock?.id]);
  const [submit, setSubmit] = useState({ busy: false, error: null, answers: null });
  const ready = (!isServer || !!server.questions) && !submit.answers;

  // Admin mode on a previous-year paper: the server sends this admin the current keys so they can be checked and changed
  const [keys, setKeys] = useState(null); // { [questionId]: { answer, corrected } }
  useEffect(() => {
    if (!adminMode || !isPyq || keys) return undefined;
    let live = true;
    loadAnswerKeys(mock.id).then((k) => live && setKeys(k), () => {});
    return () => { live = false; };
  }, [adminMode, isPyq, mock?.id, keys]);
  const served = useMemo(() => (server.questions && keys
    ? server.questions.map((x) => (keys[x.id] ? { ...x, answer: toKey(x.type, keys[x.id].answer), scored: keys[x.id].answer != null, corrected: keys[x.id].corrected } : x))
    : server.questions), [server.questions, keys]);
  const onKeySaved = (id, answer) => setKeys((k) => ({ ...k, [id]: { answer, corrected: true } }));

  const bank = useMemo(() => Object.fromEntries(sections.map((s) => {
    const pool = served || [];
    return [s, pool.filter((q) => q.section === s)];
  })), [mock, sections, isServer, served]);
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
    if (!ready) return undefined; // the clock starts once the paper has loaded
    const t = setInterval(() => {
      setLeft((l) => l - 1);
      timeUsed.current[secId] = (timeUsed.current[secId] || 0) + 1;
    }, 1000);
    return () => clearInterval(t);
  }, [secId, ready]);

  // Every mock is scored by the server; the result page then loads the solutions from it.
  const finish = async (finalAnswers) => {
    setSubmit({ busy: true, error: null, answers: finalAnswers });
    try {
      const attempt = await submitServerAttempt(mock, sections, finalAnswers, timeUsed.current);
      addAttempt({ ...attempt, title });
      nav(`/mocks/result/${attempt.id}`, { replace: true });
    } catch (e) {
      setSubmit({ busy: false, error: e.message || 'Could not submit the test.', answers: finalAnswers });
    }
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
    if (left <= 0 && mock && ready) nextSection();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [left]);

  if (submit.answers) {
    return (
      <div className="page">
        <div className="card">
          <h1>{title}</h1>
          {submit.error ? (
            <>
              <p className="muted">Your answers are safe in this tab, but they couldn’t be submitted: {submit.error}</p>
              <button className="btn primary" disabled={submit.busy} onClick={() => finish(submit.answers)}>Try again</button>
            </>
          ) : <p className="muted" style={{ marginBottom: 0 }}>Scoring your test…</p>}
        </div>
      </div>
    );
  }
  if (isServer && !server.questions) {
    return (
      <div className="page">
        <div className="card">
          <h1>{mock.title}</h1>
          <p className="muted" style={{ marginBottom: 0 }}>
            {server.error ? `This mock couldn’t be loaded: ${server.error.message}` : 'Loading your mock… The timer starts once it’s ready.'}
          </p>
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
          {isPyq && <AdminModeSwitch compact />}
          <button className="btn sm ghost" onClick={() => setConfirm('exit')}>Exit</button>
        </div>
      </div>

      <div className="player-body">
        <div style={{ display: 'flex', flexDirection: 'column', minWidth: 0 }}>
          <div className={`q-area ${q.passage ? 'split' : ''}`} style={{ flex: 1 }}>
            {q.passage && (
              <div className="card passage">
                <div className="small faint" style={{ marginBottom: 8, fontWeight: 600 }}>{q.section === 'VARC' ? 'Read the passage and answer the questions' : 'Read the set and answer the questions'}</div>
                <RichText text={q.passage} />
              </div>
            )}
            <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 8 }}>
                <h2>Question {qIdx + 1}</h2>
                <div style={{ display: 'flex', gap: 6 }}>
                  <span className="chip">{q.type === 'mcq' ? 'MCQ' : 'TITA'}</span>
                  {isScored(q) ? (
                    <>
                      <span className="chip good">+3</span>
                      <span className="chip bad">{q.type === 'mcq' ? '−1' : '0'}</span>
                    </>
                  ) : <span className="chip" title="No verified answer key is available for this question">Not scored</span>}
                </div>
              </div>
              {q.figure && (
                <p className="small faint" style={{ margin: 0 }}>This question refers to a chart, grid or figure in the original paper that isn’t reproduced here.</p>
              )}
              <RichText className="q-text" text={q.q} />
              {q.type === 'mcq' ? (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                  {q.options.map((o, i) => (
                    <button key={i} className={`option ${draft === i ? 'selected' : ''} ${adminMode && isPyq && q.answer === i ? 'correct' : ''}`} onClick={() => setDraft(i)}>
                      <span className="key">{String.fromCharCode(65 + i)}</span><RichText as="span" text={o} />
                    </button>
                  ))}
                </div>
              ) : (
                <label className="field" style={{ maxWidth: 280 }}>Your answer
                  <input className="input num" inputMode="numeric" value={draft ?? ''} onChange={(e) => setDraft(e.target.value.replace(/[^0-9.\-]/g, ''))} placeholder="Type your answer" />
                </label>
              )}
              {adminMode && isPyq && keys && <AnswerKeyEditor key={`${q.id}-${q.answer}`} q={q} onSaved={onKeySaved} />}
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
          <p className="small faint" style={{ margin: 0 }}>
            {isPyq
              ? `Official CAT ${mock.year} Slot ${mock.slot} paper · ${qs.length} ${secId} questions.`
              : isServer
                ? `AI-generated mock · ${qs.length} ${secId} questions · scored on submit.`
                : `Practice question set · ${qs.length} ${secId} questions.`}
          </p>
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
