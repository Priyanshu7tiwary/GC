// Sectional mocks. Previous-year papers come from the API (lib/catalog.js); generated mocks live in the store.

export const SECTION_MINUTES = 40;

// CAT years 2011–2025 shown in the year picker; years without an official paper say so.
export const PYQ_YEARS = Array.from({ length: 15 }, (_, i) => 2025 - i);

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

export const MOCKS = SECTIONAL;

export const findMock = (id, customMocks = []) => MOCKS.find((m) => m.id === id) || customMocks.find((m) => m.id === id);
