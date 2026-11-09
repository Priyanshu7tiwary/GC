// CAT marking, shared by the test player (on submit) and the result page (rescoring after a key correction).
import { estPercentile } from './percentile.js';

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

/**
 * Section results and totals for a set of answers.
 * bank: { [section]: question[] }, time: { [section]: seconds }, fullLength: estimate a percentile.
 */
export function tally(bank, answers, time = {}, fullLength = false) {
  const results = {};
  let score = 0, max = 0;
  Object.entries(bank).forEach(([s, qs]) => {
    const st = { correct: 0, wrong: 0, unattempted: 0, unscored: 0, score: 0, time: time[s] || 0 };
    qs.forEach((qq) => {
      const r = scoreQuestion(qq, answers[qq.id]);
      st.score += r.marks;
      st[r.result === 'skip' ? 'unattempted' : r.result]++;
    });
    results[s] = st;
    score += st.score;
    max += qs.filter(isScored).length * 3;
  });
  return { sections: results, score, max, percentile: fullLength && max > 0 ? estPercentile(score / max) : undefined };
}
