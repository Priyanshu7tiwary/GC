// Mock catalogue: previous-year papers (by year & slot) and sectional mocks.
// Custom (AI-generated) mocks live in the store, not here.

export const SECTION_MINUTES = 40;

// Verified official papers bundled in this app. Earlier CAT papers were not
// publicly released by IIMs, so do not create fake playable entries for them.
const PATTERNS = {
  2017: { slots: 2, q: { VARC: 34, DILR: 32, QA: 34 }, min: 60 },
  2018: { slots: 2, q: { VARC: 34, DILR: 32, QA: 34 }, min: 60 },
  2019: { slots: 2, q: { VARC: 34, DILR: 32, QA: 34 }, min: 60 },
  2020: { slots: 3, q: { VARC: 26, DILR: 24, QA: 26 }, min: 40 },
  2021: { slots: 3, q: { VARC: 24, DILR: 20, QA: 22 }, min: 40 },
  2022: { slots: 3, q: { VARC: 24, DILR: 20, QA: 22 }, min: 40 },
  2023: { slots: 3, q: { VARC: 24, DILR: 20, QA: 22 }, min: 40 },
  2024: { slots: 3, q: { VARC: 24, DILR: 22, QA: 22 }, min: 40 },
  2025: { slots: 3, q: { VARC: 24, DILR: 22, QA: 22 }, min: 40 },
};

// CAT years 2011–2025: include the archival years so their availability is explicit.
export const PYQ_YEARS = Array.from({ length: 15 }, (_, i) => 2025 - i);

const PAPER_ASSETS = {
  2017: [1, 2],
  2018: [1, 2],
  2019: [1, 2],
  2020: [1, 2, 3],
  2021: [1, 2, 3],
  2022: [1, 2, 3],
  2023: [1, 2, 3],
  2024: [1, 2, 3],
  2025: [1, 2, 3],
};

const PYQ = PYQ_YEARS.flatMap((year) => {
  const p = PATTERNS[year];
  if (!p) return []; // archival years (2011–2016) have no official paper, so no playable entries
  const total = p.q.VARC + p.q.DILR + p.q.QA;
  return Array.from({ length: p.slots }, (_, i) => ({
    id: `pyq-${year}-${i + 1}`,
    kind: 'pyq',
    year,
    slot: i + 1,
    title: `CAT ${year} · Slot ${i + 1}`,
    sections: ['VARC', 'DILR', 'QA'],
    sectionQs: p.q,
    questions: total,
    sectionMinutes: p.min,
    minutes: p.min * 3,
    difficulty: 'Actual paper',
    paperUrl: PAPER_ASSETS[year]?.includes(i + 1)
      ? `/cat-papers/${year}/CAT_${year}_slot_${i + 1}_official.pdf`
      : null,
  }));
});

const SECTIONAL_TOPICS = {
  VARC: ['RC Mixed', 'RC Philosophy & Abstract', 'Para Jumbles & Summary', 'RC Economics & Policy', 'VA Mixed', 'RC Science & Tech', 'Full VARC Drill', 'Odd Sentence & Placement'],
  DILR: ['Arrangements', 'Tables & Caselets', 'Games & Tournaments', 'Graphs & Charts', 'Networks & Routes', 'Quant-based LR', 'Mixed Sets', 'Scheduling'],
  QA: ['Arithmetic I', 'Algebra I', 'Number System', 'Geometry & Mensuration', 'Arithmetic II', 'Modern Maths', 'Algebra II', 'Mixed QA'],
};

const SECTIONAL = Object.entries(SECTIONAL_TOPICS).flatMap(([sec, names]) =>
  names.map((name, i) => ({
    id: `sec-${sec}-${i + 1}`,
    kind: 'sectional',
    title: `${sec} Sectional ${String(i + 1).padStart(2, '0')}`,
    subtitle: name,
    sections: [sec],
    questions: sec === 'VARC' ? 24 : 22,
    sectionMinutes: SECTION_MINUTES,
    minutes: SECTION_MINUTES,
    difficulty: ['Easy', 'Moderate', 'Hard', 'Moderate', 'Easy', 'Hard', 'Moderate', 'Hard'][i],
  })),
);

export const MOCKS = [...PYQ, ...SECTIONAL];

export const findMock = (id, customMocks = []) => MOCKS.find((m) => m.id === id) || customMocks.find((m) => m.id === id);
