// CAT marking for one question, used to show results in reviews (the server does the actual scoring).

/**
 * A question without an answer key is shown in the paper but never scored. Questions served by the API carry
 * `scored` instead of their key (the key arrives only after submit).
 */
export const isScored = (q) => (typeof q.scored === 'boolean' ? q.scored : q.answer !== null && q.answer !== undefined);

export function scoreQuestion(q, ans) {
  if (!isScored(q)) return { result: 'unscored', marks: 0 };
  if (ans === undefined || ans === '' || ans === null) return { result: 'skip', marks: 0 };
  const ok = q.type === 'mcq' ? ans === q.answer : String(ans).trim() === String(q.answer);
  return { result: ok ? 'correct' : 'wrong', marks: ok ? 3 : q.type === 'mcq' ? -1 : 0 };
}
