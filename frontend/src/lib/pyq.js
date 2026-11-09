// Previous-year papers are served and scored by the API. What is left here is admin work on their answer keys.
import { api } from './api.js';

export { isScored } from './scoring.js';

/** Admin only (enforced by the server): every question's current key in a paper, as { [questionId]: {answer, corrected} }. */
export async function loadAnswerKeys(mockId) {
  const keys = await api(`/api/admin/mocks/${encodeURIComponent(mockId)}/answer-keys`);
  return Object.fromEntries(keys.map((k) => [k.questionId, { answer: k.answer, corrected: k.corrected }]));
}

/** Admin only (enforced by the server): replace a question's answer key. Resolves to { questionId, answer }. */
export const saveAnswerKey = (questionId, answer) =>
  api(`/api/admin/questions/${encodeURIComponent(questionId)}/answer`, { method: 'PUT', body: { answer: String(answer) } });

/** A key as the app's question shape stores it: option index for MCQ, text for TITA, null when there is none. */
export const toKey = (type, answer) => (answer == null ? null : type === 'mcq' ? Number(answer) : String(answer));
