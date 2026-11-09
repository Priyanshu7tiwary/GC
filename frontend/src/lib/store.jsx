import { createContext, useContext, useEffect, useMemo, useRef, useState, useCallback } from 'react';
import { ALL_TOPICS } from '../data/syllabus.js';
import { practiceSummary } from './practice.js';
import { enrollTopic, enrollTopics, fetchBoard, pauseTopic, resetTopic, reviewTopic, toEntry, toMap } from './revisions.js';

const KEY = 'catalyst-state-v3';
try { localStorage.removeItem(KEY); } catch { /* ignore */ }
const DAY = 86400000;
export const INTERVALS = [1, 3, 7, 15, 30, 60]; // spaced-repetition ladder (days)

export const today = () => {
  const d = new Date();
  d.setHours(0, 0, 0, 0);
  return d.getTime();
};
export const daysBetween = (a, b) => Math.round((b - a) / DAY);

const STATE_VERSION = 4; // 4: new accounts start empty (earlier versions were pre-filled with demo data)

// A new account starts with nothing: no coverage, attempts or activity. All of it builds up from what this user does.
function fresh() {
  return {
    v: STATE_VERSION,
    examDate: catDate(2026),
    targetYear: 2026,
    targetPercentile: 99,
    coverage: {},
    revisions: {}, // filled from the server (/api/revisions)
    attempts: [],
    activity: {},
    customMocks: [],
  };
}

/** The revision tracker only covers topics currently marked complete; uncovered topics never appear in it. */
export function coveredOnly(revisions, coverage) {
  return Object.fromEntries(Object.entries(revisions).filter(([id]) => coverage[id]?.status === 2));
}

// CAT is typically held on the last Sunday of November
export function catDate(year) {
  const d = new Date(year, 10, 30);
  d.setDate(30 - d.getDay());
  return d.getTime();
}

function load(key) {
  try {
    const raw = localStorage.getItem(key);
    if (raw) {
      const st = JSON.parse(raw);
      const base = { customMocks: [], targetYear: new Date(st.examDate).getFullYear(), ...st, revisions: {} };
      if (st.v >= STATE_VERSION) return base;
      // Saved by an older version that pre-filled demo coverage, mock attempts and study activity. Drop that made-up
      // data (and anything built on it) and keep only what this user really did.
      return { ...base, v: STATE_VERSION, coverage: {}, activity: {}, attempts: (st.attempts || []).filter((a) => !String(a.id).startsWith('seed-')) };
    }
  } catch {
    /* ignore */
  }
  return fresh();
}

const Ctx = createContext(null);

export function StoreProvider({ userId, children }) {
  const storageKey = `${KEY}:${userId}`;
  const [state, setState] = useState(() => load(storageKey));
  const stateRef = useRef(state);
  stateRef.current = state;

  useEffect(() => {
    try {
      if (userId !== 'anonymous') localStorage.setItem(storageKey, JSON.stringify({ ...state, revisions: undefined }));
    } catch {
      /* ignore */
    }
  }, [state, storageKey, userId]);

  // One snapshot a day of the revision backlog, so Analytics can show whether overdue topics are shrinking.
  // Older history can't be reconstructed, so the trend starts the first day this runs.
  useEffect(() => {
    const { overdue, due } = revisionBuckets(coveredOnly(state.revisions, state.coverage));
    const t = today();
    const cur = state.revisionLog?.[t];
    if (cur && cur.overdue === overdue.length && cur.due === due.length) return;
    setState((s) => {
      const log = { ...(s.revisionLog || {}), [t]: { overdue: overdue.length, due: due.length } };
      const keep = Object.keys(log).map(Number).sort((a, b) => b - a).slice(0, 180);
      return { ...s, revisionLog: Object.fromEntries(keep.map((k) => [k, log[k]])) };
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [state.revisions, state.coverage]);

  // ---- revision schedule: loaded from and changed on the server ----
  const setRevision = useCallback((id, view) => setState((s) => ({ ...s, revisions: { ...s.revisions, [id]: toEntry(view) } })), []);

  const refreshRevisions = useCallback(async () => {
    if (userId === 'anonymous') return;
    try {
      const board = await fetchBoard();
      setState((s) => ({ ...s, revisions: toMap(board.items), revisionsError: null }));
    } catch (e) {
      setState((s) => ({ ...s, revisionsError: e.message }));
    }
  }, [userId]);

  // On sign-in: topics already marked learned in this browser are enrolled (idempotent), then the server's schedule
  // is loaded. It is reloaded when the tab regains focus so a new day's due topics appear.
  useEffect(() => {
    if (userId === 'anonymous') return undefined;
    let live = true;
    (async () => {
      try {
        const board = await fetchBoard();
        const onServer = new Set(board.items.map((v) => v.topicId));
        const learned = Object.entries(state.coverage).filter(([id, c]) => c.status === 2 && !onServer.has(id)).map(([id]) => id);
        for (let i = 0; i < learned.length; i += 200) await enrollTopics(learned.slice(i, i + 200));
        // Only topics marked complete belong in the tracker. Any the server still holds as active for a topic that
        // isn't complete (e.g. left over from older versions) are paused, keeping their history.
        const cov = stateRef.current.coverage;
        const stray = board.items.filter((v) => !v.paused && cov[v.topicId]?.status !== 2).map((v) => v.topicId);
        for (const id of stray) await pauseTopic(id);
        const fresh = learned.length || stray.length ? await fetchBoard() : board;
        if (live) setState((s) => ({ ...s, revisions: toMap(fresh.items), revisionsError: null }));
      } catch (e) {
        if (live) setState((s) => ({ ...s, revisionsError: e.message }));
      }
    })();
    const onFocus = () => refreshRevisions();
    window.addEventListener('focus', onFocus);
    const timer = setInterval(refreshRevisions, 30 * 60 * 1000);
    return () => { live = false; window.removeEventListener('focus', onFocus); clearInterval(timer); };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [userId]);

  // ---- practised-question counters: the number of bank questions marked solved, kept on the server ----
  const applyPractice = useCallback((summary) => setState((s) => ({
    ...s,
    coverage: Object.fromEntries(ALL_TOPICS.map((t) => {
      const c = s.coverage[t.id] || { status: 0, practiced: 0 };
      return [t.id, { ...c, practiced: summary[t.id]?.solved ?? 0 }];
    })),
  })), []);

  const refreshPractice = useCallback(async () => {
    if (userId === 'anonymous') return;
    try { applyPractice(await practiceSummary()); } catch { /* keep the last known counts */ }
  }, [userId, applyPractice]);

  useEffect(() => {
    if (userId === 'anonymous') return undefined;
    refreshPractice();
    window.addEventListener('focus', refreshPractice);
    return () => window.removeEventListener('focus', refreshPractice);
  }, [userId, refreshPractice]);

  const logStudy = (s, minutes) => {
    const t = today();
    return { ...s.activity, [t]: (s.activity[t] || 0) + minutes };
  };

  const resetDemo = useCallback(() => setState(() => fresh()), []);
  const updateSettings = useCallback((patch) => setState((s) => ({ ...s, ...patch })), []);

  // Marking a topic learned enrols it on the server (first review tomorrow; idempotent, never resets progress).
  // Reopening it pauses the schedule, keeping its stage and history.
  const setCoverage = useCallback((id, patch) => {
    const cur = stateRef.current;
    const prev = cur.coverage[id] || { status: 0, practiced: 0 };
    const nextStatus = patch.status ?? prev.status;
    const change = nextStatus === 2 && prev.status !== 2 ? 'enroll'
      : nextStatus !== 2 && prev.status === 2 && cur.revisions[id] ? 'pause' : null;
    setState((s) => ({ ...s, coverage: { ...s.coverage, [id]: { ...(s.coverage[id] || { status: 0, practiced: 0 }), ...patch } } }));
    if (change === 'enroll') enrollTopic(id).then((v) => setRevision(id, v), () => refreshRevisions());
    if (change === 'pause') pauseTopic(id).then((v) => setRevision(id, v), () => refreshRevisions());
  }, [setRevision, refreshRevisions]);

  /** Records a Hard / Good / Easy review (rating: 'hard' | 'ok' | 'easy'). Resolves to { outcome, message }. */
  const reviseTopic = useCallback(async (id, rating) => {
    const result = await reviewTopic(id, rating);
    setState((s) => ({
      ...s,
      revisions: { ...s.revisions, [id]: toEntry(result.item) },
      activity: result.outcome === 'DUPLICATE_REQUEST' ? s.activity : logStudy(s, 15),
    }));
    return result;
  }, []);

  /** Adds a topic to the schedule (or resumes a paused one). */
  const scheduleTopic = useCallback(async (id) => setRevision(id, await enrollTopic(id)), [setRevision]);

  /** Explicitly restarts a topic at the first rung (history is kept on the server). */
  const restartTopic = useCallback(async (id) => setRevision(id, await resetTopic(id)), [setRevision]);

  /** Adds study minutes to today (e.g. for practice questions solved). */
  const logMinutes = useCallback((minutes) => setState((s) => ({ ...s, activity: logStudy(s, minutes) })), []);

  const addAttempt = useCallback((attempt) =>
    setState((s) => ({ ...s, attempts: [...s.attempts, attempt], activity: logStudy(s, Math.round(attempt.timeSpent / 60) || 1) })), []);

  // Used after an admin corrects an answer key, to rescore an attempt with the new key.
  const updateAttempt = useCallback((id, patch) =>
    setState((s) => ({ ...s, attempts: s.attempts.map((a) => (a.id === id ? { ...a, ...patch } : a)) })), []);
  const addCustomMock = useCallback((m) => setState((s) => ({ ...s, customMocks: [m, ...s.customMocks] })), []);
  const removeCustomMock = useCallback((id) => setState((s) => ({ ...s, customMocks: s.customMocks.filter((m) => m.id !== id) })), []);
  // Generated mocks live on the server; keep the local list in step with it (older browser-only mocks stay).
  const syncServerMocks = useCallback((list) => setState((s) => ({ ...s, customMocks: [...list, ...s.customMocks.filter((m) => m.source !== 'server')] })), []);

  const value = useMemo(
    () => ({ ...state, revisions: coveredOnly(state.revisions, state.coverage), resetDemo, updateSettings, setCoverage, reviseTopic, scheduleTopic, restartTopic, refreshRevisions, refreshPractice, logMinutes, addAttempt, updateAttempt, addCustomMock, removeCustomMock, syncServerMocks }),
    [state, resetDemo, updateSettings, setCoverage, reviseTopic, scheduleTopic, restartTopic, refreshRevisions, refreshPractice, logMinutes, addAttempt, updateAttempt, addCustomMock, removeCustomMock, syncServerMocks],
  );
  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

export const useStore = () => useContext(Ctx);

// ---------- derived helpers ----------
export function coverageStats(coverage, topics = ALL_TOPICS) {
  const total = topics.length;
  let done = 0, prog = 0;
  topics.forEach((t) => {
    const st = coverage[t.id]?.status || 0;
    if (st === 2) done++;
    else if (st === 1) prog++;
  });
  return { total, done, prog, pct: total ? Math.round(((done + prog * 0.5) / total) * 100) : 0 };
}

/** Splits the schedule into overdue / due today / upcoming using the server's day counts. Paused topics are left out. */
export function revisionBuckets(revisions) {
  const overdue = [], due = [], upcoming = [];
  Object.entries(revisions).forEach(([id, r]) => {
    if (r.paused) return;
    const d = r.delta ?? daysBetween(today(), r.next);
    if (d < 0) overdue.push({ id, ...r, delta: d });
    else if (d === 0) due.push({ id, ...r, delta: d });
    else upcoming.push({ id, ...r, delta: d });
  });
  const byNext = (a, b) => a.delta - b.delta;
  return { overdue: overdue.sort(byNext), due: due.sort(byNext), upcoming: upcoming.sort(byNext) };
}

export function streak(activity) {
  let n = 0;
  let d = today();
  if (!activity[d]) d -= DAY; // today not logged yet doesn't break the streak
  while (activity[d] > 0) {
    n++;
    d -= DAY;
  }
  return n;
}
