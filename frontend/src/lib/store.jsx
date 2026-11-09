import { createContext, useContext, useEffect, useMemo, useRef, useState, useCallback } from 'react';
import { ALL_TOPICS } from '../data/syllabus.js';
import { practiceSummary } from './practice.js';
import { enrollTopic, fetchBoard, pauseTopic, resetTopic, reviewTopic, toEntry, toMap } from './revisions.js';
import { fetchActivity, fetchAttempts, fetchBacklog, fetchCoverage, saveCoverageStatus, saveProfile } from './progress.js';
import { useAuth } from './auth.jsx';

const DAY = 86400000;
export const INTERVALS = [1, 3, 7, 15, 30, 60]; // spaced-repetition ladder (days)

export const today = () => {
  const d = new Date();
  d.setHours(0, 0, 0, 0);
  return d.getTime();
};
export const daysBetween = (a, b) => Math.round((b - a) / DAY);

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

// Everything here is loaded from the server, which owns every learner's progress. Nothing is kept in the browser,
// so a learner sees the same data on every device, and two people sharing a browser never see each other's.
function empty(user) {
  return {
    examDate: user?.examDate ? dayOfIso(user.examDate) : catDate(user?.targetYear || new Date().getFullYear()),
    targetYear: user?.targetYear || new Date().getFullYear(),
    targetPercentile: user?.targetPercentile || 99,
    coverage: {},      // topicId -> { status, practiced }
    revisions: {},     // topicId -> schedule entry
    attempts: [],      // oldest first
    activity: {},      // local-midnight epoch -> minutes
    revisionLog: {},   // local-midnight epoch -> { overdue, due }
    customMocks: [],
    loaded: false,
    loadError: null,
  };
}
const dayOfIso = (iso) => { const [y, m, d] = iso.split('-').map(Number); return new Date(y, m - 1, d).getTime(); };

/**
 * Progress used to be kept in the browser. On first load after the move to the server, topics this browser had
 * marked in progress or complete are saved to the server (the server's own status wins), then the old copy is removed.
 * Old mock attempts can't be carried over: they were scored in the browser and the server never saw the answers.
 */
async function migrateLocalCoverage(userId) {
  const key = `catalyst-state-v3:${userId}`;
  let old;
  try { old = JSON.parse(localStorage.getItem(key) || 'null'); } catch { old = null; }
  if (!old?.coverage) return;
  const server = await fetchCoverage();
  for (const [id, c] of Object.entries(old.coverage)) {
    if ((c?.status ?? 0) > 0 && (server[id] ?? 0) === 0) await saveCoverageStatus(id, c.status).catch(() => {});
  }
  try { localStorage.removeItem(key); } catch { /* ignore */ }
}

const Ctx = createContext(null);

export function StoreProvider({ userId, children }) {
  const { user } = useAuth();
  const [state, setState] = useState(() => empty(user));
  const stateRef = useRef(state);
  stateRef.current = state;
  const lastStatuses = useRef(null); // the coverage and schedule as last fetched from the server
  const lastBoard = useRef(null);
  const signedIn = userId !== 'anonymous';

  // ---- progress: coverage, mock attempts, study time, backlog history ----
  const refreshProgress = useCallback(async () => {
    if (!signedIn) return;
    const [attempts, activity, revisionLog] = await Promise.all([fetchAttempts(), fetchActivity(), fetchBacklog()]);
    setState((s) => ({ ...s, attempts, activity, revisionLog }));
  }, [signedIn]);

  const refreshCoverage = useCallback(async () => {
    if (!signedIn) return;
    const [statuses, summary] = await Promise.all([fetchCoverage(), practiceSummary().catch(() => ({}))]);
    lastStatuses.current = statuses;
    setState((s) => ({
      ...s,
      coverage: Object.fromEntries(ALL_TOPICS.map((t) => [t.id, { status: statuses[t.id] ?? 0, practiced: summary[t.id]?.solved ?? 0 }])),
    }));
  }, [signedIn]);

  // ---- revision schedule ----
  const setRevision = useCallback((id, view) => setState((s) => ({ ...s, revisions: { ...s.revisions, [id]: toEntry(view) } })), []);

  const refreshRevisions = useCallback(async () => {
    if (!signedIn) return;
    try {
      const board = await fetchBoard();
      lastBoard.current = board.items;
      setState((s) => ({ ...s, revisions: toMap(board.items), revisionsError: null }));
    } catch (e) {
      setState((s) => ({ ...s, revisionsError: e.message }));
    }
  }, [signedIn]);

  // On sign-in everything is loaded; it is reloaded when the tab regains focus (another device may have changed it)
  // and every 30 minutes so a new day's due topics appear.
  useEffect(() => {
    if (!signedIn) return undefined;
    let live = true;
    const loadAll = async () => {
      try {
        await migrateLocalCoverage(userId);
        await Promise.all([refreshCoverage(), refreshProgress(), refreshRevisions()]);
        // Only completed topics belong in the tracker: pause any active ones whose topic isn't complete (left over
        // from older versions), keeping their history. Uses the data just fetched, not React state, which may not
        // have re-rendered yet.
        const statuses = lastStatuses.current, items = lastBoard.current;
        const stray = statuses && items ? items.filter((v) => !v.paused && (statuses[v.topicId] ?? 0) !== 2).map((v) => v.topicId) : [];
        for (const id of stray) await pauseTopic(id);
        if (stray.length) await refreshRevisions();
        if (live) setState((s) => ({ ...s, loaded: true, loadError: null }));
      } catch (e) {
        if (live) setState((s) => ({ ...s, loaded: true, loadError: e.message }));
      }
    };
    loadAll();
    const onFocus = () => { refreshCoverage().catch(() => {}); refreshProgress().catch(() => {}); refreshRevisions(); };
    window.addEventListener('focus', onFocus);
    const timer = setInterval(refreshRevisions, 30 * 60 * 1000);
    return () => { live = false; window.removeEventListener('focus', onFocus); clearInterval(timer); };
  }, [signedIn, userId, refreshCoverage, refreshProgress, refreshRevisions]);

  /** Solved-question counters come from the practice bank. */
  const refreshPractice = useCallback(() => refreshCoverage().catch(() => {}), [refreshCoverage]);

  /** patch: { targetPercentile?, targetYear?, examDate? }. Saved to the learner's profile. */
  const updateSettings = useCallback(async (patch) => {
    setState((s) => ({ ...s, ...patch }));
    const body = {};
    if (patch.targetPercentile != null) body.targetPercentile = patch.targetPercentile;
    if (patch.targetYear != null) body.targetYear = patch.targetYear;
    if (patch.examDate != null) { const d = new Date(patch.examDate); body.examDate = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`; }
    try {
      const p = await saveProfile(body);
      setState((s) => ({ ...s, targetPercentile: p.targetPercentile, targetYear: p.targetYear, examDate: dayOfIso(p.examDate) }));
    } catch { /* keep the local choice; it is retried the next time it changes */ }
  }, []);

  // Marking a topic complete enrols it in revision on the server; reopening it pauses its schedule.
  const setCoverage = useCallback((id, patch) => {
    const prev = stateRef.current.coverage[id] || { status: 0, practiced: 0 };
    setState((s) => ({ ...s, coverage: { ...s.coverage, [id]: { ...prev, ...patch } } }));
    if (patch.status != null && patch.status !== prev.status) {
      saveCoverageStatus(id, patch.status).then(() => refreshRevisions(), () => {
        setState((s) => ({ ...s, coverage: { ...s.coverage, [id]: prev } })); // undo: the server didn't take it
      });
    }
  }, [refreshRevisions]);

  /** Records a Hard / Good / Easy review (rating: 'hard' | 'ok' | 'easy'). Resolves to { outcome, message }. */
  const reviseTopic = useCallback(async (id, rating) => {
    const result = await reviewTopic(id, rating);
    setState((s) => ({ ...s, revisions: { ...s.revisions, [id]: toEntry(result.item) } }));
    refreshProgress().catch(() => {}); // the server logged the study time
    return result;
  }, [refreshProgress]);

  const scheduleTopic = useCallback(async (id) => setRevision(id, await enrollTopic(id)), [setRevision]);
  const restartTopic = useCallback(async (id) => setRevision(id, await resetTopic(id)), [setRevision]);

  /** After a mock is submitted (the server has scored it and logged the time). */
  const addAttempt = useCallback((attempt) => {
    setState((s) => ({ ...s, attempts: [...s.attempts.filter((a) => a.id !== attempt.id), attempt] }));
    refreshProgress().catch(() => {});
  }, [refreshProgress]);

  // After an admin corrects a key, the server rescores the attempt; this keeps the list in step.
  const updateAttempt = useCallback((id, patch) =>
    setState((s) => ({ ...s, attempts: s.attempts.map((a) => (a.id === id ? { ...a, ...patch } : a)) })), []);

  // Generated mocks
  const addCustomMock = useCallback((m) => setState((s) => ({ ...s, customMocks: [m, ...s.customMocks.filter((x) => x.id !== m.id)] })), []);
  const removeCustomMock = useCallback((id) => setState((s) => ({ ...s, customMocks: s.customMocks.filter((m) => m.id !== id) })), []);
  const syncServerMocks = useCallback((list) => setState((s) => ({ ...s, customMocks: list })), []);

  const value = useMemo(
    () => ({ ...state, revisions: coveredOnly(state.revisions, state.coverage), updateSettings, setCoverage, reviseTopic, scheduleTopic, restartTopic, refreshRevisions, refreshPractice, refreshProgress, addAttempt, updateAttempt, addCustomMock, removeCustomMock, syncServerMocks }),
    [state, updateSettings, setCoverage, reviseTopic, scheduleTopic, restartTopic, refreshRevisions, refreshPractice, refreshProgress, addAttempt, updateAttempt, addCustomMock, removeCustomMock, syncServerMocks],
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
