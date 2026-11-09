// The mock catalogue comes from the API. Previous-year papers are listed, served, scored and reviewed by the
// server (GET /api/mocks, /api/mocks/<id>/paper, POST /api/mocks/<id>/attempts); the browser holds no paper files.
import { useEffect, useState } from 'react';
import { api } from './api.js';
import { findMock as findLocalMock } from '../data/mocks.js';

const KIND = { PREVIOUS_YEAR: 'pyq', SECTIONAL: 'sectional', CUSTOM: 'custom' };

/** A server mock summary in the shape the app's screens use. */
export function fromServer(m) {
  return {
    id: m.id,
    kind: KIND[m.kind] || 'custom',
    source: 'server',
    title: m.title,
    subtitle: m.subtitle,
    year: m.year ?? undefined,
    slot: m.slot ?? undefined,
    sections: m.sections,
    sectionQs: m.sectionQuestionCounts || undefined,
    questions: m.questionCount,
    sectionMinutes: m.sectionMinutes,
    minutes: m.totalMinutes,
    difficulty: m.kind === 'PREVIOUS_YEAR' ? 'Actual paper' : m.difficulty,
    focusTopics: m.focusTopics || [],
    createdAt: m.createdAt ? Date.parse(m.createdAt) : undefined,
  };
}

let papers = null; // Promise<mock[]> of previous-year papers, loaded once per page load

export function loadPapers() {
  if (!papers) {
    papers = api('/api/mocks?kind=PREVIOUS_YEAR').then((list) => list.map(fromServer));
    papers.catch(() => { papers = null; }); // allow a retry after a failure
  }
  return papers;
}

/** Previous-year papers from the API: { papers: mock[] | null, error }. papers is null while loading. */
export function usePapers() {
  const [state, setState] = useState({ papers: null, error: null });
  useEffect(() => {
    let live = true;
    loadPapers().then((list) => live && setState({ papers: list, error: null }), (error) => live && setState({ papers: null, error }));
    return () => { live = false; };
  }, []);
  return state;
}

/**
 * One mock by id: { mock, loading, error }. Previous-year papers come from the API; sectional and generated mocks
 * from the app's lists.
 */
export function useMock(id, customMocks = []) {
  const local = findLocalMock(id, customMocks);
  const isPaper = !local && /^pyq-/.test(id || '');
  const { papers: list, error } = usePapers();
  if (local) return { mock: local, loading: false, error: null };
  if (!isPaper) return { mock: null, loading: false, error: null };
  return { mock: list ? list.find((m) => m.id === id) || null : null, loading: !list && !error, error };
}
