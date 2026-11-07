// AI mock generator workflow.
// Each step is a stage of the pipeline. Today they run locally against the demo
// question bank; replace `run` bodies with real API calls (e.g. a backend
// endpoint that prompts an LLM) without touching the UI.

import { QUESTIONS } from '../data/questions.js';
import { ALL_TOPICS } from '../data/syllabus.js';

const wait = (ms) => new Promise((r) => setTimeout(r, ms));

export const SECTION_QS = { VARC: 24, DILR: 22, QA: 22 };

export const WORKFLOW = [
  { id: 'profile', label: 'Analysing your performance', detail: 'Reading past attempts & coverage to find weak areas' },
  { id: 'blueprint', label: 'Building the test blueprint', detail: 'Choosing topic mix and difficulty curve per section' },
  { id: 'generate', label: 'Generating questions', detail: 'Drafting passages, sets and questions' },
  { id: 'validate', label: 'Validating answers', detail: 'Solving each question independently to verify the key' },
  { id: 'assemble', label: 'Assembling your mock', detail: 'Ordering sections, timing and marking scheme' },
];

/**
 * @param {{type:'full'|'sectional', sections:string[], difficulty:string, focusWeak:boolean, timed:boolean}} config
 * @param {{coverage:object}} ctx  user state used for personalisation
 * @param {(stepId:string, info?:string)=>void} onStep progress callback
 */
export async function generateMock(config, ctx, onStep) {
  const sections = config.type === 'full' ? ['VARC', 'DILR', 'QA'] : config.sections;

  onStep('profile');
  await wait(700);
  const weak = ALL_TOPICS.filter((t) => sections.includes(t.section) && (ctx.coverage[t.id]?.status ?? 0) < 2)
    .sort((a, b) => (ctx.coverage[a.id]?.practiced ?? 0) - (ctx.coverage[b.id]?.practiced ?? 0))
    .slice(0, 6);
  onStep('profile', `${weak.length} weak topics found`);

  onStep('blueprint');
  await wait(800);
  const blueprint = Object.fromEntries(sections.map((s) => [s, SECTION_QS[s]]));

  onStep('generate');
  await wait(1200);
  // Demo: draw from the local bank. Weak-topic questions first when requested.
  const weakNames = new Set(weak.map((w) => w.name));
  const questionIds = sections.flatMap((s) => {
    const pool = QUESTIONS.filter((q) => q.section === s);
    return (config.focusWeak ? [...pool].sort((a, b) => weakNames.has(b.topic) - weakNames.has(a.topic)) : pool).map((q) => q.id);
  });

  onStep('validate');
  await wait(900);

  onStep('assemble');
  await wait(500);

  const n = Date.now();
  const sectionMinutes = config.timed ? 40 : 60;
  return {
    id: `custom-${n}`,
    kind: 'custom',
    title: config.type === 'full' ? 'AI Full Mock' : `AI ${sections.join(' + ')} Sectional`,
    subtitle: `${config.difficulty}${config.focusWeak ? ' · weak-area focus' : ''}`,
    sections,
    questions: Object.values(blueprint).reduce((a, b) => a + b, 0),
    sectionMinutes,
    minutes: sectionMinutes * sections.length,
    difficulty: config.difficulty,
    focusTopics: weak.map((w) => w.name),
    questionIds,
    createdAt: n,
  };
}
