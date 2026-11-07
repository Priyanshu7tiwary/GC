import { createContext, useContext, useEffect, useMemo, useState, useCallback } from 'react';
import { ALL_TOPICS } from '../data/syllabus.js';
import { estPercentile } from './percentile.js';

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

// Deterministic pseudo-random so the demo data looks the same on every fresh load
function rng(seed) {
  let s = seed;
  return () => ((s = (s * 16807) % 2147483647) - 1) / 2147483646;
}

function seed() {
  const r = rng(42);
  const t = today();
  const coverage = {};
  const revisions = {};
  ALL_TOPICS.forEach((tp) => {
    const x = r();
    const status = x < 0.48 ? 2 : x < 0.72 ? 1 : 0; // 0 not started, 1 in progress, 2 done
    coverage[tp.id] = { status, practiced: status === 0 ? 0 : Math.floor(r() * 80) + (status === 2 ? 40 : 5) };
    if (status === 2) {
      const stage = Math.floor(r() * 4);
      const last = t - Math.floor(r() * 10) * DAY;
      const next = last + INTERVALS[stage] * DAY;
      revisions[tp.id] = {
        stage,
        last,
        next,
        history: Array.from({ length: stage + 1 }, (_, k) => last - k * INTERVALS[Math.max(0, stage - k)] * DAY),
      };
    }
  });

  // Past mock attempts with an upward trend
  const attempts = [
    [1, 16, 13, 8, 9], [2, 21, 11, 6, 9], [3, 26, 10, 5, 7],
    [4, 30, 8, 5, 6], [5, 41, 7, 4, 5], [6, 48, 5, 3, 4],
  ].map(([n, ago, v, d, q]) => {
    const score = v * 3 - Math.round(v / 4) + d * 3 - Math.round(d / 3) + q * 3 - Math.round(q / 3);
    return {
      id: `seed-${n}`,
      mockId: `pyq-${2025 - n}-1`,
      title: `CAT ${2025 - n} · Slot 1`,
      date: t - ago * DAY,
      score,
      max: 204,
      percentile: estPercentile(score / 204),
      sections: {
        VARC: { correct: v, wrong: Math.round(v / 4), unattempted: 24 - v - Math.round(v / 4), score: v * 3 - Math.round(v / 4) },
        DILR: { correct: d, wrong: Math.round(d / 3), unattempted: 22 - d - Math.round(d / 3), score: d * 3 - Math.round(d / 3) },
        QA: { correct: q, wrong: Math.round(q / 3), unattempted: 22 - q - Math.round(q / 3), score: q * 3 - Math.round(q / 3) },
      },
    };
  }).reverse();

  // Study activity per day for the last 16 weeks (minutes)
  const activity = {};
  for (let i = 0; i < 112; i++) {
    const v = r();
    activity[t - i * DAY] = i < 9 ? 60 + Math.floor(v * 180) : v < 0.2 ? 0 : Math.floor(v * 240);
  }

  return {
    examDate: catDate(2026),
    targetYear: 2026,
    targetPercentile: 99,
    coverage,
    revisions,
    attempts,
    activity,
    customMocks: [],
  };
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
      return { customMocks: [], targetYear: new Date(st.examDate).getFullYear(), ...st };
    }
  } catch {
    /* ignore */
  }
  return seed();
}

const Ctx = createContext(null);

export function StoreProvider({ userId, children }) {
  const storageKey = `${KEY}:${userId}`;
  const [state, setState] = useState(() => load(storageKey));

  useEffect(() => {
    try {
      if (userId !== 'anonymous') localStorage.setItem(storageKey, JSON.stringify(state));
    } catch {
      /* ignore */
    }
  }, [state, storageKey, userId]);

  const logStudy = (s, minutes) => {
    const t = today();
    return { ...s.activity, [t]: (s.activity[t] || 0) + minutes };
  };

  const resetDemo = useCallback(() => setState(() => seed()), []);
  const updateSettings = useCallback((patch) => setState((s) => ({ ...s, ...patch })), []);

  const setCoverage = useCallback((id, patch) =>
    setState((s) => {
      const prev = s.coverage[id] || { status: 0, practiced: 0 };
      const next = { ...prev, ...patch };
      const revisions = { ...s.revisions };
      // Completing a topic enrols it into the revision schedule
      if (next.status === 2 && !revisions[id]) {
        const t = today();
        revisions[id] = { stage: 0, last: t, next: t + INTERVALS[0] * DAY, history: [t] };
      }
      if (next.status !== 2 && revisions[id] && prev.status === 2) delete revisions[id];
      return { ...s, coverage: { ...s.coverage, [id]: next }, revisions };
    }), []);

  // rating: 'hard' | 'ok' | 'easy'
  const reviseTopic = useCallback((id, rating) =>
    setState((s) => {
      const t = today();
      const cur = s.revisions[id] || { stage: 0, history: [] };
      const stage = rating === 'hard' ? 0 : rating === 'easy' ? Math.min(cur.stage + 2, INTERVALS.length - 1) : Math.min(cur.stage + 1, INTERVALS.length - 1);
      return {
        ...s,
        revisions: { ...s.revisions, [id]: { stage, last: t, next: t + INTERVALS[stage] * DAY, history: [t, ...cur.history] } },
        activity: logStudy(s, 15),
      };
    }), []);

  const addAttempt = useCallback((attempt) =>
    setState((s) => ({ ...s, attempts: [...s.attempts, attempt], activity: logStudy(s, Math.round(attempt.timeSpent / 60) || 1) })), []);

  const addCustomMock = useCallback((m) => setState((s) => ({ ...s, customMocks: [m, ...s.customMocks] })), []);
  const removeCustomMock = useCallback((id) => setState((s) => ({ ...s, customMocks: s.customMocks.filter((m) => m.id !== id) })), []);

  const value = useMemo(
    () => ({ ...state, resetDemo, updateSettings, setCoverage, reviseTopic, addAttempt, addCustomMock, removeCustomMock }),
    [state, resetDemo, updateSettings, setCoverage, reviseTopic, addAttempt, addCustomMock, removeCustomMock],
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

export function revisionBuckets(revisions) {
  const t = today();
  const overdue = [], due = [], upcoming = [];
  Object.entries(revisions).forEach(([id, r]) => {
    const d = daysBetween(t, r.next);
    if (d < 0) overdue.push({ id, ...r, delta: d });
    else if (d === 0) due.push({ id, ...r, delta: d });
    else upcoming.push({ id, ...r, delta: d });
  });
  const byNext = (a, b) => a.next - b.next;
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
