// Turns the user's analytics into a ranked list of concrete next actions.
// Pure function of store state — easy to swap for an LLM call later.

import { ALL_TOPICS, SECTIONS } from '../data/syllabus.js';
import { coverageStats, revisionBuckets, streak, today, daysBetween } from './store.jsx';
import { ratioForPercentile } from './percentile.js';

const DAY = 86400000;
const RANK = { high: 0, medium: 1, low: 2 };
const avg = (xs) => (xs.length ? xs.reduce((a, b) => a + b, 0) / xs.length : 0);

// Chapters that carry the most marks in recent CAT papers
const HIGH_YIELD = new Set(['Arithmetic', 'Algebra', 'Reading Comprehension', 'Logical Reasoning']);

const SEC_MAX = { VARC: 72, DILR: 66, QA: 66 };

export function buildSuggestions({ attempts, coverage, revisions, activity, examDate, targetPercentile, targetYear }) {
  const out = [];
  const t0 = today();
  const daysLeft = Math.max(0, daysBetween(t0, examDate));
  const longRunway = daysLeft > 180;
  const fulls = attempts.filter((a) => Object.keys(a.sections).length === 3).sort((a, b) => a.date - b.date);
  const recent = fulls.slice(-4);

  /* ---- 0. Target breakdown: what score the chosen percentile needs ---- */
  const need = Math.round(ratioForPercentile(targetPercentile) * 204);
  const perSec = Object.fromEntries(Object.entries(SEC_MAX).map(([k, m]) => [k, Math.round((need * m) / 204)]));
  if (recent.length) {
    const cur = Object.fromEntries(Object.keys(SEC_MAX).map((k) => [k, avg(recent.map((a) => a.sections[k]?.score ?? 0))]));
    const curTotal = Object.values(cur).reduce((a, b) => a + b, 0);
    const gaps = Object.keys(SEC_MAX).map((k) => ({ k, gap: perSec[k] - cur[k] })).sort((a, b) => b.gap - a.gap);
    const gap = need - curTotal;
    const weeks = Math.max(1, Math.floor(daysLeft / 7));
    out.push({
      id: 'target', category: 'Target', priority: gap <= 0 ? 'low' : gap / weeks > 3 ? 'high' : 'medium',
      title: gap <= 0
        ? `You're scoring at ${targetPercentile}+ level — hold it`
        : `${targetPercentile} %ile in CAT ${targetYear} needs ~${need}/204`,
      body: gap <= 0
        ? `Your recent average (${curTotal.toFixed(0)}) already clears the ~${need} needed. Protect it: keep accuracy high and keep revision current.`
        : `You average ${curTotal.toFixed(0)} — a gap of ${gap.toFixed(0)} marks, ≈ ${(gap / weeks).toFixed(1)} marks/week over ${weeks} weeks. Target split: VARC ${perSec.VARC}, DILR ${perSec.DILR}, QA ${perSec.QA}. Biggest gap is ${gaps[0].k} (${cur[gaps[0].k].toFixed(0)} → ${perSec[gaps[0].k]}).`,
      metric: `gap ${Math.max(0, gap).toFixed(0)} marks`,
      action: gap <= 0 ? { label: 'See analytics', to: '/analytics' } : { label: `${gaps[0].k} sectionals`, to: `/mocks?tab=sectional&sec=${gaps[0].k}` },
    });
  }

  /* ---- 1. Revision backlog ---- */
  const { overdue, due } = revisionBuckets(revisions);
  if (overdue.length + due.length > 0) {
    const worst = overdue[0] && ALL_TOPICS.find((t) => t.id === overdue[0].id);
    out.push({
      id: 'revision', category: 'Revision', priority: overdue.length >= 5 ? 'high' : 'medium',
      title: `Clear ${overdue.length + due.length} pending revisions`,
      body: `${overdue.length} overdue${worst ? ` — oldest is “${worst.name}”, ${-overdue[0].delta} days late` : ''}. Topics left unrevised past their interval are where forgetting sets in fastest.`,
      metric: `≈ ${(overdue.length + due.length) * 15} min`,
      action: { label: 'Open revision', to: '/revision' },
    });
  }

  /* ---- 2. Section accuracy & negatives ---- */
  if (recent.length) {
    const secStats = Object.keys(SECTIONS).map((k) => {
      const list = recent.map((a) => a.sections[k]).filter(Boolean);
      const att = avg(list.map((s) => s.correct + s.wrong));
      const total = avg(list.map((s) => s.correct + s.wrong + s.unattempted)) || 1;
      return {
        k,
        acc: avg(list.map((s) => (s.correct + s.wrong ? s.correct / (s.correct + s.wrong) : 0))) * 100,
        neg: avg(list.map((s) => s.wrong)),
        rate: (att / total) * 100,
      };
    });

    const worstAcc = [...secStats].sort((a, b) => a.acc - b.acc)[0];
    if (worstAcc.acc < 75) {
      out.push({
        id: 'accuracy', category: 'Mocks', priority: worstAcc.acc < 65 ? 'high' : 'medium',
        title: `Fix accuracy in ${worstAcc.k}`,
        body: `Your ${worstAcc.k} accuracy over the last ${recent.length} mocks is ${worstAcc.acc.toFixed(0)}%, costing ~${worstAcc.neg.toFixed(1)} negative marks per mock. Do a timed sectional and only attempt questions you can verify.`,
        metric: `${worstAcc.acc.toFixed(0)}% accuracy`,
        action: { label: `${worstAcc.k} sectionals`, to: `/mocks?tab=sectional&sec=${worstAcc.k}` },
      });
    }

    const lowRate = [...secStats].sort((a, b) => a.rate - b.rate)[0];
    if (lowRate.rate < 55 && lowRate.k !== worstAcc.k) {
      out.push({
        id: 'attempt-rate', category: 'Mocks', priority: 'medium',
        title: `Attempt more in ${lowRate.k}`,
        body: `You attempt only ${lowRate.rate.toFixed(0)}% of ${lowRate.k} questions, with ${lowRate.acc.toFixed(0)}% accuracy. That accuracy supports attempting more — spend the first 5 minutes picking the easiest sets/questions.`,
        metric: `${lowRate.rate.toFixed(0)}% attempted`,
        action: { label: 'Practise selection', to: `/mocks?tab=sectional&sec=${lowRate.k}` },
      });
    }
  }

  /* ---- 3. Score trend ---- */
  if (fulls.length >= 3) {
    const last3 = fulls.slice(-3).map((a) => a.percentile || 0);
    const slope = last3[2] - last3[0];
    if (slope < -2) {
      out.push({
        id: 'trend-down', category: 'Performance', priority: 'high',
        title: 'Your last mocks are trending down',
        body: `Percentile dropped ${Math.abs(slope).toFixed(1)} points over your last 3 full mocks. Pause new mocks for 2 days and analyse the last two attempts question by question before taking another.`,
        metric: `${slope.toFixed(1)} %ile`,
        action: { label: 'Review last mock', to: `/mocks/result/${fulls[fulls.length - 1].id}` },
      });
    }
  }

  /* ---- 4. Mock cadence ---- */
  const lastFull = fulls[fulls.length - 1];
  const sinceLast = lastFull ? daysBetween(lastFull.date, t0) : Infinity;
  const cadenceGap = daysLeft <= 30 ? 4 : longRunway ? 21 : 7;
  if (sinceLast > cadenceGap) {
    out.push({
      id: 'cadence', category: 'Mocks', priority: daysLeft <= 30 ? 'high' : longRunway ? 'low' : 'medium',
      title: lastFull ? `No full mock in ${sinceLast} days` : 'Take your first full mock',
      body: daysLeft <= 30
        ? `With ${daysLeft} days to CAT, aim for 2–3 full mocks a week, each followed by a full analysis session.`
        : longRunway
          ? `CAT ${targetYear} is ${Math.round(daysLeft / 30)} months away — one full mock every 2–3 weeks is enough to benchmark while you build fundamentals.`
          : 'One full mock a week keeps your stamina and section-switching sharp.',
      metric: lastFull ? `last: ${sinceLast}d ago` : '0 mocks',
      action: { label: 'Pick a paper', to: '/mocks?tab=pyq' },
    });
  }

  /* ---- 5. Syllabus pace ---- */
  const cov = coverageStats(coverage);
  const remaining = cov.total - cov.done;
  if (remaining > 0) {
    const perWeek = daysLeft ? (remaining / daysLeft) * 7 : remaining;
    const lowest = Object.keys(SECTIONS)
      .map((k) => ({ k, ...coverageStats(coverage, ALL_TOPICS.filter((t) => t.section === k)) }))
      .sort((a, b) => a.pct - b.pct)[0];
    out.push({
      id: 'coverage', category: 'Coverage', priority: perWeek > 4 ? 'high' : 'medium',
      title: `${remaining} topics left — ${lowest.k} is furthest behind`,
      body: `${lowest.k} is at ${lowest.pct}% coverage. To finish everything before CAT ${targetYear} you need ~${Math.ceil(perWeek)} topics a week${perWeek > 4 ? ' — consider dropping low-yield topics and protecting high-yield ones' : ''}.`,
      metric: `${cov.pct}% covered`,
      action: { label: 'Open coverage', to: '/coverage' },
    });
  }

  /* ---- 6. High-yield topics not started ---- */
  const untouched = ALL_TOPICS.filter((t) => HIGH_YIELD.has(t.chapter) && (coverage[t.id]?.status ?? 0) === 0);
  if (untouched.length) {
    out.push({
      id: 'high-yield', category: 'Coverage', priority: untouched.length >= 3 ? 'high' : 'medium',
      title: `Start ${untouched.length} high-yield topic${untouched.length > 1 ? 's' : ''}`,
      body: `${untouched.slice(0, 3).map((t) => t.name).join(', ')}${untouched.length > 3 ? ` and ${untouched.length - 3} more` : ''} — these chapters carry the most marks in recent papers and you haven't begun them.`,
      metric: untouched[0].section,
      action: { label: 'Plan them', to: '/coverage' },
    });
  }

  /* ---- 7. Weak topics → custom mock ---- */
  const weak = ALL_TOPICS.filter((t) => coverage[t.id]?.status === 1 && (coverage[t.id]?.practiced ?? 0) < 30);
  if (weak.length >= 2) {
    out.push({
      id: 'custom', category: 'Practice', priority: 'low',
      title: 'Generate a weak-area mock',
      body: `${weak.length} in-progress topics have under 30 practice questions (e.g. ${weak.slice(0, 2).map((t) => t.name).join(', ')}). A custom mock weighted toward them builds volume fast.`,
      metric: `${weak.length} topics`,
      action: { label: 'Open generator', to: '/mocks?tab=custom' },
    });
  }

  /* ---- 8. Study consistency ---- */
  const week = (offset) => Array.from({ length: 7 }, (_, i) => activity[t0 - (i + offset) * DAY] || 0).reduce((a, b) => a + b, 0);
  const thisWeek = week(0), lastWeek = week(7);
  const s = streak(activity);
  if (lastWeek && thisWeek < lastWeek * 0.75) {
    out.push({
      id: 'hours', category: 'Habits', priority: 'medium',
      title: 'Study time is down this week',
      body: `${(thisWeek / 60).toFixed(1)} h in the last 7 days vs ${(lastWeek / 60).toFixed(1)} h the week before. Block a fixed daily slot — consistency matters more than long weekend sessions.`,
      metric: `${Math.round((1 - thisWeek / lastWeek) * 100)}% less`,
      action: { label: 'View dashboard', to: '/' },
    });
  } else if (s >= 7) {
    out.push({
      id: 'streak', category: 'Habits', priority: 'low',
      title: `${s}-day streak — keep it going`,
      body: `You've studied ${(thisWeek / 60).toFixed(1)} h this week. Use today's session for one revision block and one timed sectional.`,
      metric: `${s} days`,
      action: { label: 'Revise now', to: '/revision' },
    });
  }

  return out.sort((a, b) => RANK[a.priority] - RANK[b.priority]);
}
