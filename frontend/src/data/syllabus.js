// CAT syllabus, grouped by section → chapter → topic.
export const SECTIONS = {
  VARC: { id: 'VARC', name: 'Verbal Ability & Reading Comprehension', short: 'VARC', color: 'var(--s-varc)' },
  DILR: { id: 'DILR', name: 'Data Interpretation & Logical Reasoning', short: 'DILR', color: 'var(--s-dilr)' },
  QA: { id: 'QA', name: 'Quantitative Aptitude', short: 'QA', color: 'var(--s-qa)' },
};

export const SYLLABUS = [
  {
    section: 'VARC',
    chapters: [
      { name: 'Reading Comprehension', topics: ['Inference questions', 'Main idea / Title', 'Tone & Attitude', 'Specific detail', 'Strengthen / Weaken', 'Philosophy passages', 'Science & Tech passages', 'Economics passages'] },
      { name: 'Verbal Ability', topics: ['Para jumbles', 'Para summary', 'Odd sentence out', 'Sentence placement'] },
    ],
  },
  {
    section: 'DILR',
    chapters: [
      { name: 'Data Interpretation', topics: ['Tables & Caselets', 'Bar & Line graphs', 'Pie charts', 'Venn diagrams', 'Data sufficiency'] },
      { name: 'Logical Reasoning', topics: ['Arrangements (linear/circular)', 'Scheduling & Calendars', 'Games & Tournaments', 'Networks & Routes', 'Cubes & Dice', 'Binary logic (truth/lie)', 'Selections & Distributions', 'Quant-based LR'] },
    ],
  },
  {
    section: 'QA',
    chapters: [
      { name: 'Arithmetic', topics: ['Percentages', 'Profit, Loss & Discount', 'Simple & Compound Interest', 'Ratio & Proportion', 'Mixtures & Alligation', 'Time & Work', 'Time, Speed & Distance', 'Averages'] },
      { name: 'Algebra', topics: ['Linear equations', 'Quadratic equations', 'Inequalities & Modulus', 'Functions & Graphs', 'Logarithms', 'Progressions (AP/GP/HP)', 'Surds & Indices'] },
      { name: 'Number System', topics: ['Divisibility & Remainders', 'HCF & LCM', 'Factors', 'Base systems', 'Unit & last digits'] },
      { name: 'Geometry & Mensuration', topics: ['Triangles', 'Circles', 'Polygons & Quadrilaterals', 'Coordinate geometry', '3D Mensuration', 'Trigonometry basics'] },
      { name: 'Modern Maths', topics: ['Permutations & Combinations', 'Probability', 'Set theory', 'Sequences & Series'] },
    ],
  },
];

export const topicId = (section, chapter, topic) =>
  `${section}::${chapter}::${topic}`.toLowerCase().replace(/[^a-z0-9:]+/g, '-');

export const ALL_TOPICS = SYLLABUS.flatMap((s) =>
  s.chapters.flatMap((c) =>
    c.topics.map((t) => ({ id: topicId(s.section, c.name, t), section: s.section, chapter: c.name, name: t })),
  ),
);
