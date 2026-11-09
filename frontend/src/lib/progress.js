// A learner's progress lives on the server: coverage, targets, mock attempts, study time and the revision backlog.
// The browser only displays it, so it is the same on every device.
import { api } from './api.js';

const STATUS = { NOT_STARTED: 0, IN_PROGRESS: 1, COMPLETED: 2 };
const STATUS_NAME = ['NOT_STARTED', 'IN_PROGRESS', 'COMPLETED'];

/** 'YYYY-MM-DD' → local-midnight epoch (display only). */
export const dayOf = (iso) => {
  const [y, m, d] = iso.split('-').map(Number);
  return new Date(y, m - 1, d).getTime();
};

/** { [topicId]: status 0 | 1 | 2 } */
export async function fetchCoverage() {
  const view = await api('/api/coverage');
  return Object.fromEntries(view.topics.map((t) => [t.topicId, STATUS[t.status] ?? 0]));
}

export const saveCoverageStatus = (topicId, status) =>
  api(`/api/coverage/${encodeURIComponent(topicId)}`, { method: 'PATCH', body: { status: STATUS_NAME[status] } });

/** A server attempt summary in the shape the app's screens use. */
export const toAttempt = (s) => ({
  id: s.id,
  mockId: s.mockId,
  title: s.title,
  date: Date.parse(s.submittedAt),
  score: s.score,
  max: s.maxScore,
  percentile: s.percentile ?? undefined,
  fullLength: s.fullLength,
  timeSpent: s.timeSpentSeconds,
  sections: Object.fromEntries(Object.entries(s.sections).map(([k, r]) => [k, {
    correct: r.correct, wrong: r.wrong, unattempted: r.unattempted, score: r.score, time: r.timeSeconds,
  }])),
});

/** Every attempt, oldest first. */
export const fetchAttempts = async () => (await api('/api/attempts')).map(toAttempt);

/** { [local-midnight epoch]: minutes } for the last `days` days. */
export async function fetchActivity(days = 120) {
  const view = await api(`/api/activity?days=${days}`);
  return Object.fromEntries(view.days.filter((d) => d.minutes > 0).map((d) => [dayOf(d.day), d.minutes]));
}

/** { [local-midnight epoch]: { overdue, due } } */
export async function fetchBacklog(days = 180) {
  const rows = await api(`/api/revisions/backlog?days=${days}`);
  return Object.fromEntries(rows.map((r) => [dayOf(r.day), { overdue: r.overdue, due: r.due }]));
}

/** patch: { targetPercentile?, targetYear? }. Resolves to the updated profile. */
export const saveProfile = (patch) => api('/api/me', { method: 'PATCH', body: patch });
