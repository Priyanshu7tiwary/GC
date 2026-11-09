// Topic practice: every question in a topic's bank, with the learner's solved / marked-for-revision state kept on
// the server. Keys and explanations arrive only after an answer is checked or the question is marked solved.
import { api } from './api.js';

const enc = encodeURIComponent;

/** { [topicId]: { questions, keyed, solved, marked } } */
export const practiceSummary = () => api('/api/practice/topics');

/** filter: 'all' | 'unsolved' | 'solved' | 'marked' */
export const topicQuestions = (topicId, filter = 'all', page = 0, size = 20) =>
  api(`/api/practice/topics/${enc(topicId)}/questions?filter=${filter}&page=${page}&size=${size}`);

/** answer: option index (MCQ) or the typed value (TITA). Resolves to { correct, correctAnswer, explanation, state }. */
export const checkAnswer = (questionId, answer) =>
  api(`/api/practice/questions/${enc(questionId)}/check`, { method: 'POST', body: { answer: String(answer) } });

/** patch: { solved?: boolean, marked?: boolean }. Resolves to the updated question. */
export const markQuestion = (questionId, patch) =>
  api(`/api/practice/questions/${enc(questionId)}`, { method: 'PUT', body: patch });
