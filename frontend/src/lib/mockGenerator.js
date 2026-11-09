// AI mock generation runs on the server: POST /api/mocks/custom starts a job, and the job's steps are polled
// until it succeeds. The server draws questions from every keyed question in the database (official CAT papers,
// the practice bank and admin imports) and, when an Anthropic API key is configured, adds a few new questions
// drafted by Claude that pass an independent solve.
//
// Generated mocks are played and scored through the API, so their answer keys never reach the browser before submit.
import { api } from './api.js';
import { fromServer } from './catalog.js';
import { toAttempt } from './progress.js';
import { ALL_TOPICS } from '../data/syllabus.js';

/** Questions per section in a generated mock (the server's app.generator.questions-per-section). */
export const QUESTIONS_PER_SECTION = 22;
export const SECTION_QS = { VARC: QUESTIONS_PER_SECTION, DILR: QUESTIONS_PER_SECTION, QA: QUESTIONS_PER_SECTION };

// Mirrors the server pipeline (generator/steps); the server sends the live state of each step.
export const WORKFLOW = [
  { id: 'profile', label: 'Analysing your performance', detail: 'Reading your coverage to find weak areas' },
  { id: 'blueprint', label: 'Building the test blueprint', detail: `${QUESTIONS_PER_SECTION} per section, spread over chapters and topics like recent CAT papers` },
  { id: 'generate', label: 'Generating questions', detail: 'Picking whole sets and a question for every planned topic' },
  { id: 'validate', label: 'Validating answers', detail: 'Solving each question independently to verify the key' },
  { id: 'assemble', label: 'Assembling your mock', detail: 'Ordering sections, timing and marking scheme' },
];

const DIFFICULTY = { Easy: 'EASY', Moderate: 'MODERATE', Hard: 'HARD', 'CAT-level': 'CAT_LEVEL', Adaptive: 'ADAPTIVE' };
const wait = (ms) => new Promise((r) => setTimeout(r, ms));

/** A generated mock in the shape the rest of the app uses for mocks. */
export const toLocalMock = (m) => ({ ...fromServer(m), kind: 'custom', createdAt: m.createdAt ? Date.parse(m.createdAt) : Date.now() });

/**
 * @param {{type:'full'|'sectional', sections:string[], difficulty:string, focusWeak:boolean, timed:boolean}} config
 * @param {{coverage:object}} ctx  learner state used for personalisation (coverage lives in the browser)
 * @param {(steps: object[]) => void} onSteps  receives the server's step list on every poll
 */
export async function generateMock(config, ctx, onSteps) {
  const sections = config.type === 'full' ? ['VARC', 'DILR', 'QA'] : config.sections;
  const weak = ALL_TOPICS.filter((t) => sections.includes(t.section) && (ctx.coverage[t.id]?.status ?? 0) < 2)
    .sort((a, b) => (ctx.coverage[a.id]?.practiced ?? 0) - (ctx.coverage[b.id]?.practiced ?? 0))
    .slice(0, 8)
    .map((t) => t.name);

  let job = await api('/api/mocks/custom', {
    method: 'POST',
    body: {
      type: config.type === 'full' ? 'FULL' : 'SECTIONAL',
      sections,
      difficulty: DIFFICULTY[config.difficulty] || 'CAT_LEVEL',
      focusWeakTopics: config.focusWeak,
      strictTiming: config.timed,
      focusTopics: config.focusWeak ? weak : [],
    },
  });
  onSteps(job.steps);
  const deadline = Date.now() + 10 * 60 * 1000; // AI drafting can take a few minutes
  while (job.status !== 'SUCCEEDED') {
    if (job.status === 'FAILED') throw new Error(job.error || 'Generation failed.');
    if (Date.now() > deadline) throw new Error('Generation is taking too long. Check “Your generated mocks” again in a few minutes.');
    await wait(1000);
    job = await api(`/api/generation-jobs/${job.id}`);
    onSteps(job.steps);
  }
  return toLocalMock(await api(`/api/mocks/${job.resultMockId}`));
}

export const listServerMocks = async () => (await api('/api/mocks?kind=CUSTOM')).map(toLocalMock);
export const deleteServerMock = (id) => api(`/api/mocks/${encodeURIComponent(id)}`, { method: 'DELETE' });

/** The questions of a generated mock, without answer keys. */
export async function loadServerPaper(mockId) {
  const paper = await api(`/api/mocks/${encodeURIComponent(mockId)}/paper`);
  return paper.sections.flatMap((s) => s.questions.map((q) => ({
    id: q.id,
    section: q.section,
    topic: q.topic,
    type: q.type === 'MCQ' ? 'mcq' : 'tita',
    q: q.prompt,
    passage: q.passage,
    options: q.options || [],
    figure: q.hasFigure,
    scored: q.scored,
  })));
}

/** Scores an attempt on the server and returns it in the app's attempt shape. */
export async function submitServerAttempt(mock, sections, answers, timeBySection) {
  const detail = await api(`/api/mocks/${encodeURIComponent(mock.id)}/attempts`, {
    method: 'POST',
    body: {
      answers: Object.fromEntries(Object.entries(answers).map(([k, v]) => [k, String(v)])),
      sectionSeconds: Object.fromEntries(sections.map((s) => [s, timeBySection[s] || 0])),
      sections,
    },
  });
  return toAttempt(detail.summary);
}

/** A server-scored attempt: { questions (with keys and explanations), summary } where summary is in the local attempt shape. */
export async function loadServerAttempt(serverAttemptId) {
  const detail = await api(`/api/attempts/${encodeURIComponent(serverAttemptId)}`);
  const summary = toAttempt(detail.summary);
  // the learner's answers in the app's shape: option index for MCQ, text for TITA
  const answers = Object.fromEntries(detail.solutions.filter((x) => x.response != null)
    .map((x) => [x.questionId, x.type === 'MCQ' ? Number(x.response) : x.response]));
  return { summary, answers, questionIds: detail.solutions.map((x) => x.questionId), questions: detail.solutions.map((x) => ({
    id: x.questionId,
    section: x.section,
    topic: x.topic,
    type: x.type === 'MCQ' ? 'mcq' : 'tita',
    q: x.prompt,
    passage: x.passage,
    options: x.options || [],
    answer: x.correctAnswer == null ? null : x.type === 'MCQ' ? Number(x.correctAnswer) : x.correctAnswer,
    explanation: x.explanation,
    corrected: !!x.corrected,
  })) };
}
