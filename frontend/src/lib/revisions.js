// The revision schedule lives on the server, which owns due dates, stages and the rules for moving them
// (only due reviews advance, at most once a day; dates are calendar dates in the exam time zone).
import { api } from './api.js';

const RATING = { hard: 'HARD', ok: 'GOOD', good: 'GOOD', easy: 'EASY' };

/** 'YYYY-MM-DD' → local-midnight epoch, for display only (scheduling never uses the browser's clock). */
const day = (iso) => {
  const [y, m, d] = iso.split('-').map(Number);
  return new Date(y, m - 1, d).getTime();
};

/** A server RevisionView in the shape the app's screens use. */
export const toEntry = (v) => ({
  stage: v.stage,
  intervalDays: v.intervalDays,
  enrolled: day(v.enrolledOn),
  last: day(v.lastRevisedOn),
  next: day(v.nextDueOn),
  delta: v.daysUntilDue,
  reviewCount: v.reviewCount,
  paused: v.paused,
  canAdvanceToday: v.canAdvanceToday,
});

export const toMap = (items) => Object.fromEntries(items.map((v) => [v.topicId, toEntry(v)]));

export const fetchBoard = () => api('/api/revisions');
export const enrollTopic = (id) => api(`/api/revisions/${encodeURIComponent(id)}`, { method: 'PUT' });
export const enrollTopics = (ids) => api('/api/revisions', { method: 'PUT', body: { topicIds: ids } });
export const pauseTopic = (id) => api(`/api/revisions/${encodeURIComponent(id)}/pause`, { method: 'POST' });
export const resetTopic = (id) => api(`/api/revisions/${encodeURIComponent(id)}/reset`, { method: 'POST' });
export const topicHistory = (id) => api(`/api/revisions/${encodeURIComponent(id)}/history`);

/** Records a review. The request id makes a retry safe: the server never applies the same review twice. */
export async function reviewTopic(id, rating) {
  const requestId = (crypto.randomUUID?.() || `${Date.now()}-${Math.random().toString(36).slice(2)}`).replace(/[^A-Za-z0-9-]/g, '');
  const send = () => api(`/api/revisions/${encodeURIComponent(id)}/reviews`, { method: 'POST', body: { rating: RATING[rating] || rating, requestId } });
  try {
    return await send();
  } catch (e) {
    if (e.status === 0) return send(); // network blip: retry once with the same id
    throw e;
  }
}
