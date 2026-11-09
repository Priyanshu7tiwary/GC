// The AI study plan: the learner's progress is sent to the server, which asks an AI model for topics to cover and
// tests to attempt. The server checks the answer against real topics and tests and builds the links, so everything
// returned here exists in the app.
import { api } from './api.js';
import { revisionBuckets, today } from './store.jsx';

const DAY = 86400000;

/** Only ids and numbers leave the browser: no name, no email. */
export function buildContext(state) {
  const t0 = today();
  const minutes = (from, to) => Object.entries(state.activity || {})
    .filter(([d]) => Number(d) > t0 - to * DAY && Number(d) <= t0 - from * DAY)
    .reduce((a, [, m]) => a + m, 0);
  const recent = [...state.attempts].sort((a, b) => a.date - b.date).slice(-6);
  return {
    targetPercentile: state.targetPercentile,
    targetYear: state.targetYear,
    daysLeft: Math.max(0, Math.round((state.examDate - t0) / DAY)),
    topicStatus: Object.fromEntries(Object.entries(state.coverage).map(([id, c]) => [id, c.status ?? 0])),
    recentAttempts: recent.map((a) => ({
      mockId: String(a.mockId || '').slice(0, 80),
      title: String(a.title || '').slice(0, 160),
      score: Math.round(a.score),
      max: Math.round(a.max),
      sections: Object.fromEntries(Object.entries(a.sections).map(([k, s]) => [k, {
        correct: s.correct, wrong: s.wrong, unattempted: s.unattempted, score: Math.round(s.score), minutes: Math.round((s.time || 0) / 60),
      }])),
    })),
    attemptedMockIds: [...new Set(state.attempts.map((a) => a.mockId).filter(Boolean))].slice(0, 300),
    overdueTopicIds: revisionBuckets(state.revisions).overdue.map((r) => r.id).slice(0, 80),
    minutesLast7Days: Math.round(minutes(0, 7)),
    minutesPrevious7Days: Math.round(minutes(7, 14)),
  };
}

/** The latest plan and the weekly allowance. Looking costs nothing.
 *  { available, plan: {summary, topics, tests, model, generatedAt} | null, quota: {limit, used, remaining, nextAvailableAt} } */
export const fetchAiState = () => api('/api/suggestions/ai', { quiet: true });

/** Generates a new plan. Uses one of the learner's weekly allowance; resolves to the same shape as fetchAiState. */
export const generateAiPlan = (state) => api('/api/suggestions/ai', { method: 'POST', body: buildContext(state), quiet: true });

/** 'in 3 days', 'in 5 hours', 'in under an hour' */
export function untilText(iso, now = Date.now()) {
  const ms = new Date(iso).getTime() - now;
  if (ms <= 0) return 'now';
  const hours = Math.ceil(ms / 3600000);
  if (hours >= 24) { const d = Math.ceil(ms / 86400000); return `in ${d} day${d === 1 ? '' : 's'}`; }
  return hours <= 1 ? 'in under an hour' : `in ${hours} hours`;
}
