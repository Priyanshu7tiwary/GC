// How much effort the learner is putting in, 0–100, from the last 7 days. Every part is shown on the dashboard, so the
// number is never a black box.
const DAY = 86400000;

export const EFFORT_PARTS = [
  { id: 'time', label: 'Study time', weight: 40 },
  { id: 'days', label: 'Days studied', weight: 25 },
  { id: 'mocks', label: 'Mocks taken', weight: 20 },
  { id: 'revision', label: 'Revision on time', weight: 15 },
];
const WEEK_MINUTES = 21 * 60; // about 3 hours a day is a full tank
const MOCKS_PER_WEEK = 2;

/** Effort for the 7 days ending `endDay` (local midnight). overdue: revision topics overdue now; scheduled: topics in the schedule. */
export function effortFor({ activity, attempts, overdue, scheduled }, endDay) {
  const days = Array.from({ length: 7 }, (_, i) => activity[endDay - i * DAY] || 0);
  const minutes = days.reduce((a, b) => a + b, 0);
  const active = days.filter((m) => m > 0).length;
  const mocks = attempts.filter((a) => a.date > endDay - 6 * DAY && a.date < endDay + DAY).length;
  const parts = {
    time: { ratio: Math.min(1, minutes / WEEK_MINUTES), detail: `${(minutes / 60).toFixed(1)} h of ${WEEK_MINUTES / 60} h` },
    days: { ratio: active / 7, detail: `${active} of 7 days` },
    mocks: { ratio: Math.min(1, mocks / MOCKS_PER_WEEK), detail: `${mocks} of ${MOCKS_PER_WEEK} this week` },
    revision: scheduled === 0
      ? { ratio: null, detail: 'Counts once you complete a topic' } // not part of the score until there is a schedule
      : { ratio: 1 - Math.min(overdue, 8) / 8, detail: overdue ? `${overdue} overdue` : 'Nothing overdue' },
  };
  const counted = EFFORT_PARTS.filter((p) => parts[p.id].ratio != null);
  const weight = counted.reduce((a, p) => a + p.weight, 0);
  const score = Math.round((counted.reduce((a, p) => a + parts[p.id].ratio * p.weight, 0) / weight) * 100);
  return { score, parts };
}

/** One colour scale for every effort meter: red under 35%, amber under 65%, green above. */
export const effortTone = (pct) => (pct < 35 ? 'bad' : pct < 65 ? 'warn' : 'good');

export const effortZone = (s) => ({
  label: s < 35 ? 'Low' : s < 65 ? 'Steady' : s < 85 ? 'Strong' : 'Excellent',
  tone: effortTone(s),
});
