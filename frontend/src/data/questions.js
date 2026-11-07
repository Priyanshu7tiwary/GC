// A compact original question bank used to run mock tests in the browser.
// type: 'mcq' (+3 / -1) or 'tita' (+3 / 0, type-in-the-answer)

const RC_PASSAGE = `Cities have long been described as engines of innovation, yet the mechanism is less romantic than the metaphor suggests. Density does not make individuals smarter; it makes accidental encounters cheaper. A designer who shares a lunch counter with a chemist, or a programmer who overhears a logistics manager complaining, is exposed to problems they would never have sought out. The value lies not in the planned meeting but in the unplanned one.

This explains a paradox that has puzzled observers of remote work. Productivity on well-defined tasks often rises when workers leave the office, because interruptions fall. But the generation of new ideas — which depends on precisely those interruptions — tends to slow. Firms that measure output by tickets closed will see gains; firms that depend on recombining knowledge from unrelated domains may see a quiet erosion that does not appear on any dashboard for years.

None of this means that physical proximity is sufficient. Many dense neighbourhoods produce little innovation because their residents occupy a narrow band of professions, or because institutions discourage strangers from talking. Proximity creates the opportunity for collision; culture decides whether collisions happen.`;

const DI_SET = `Five friends — Arun, Bela, Chirag, Divya and Esha — each bought exactly one gadget from: Laptop, Phone, Tablet, Watch, Camera (all different). Their spends were ₹10k, ₹20k, ₹30k, ₹40k and ₹50k in some order.
• The Laptop cost the most.
• Bela spent ₹20k more than Divya.
• Chirag bought the Camera and spent more than Divya but less than Bela.
• Arun spent more than Chirag.
• Esha spent ₹10k and bought neither the Watch nor the Tablet.
• Divya did not buy the Watch.`;

export const QUESTIONS = [
  // ---------------- VARC ----------------
  {
    id: 'v1', section: 'VARC', topic: 'Main idea / Title', type: 'mcq', passage: RC_PASSAGE,
    q: 'Which of the following best captures the central argument of the passage?',
    options: [
      'Dense cities make individuals more intelligent through competition.',
      'Innovation in cities stems from cheap, unplanned encounters, which culture must enable.',
      'Remote work is harmful to all kinds of productivity.',
      'Firms should measure innovation using dashboards of closed tickets.',
    ],
    answer: 1,
    explanation: 'The author argues density lowers the cost of accidental encounters (para 1) and that culture determines whether they occur (para 3). Option A is explicitly rejected; C and D distort para 2.',
  },
  {
    id: 'v2', section: 'VARC', topic: 'Inference questions', type: 'mcq', passage: RC_PASSAGE,
    q: 'The author would most likely agree that a firm relying on cross-domain ideas that moves fully remote would:',
    options: [
      'immediately see a sharp fall in measured output.',
      'see no change since proximity is never sufficient.',
      'possibly suffer a slow decline in innovation that is hard to detect early.',
      'benefit, because interruptions are always counterproductive.',
    ],
    answer: 2,
    explanation: 'Para 2: such firms "may see a quiet erosion that does not appear on any dashboard for years."',
  },
  {
    id: 'v3', section: 'VARC', topic: 'Strengthen / Weaken', type: 'mcq', passage: RC_PASSAGE,
    q: 'Which finding would most WEAKEN the claim made in the final paragraph?',
    options: [
      'Dense neighbourhoods with diverse professions and open institutions show no more innovation than sparse ones.',
      'Remote workers close more tickets than office workers.',
      'Chemists and designers rarely share lunch counters.',
      'Some dense neighbourhoods have narrow professional bands.',
    ],
    answer: 0,
    explanation: 'The last paragraph says proximity + enabling culture produce collisions/innovation. If diverse, open, dense areas show no gain, that claim is undercut.',
  },
  {
    id: 'v4', section: 'VARC', topic: 'Para jumbles', type: 'tita',
    q: `Arrange the sentences into a coherent paragraph. Enter the sequence of numbers (e.g. 2143).
1. Yet the habit persisted long after the reason had disappeared.
2. Early printers set type by hand, so spacing after a full stop was adjusted to help the eye.
3. Today, most style guides recommend a single space.
4. Typewriters, with their fixed-width letters, encouraged typists to add two spaces.`,
    answer: '2413',
    explanation: '2 introduces the origin, 4 continues the history with typewriters, 1 contrasts ("Yet the habit persisted"), 3 concludes with today.',
  },
  {
    id: 'v5', section: 'VARC', topic: 'Odd sentence out', type: 'tita',
    q: `Four sentences are given. Three form a coherent paragraph. Enter the number of the sentence that does not fit.
1. Coral reefs occupy less than one percent of the ocean floor.
2. Despite this, they support roughly a quarter of all marine species.
3. Many reef fish change colour as they mature.
4. Their loss would therefore ripple far beyond the reefs themselves.`,
    answer: '3',
    explanation: '1-2-4 discuss reefs’ outsized ecological importance. 3 is a tangential fact about reef fish.',
  },

  // ---------------- DILR ----------------
  {
    id: 'd1', section: 'DILR', topic: 'Selections & Distributions', type: 'mcq', passage: DI_SET,
    q: 'Who bought the Laptop?',
    options: ['Arun', 'Bela', 'Chirag', 'Divya'],
    answer: 0,
    explanation: 'Esha = 10k. Bela − Divya = 20k → (20,40) or (30,50). Chirag lies between them: 30 or 40. Arun > Chirag rules out the second case (Arun would be 20). So D=20, C=30, B=40, A=50 → Arun has the Laptop.',
  },
  {
    id: 'd2', section: 'DILR', topic: 'Selections & Distributions', type: 'tita', passage: DI_SET,
    q: 'How much (in ₹ thousands) did the person who bought the Watch spend?',
    answer: '40',
    explanation: 'Esha (10k) has neither Watch nor Tablet, nor Laptop/Camera → Phone. Divya is not Watch → Tablet. Bela → Watch at 40k.',
  },
  {
    id: 'd3', section: 'DILR', topic: 'Tables & Caselets', type: 'mcq',
    q: 'A store’s revenue was ₹80L in 2023 and ₹92L in 2024. Expenses were ₹60L and ₹66L respectively. By what percentage did profit grow?',
    options: ['20%', '25%', '30%', '15%'],
    answer: 2,
    explanation: 'Profit 2023 = 20L, 2024 = 26L → growth = 6/20 = 30%.',
  },
  {
    id: 'd4', section: 'DILR', topic: 'Games & Tournaments', type: 'tita',
    q: 'In a single round-robin tournament, every team plays every other team exactly once. If 45 matches are played in total, how many teams took part?',
    answer: '10',
    explanation: 'n(n−1)/2 = 45 → n = 10.',
  },
  {
    id: 'd5', section: 'DILR', topic: 'Arrangements (linear/circular)', type: 'mcq',
    q: 'In how many distinct ways can 6 people sit around a round table if two particular people must sit together?',
    options: ['24', '48', '120', '240'],
    answer: 1,
    explanation: 'Treat the pair as one unit: (5−1)! = 24 arrangements × 2 internal orders = 48.',
  },

  // ---------------- QA ----------------
  {
    id: 'q1', section: 'QA', topic: 'Percentages', type: 'mcq',
    q: 'The price of an item is increased by 25% and then decreased by 20%. The net change in price is:',
    options: ['5% increase', '0%', '5% decrease', '2% increase'],
    answer: 1,
    explanation: '1.25 × 0.80 = 1.00 → no change.',
  },
  {
    id: 'q2', section: 'QA', topic: 'Time, Speed & Distance', type: 'tita',
    q: 'A car travels from A to B at 60 km/h and returns at 40 km/h. What is its average speed (km/h) for the round trip?',
    answer: '48',
    explanation: 'Harmonic mean: 2·60·40/(60+40) = 48.',
  },
  {
    id: 'q3', section: 'QA', topic: 'Quadratic equations', type: 'mcq',
    q: 'If the roots of x² − 7x + k = 0 differ by 3, what is k?',
    options: ['8', '10', '12', '14'],
    answer: 1,
    explanation: 'Roots r, r+3 with sum 7 → r = 2 → roots 2, 5 → k = 10.',
  },
  {
    id: 'q4', section: 'QA', topic: 'Divisibility & Remainders', type: 'tita',
    q: 'What is the remainder when 2¹⁰⁰ is divided by 7?',
    answer: '2',
    explanation: '2³ ≡ 1 (mod 7). 100 = 3·33 + 1 → 2¹⁰⁰ ≡ 2.',
  },
  {
    id: 'q5', section: 'QA', topic: 'Time & Work', type: 'mcq',
    q: 'A can finish a job in 12 days and B in 18 days. They work together for 4 days, then A leaves. How many more days does B need?',
    options: ['6', '8', '10', '12'],
    answer: 1,
    explanation: 'Together per day: 1/12 + 1/18 = 5/36. In 4 days: 20/36. Left: 16/36 → B needs 16/36 × 18 = 8 days.',
  },
  {
    id: 'q6', section: 'QA', topic: 'Probability', type: 'mcq',
    q: 'Two fair dice are rolled. What is the probability that the sum is a prime number?',
    options: ['5/12', '1/2', '7/18', '15/36'],
    answer: 0,
    explanation: 'Prime sums 2,3,5,7,11 → 1+2+4+6+2 = 15 outcomes → 15/36 = 5/12.',
  },
  {
    id: 'q7', section: 'QA', topic: 'Logarithms', type: 'tita',
    q: 'If log₂(x) + log₂(x − 2) = 3, find x.',
    answer: '4',
    explanation: 'x(x−2) = 8 → x² − 2x − 8 = 0 → x = 4 (x = −2 rejected).',
  },
  {
    id: 'q8', section: 'QA', topic: 'Triangles', type: 'mcq',
    q: 'The sides of a triangle are 13, 14 and 15. What is its area?',
    options: ['80', '84', '90', '96'],
    answer: 1,
    explanation: 's = 21; Heron: √(21·8·7·6) = √7056 = 84.',
  },
];
