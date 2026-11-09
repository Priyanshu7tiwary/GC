// Analytics answers one question: "What should I practise next, and why?"
// Everything here is derived from data the app already records per attempt: section results, section time,
// answers and question ids. Question topics come from the question itself (paper JSON, server, or local bank).
import { useEffect, useState } from 'react';
import { ALL_TOPICS } from '../data/syllabus.js';
import { QUESTIONS } from '../data/questions.js';
import { loadServerSolutions } from './mockGenerator.js';
import { scoreQuestion } from './scoring.js';

export const SECTION_KEYS = ['VARC', 'DILR', 'QA'];

/** Section counts and rates. Unscored questions (no answer key) are left out of every rate. */
export function sectionStats(s) {
  const attempted = s.correct + s.wrong;
  const total = attempted + s.unattempted;
  return {
    correct: s.correct,
    wrong: s.wrong,
    skipped: s.unattempted,
    score: s.score,
    max: total * 3,
    accuracy: attempted ? s.correct / attempted : null,
    attemptRate: total ? attempted / total : null,
    seconds: s.time ?? null,
  };
}

export function attemptTotals(a) {
  const secs = Object.values(a.sections).map(sectionStats);
  const correct = secs.reduce((t, s) => t + s.correct, 0);
  const wrong = secs.reduce((t, s) => t + s.wrong, 0);
  const skipped = secs.reduce((t, s) => t + s.skipped, 0);
  return { correct, wrong, skipped, accuracy: correct + wrong ? correct / (correct + wrong) : null, attemptRate: correct + wrong + skipped ? (correct + wrong) / (correct + wrong + skipped) : null };
}

export const isFullLength = (a) => Object.keys(a.sections).length === 3;

/** Trailing moving average over the last n values (fewer at the start of the series). */
export function movingAverage(values, n = 3) {
  return values.map((_, i) => {
    const w = values.slice(Math.max(0, i - n + 1), i + 1).filter((v) => v != null);
    return w.length ? Math.round((w.reduce((a, b) => a + b, 0) / w.length) * 10) / 10 : null;
  });
}

export const fmtDate = (t) => new Date(t).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' });
export const pct = (v) => (v == null ? '—' : `${Math.round(v * 100)}%`);
export const mins = (s) => (s == null ? '—' : `${Math.floor(s / 60)}m ${String(s % 60).padStart(2, '0')}s`);

/* ---------------- question-level data ---------------- */

const solutionCache = new Map();

/** The questions of one attempt (with keys and topics), or null when the attempt has no question-level data. */
async function questionsFor(a) {
  if (!a.answers || !a.questionIds?.length) return null;
  let pool;
  if (a.serverAttemptId) {
    if (!solutionCache.has(a.serverAttemptId)) solutionCache.set(a.serverAttemptId, loadServerSolutions(a.serverAttemptId));
    pool = await solutionCache.get(a.serverAttemptId);
  } else if (a.mockId?.startsWith('pyq-')) {
    return null; // a paper attempt from before papers were scored by the server: no question-level data kept
  } else {
    pool = QUESTIONS;
  }
  const byId = new Map(pool.map((q) => [q.id, q]));
  return a.questionIds.map((id) => byId.get(id)).filter(Boolean);
}

/** Loads question-level data for the given attempts: { byAttempt: {id: question[]}, loading, failed }. */
export function useAttemptQuestions(attempts) {
  const [state, setState] = useState({ byAttempt: {}, loading: true, failed: 0 });
  const key = attempts.map((a) => a.id).join(',');
  useEffect(() => {
    let live = true;
    setState((s) => ({ ...s, loading: true }));
    Promise.allSettled(attempts.map((a) => questionsFor(a).then((qs) => [a.id, qs]))).then((results) => {
      if (!live) return;
      const byAttempt = {};
      let failed = 0;
      results.forEach((r) => {
        if (r.status === 'fulfilled') { if (r.value[1]) byAttempt[r.value[0]] = r.value[1]; } else failed++;
      });
      setState({ byAttempt, loading: false, failed });
    });
    return () => { live = false; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key]);
  return state;
}

const topicMeta = (section, name) => ALL_TOPICS.find((t) => t.section === section && t.name === name);

/**
 * Wrong and skipped questions grouped by topic, newest attempt weighted most.
 * Each topic: { section, topic, topicId, seen, correct, wrong, skipped, priority, items: [{attempt, question, result}] }
 */
export function topicReview(attempts, byAttempt) {
  const rows = new Map();
  const ordered = [...attempts].sort((a, b) => b.date - a.date);
  ordered.forEach((a, rank) => {
    const qs = byAttempt[a.id];
    if (!qs) return;
    const weight = Math.pow(0.8, rank); // a miss in the latest mock counts most
    qs.forEach((q) => {
      const r = scoreQuestion(q, a.answers[q.id]).result;
      if (r === 'unscored') return;
      const key = `${q.section}|${q.topic}`;
      if (!rows.has(key)) {
        const meta = topicMeta(q.section, q.topic);
        rows.set(key, { section: q.section, topic: q.topic, topicId: meta?.id, seen: 0, correct: 0, wrong: 0, skipped: 0, priority: 0, items: [] });
      }
      const row = rows.get(key);
      row.seen++;
      if (r === 'correct') row.correct++;
      if (r === 'wrong') { row.wrong++; row.priority += 1 * weight; }
      if (r === 'skip') { row.skipped++; row.priority += 0.5 * weight; }
      if (r === 'wrong' || r === 'skip') row.items.push({ attempt: a, question: q, result: r });
    });
  });
  return [...rows.values()]
    .filter((r) => r.wrong + r.skipped > 0)
    .map((r) => ({ ...r, accuracy: r.correct + r.wrong ? r.correct / (r.correct + r.wrong) : null }))
    .sort((a, b) => b.priority - a.priority || b.wrong - a.wrong);
}

/* ---------------- "what to practise next" ---------------- */

/**
 * Up to three next actions, most useful first. Each: { title, why, to, label }.
 * Topic misses come first (they say exactly what to study), then section pacing, then the revision backlog.
 */
export function nextActions({ topics, sectionTrend, revision, revisionDueIds }) {
  const out = [];
  topics.slice(0, 2).forEach((t) => {
    const overdue = t.topicId && revisionDueIds.has(t.topicId);
    out.push({
      title: `Practise ${t.topic} (${t.section})`,
      why: `${t.wrong} wrong and ${t.skipped} skipped across your recent mocks`
        + (t.accuracy != null ? `; ${pct(t.accuracy)} accuracy when attempted` : '')
        + (overdue ? '. Its revision is also due.' : '.'),
      to: `#topic-${encodeURIComponent(`${t.section}-${t.topic}`)}`,
      label: 'Review these questions',
    });
  });
  // section pacing from the last three attempts that include the section
  const pacing = SECTION_KEYS.map((k) => {
    const recent = sectionTrend[k].slice(-3);
    const att = recent.filter((s) => s.accuracy != null);
    if (att.length < 2) return null;
    const acc = att.reduce((t, s) => t + s.accuracy, 0) / att.length;
    const rate = recent.reduce((t, s) => t + (s.attemptRate ?? 0), 0) / recent.length;
    if (acc < 0.6) return { k, acc, rate, kind: 'slow', gap: 0.6 - acc };
    if (acc >= 0.8 && rate < 0.5) return { k, acc, rate, kind: 'more', gap: 0.5 - rate };
    return null;
  }).filter(Boolean).sort((a, b) => b.gap - a.gap)[0];
  if (pacing) {
    out.push(pacing.kind === 'slow'
      ? { title: `Attempt fewer, surer questions in ${pacing.k}`, why: `Accuracy is ${pct(pacing.acc)} over your last mocks, so wrong answers are costing marks. Skip questions you can't settle quickly.`, to: '/mocks?tab=sectional&sec=' + pacing.k, label: `Take a ${pacing.k} sectional` }
      : { title: `Attempt more questions in ${pacing.k}`, why: `Accuracy is ${pct(pacing.acc)} but you attempt only ${pct(pacing.rate)} of questions. You can afford to attempt more.`, to: '/mocks?tab=sectional&sec=' + pacing.k, label: `Take a ${pacing.k} sectional` });
  }
  if (revision.overdue > 0) {
    out.push({ title: `Clear ${revision.overdue} overdue revision${revision.overdue === 1 ? '' : 's'}`, why: `The oldest has been waiting ${revision.oldestDays} day${revision.oldestDays === 1 ? '' : 's'}. Short, regular review keeps finished topics from slipping.`, to: '/revision', label: 'Open revision' });
  }
  return out.slice(0, 3);
}
