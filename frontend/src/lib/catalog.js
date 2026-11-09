// The mock catalogue comes from the API. Previous-year papers are listed, served, scored and reviewed by the
// server (GET /api/mocks, /api/mocks/<id>/paper, POST /api/mocks/<id>/attempts); the browser holds no paper files.
import { useEffect, useState } from 'react';
import { api } from './api.js';

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

const cache = {}; // kind -> Promise<mock[]>, loaded once per page load

function load(kind) {
  if (!cache[kind]) {
    cache[kind] = api(`/api/mocks?kind=${kind}`).then((list) => list.map(fromServer));
    cache[kind].catch(() => { delete cache[kind]; }); // allow a retry after a failure
  }
  return cache[kind];
}
export const loadPapers = () => load('PREVIOUS_YEAR');

/** Mocks of one kind from the API: { list: mock[] | null, error }. list is null while loading. */
export function useCatalog(kind) {
  const [state, setState] = useState({ list: null, error: null });
  useEffect(() => {
    let live = true;
    load(kind).then((list) => live && setState({ list, error: null }), (error) => live && setState({ list: null, error }));
    return () => { live = false; };
  }, [kind]);
  return state;
}

/** Previous-year papers: { papers, error }. */
export function usePapers() {
  const { list, error } = useCatalog('PREVIOUS_YEAR');
  return { papers: list, error };
}

/** One mock by id: { mock, loading, error }. Papers and sectionals come from the API, generated mocks from the app's list. */
export function useMock(id, customMocks = []) {
  const custom = customMocks.find((m) => m.id === id);
  const kind = /^pyq-/.test(id || '') ? 'PREVIOUS_YEAR' : /^sec-/.test(id || '') ? 'SECTIONAL' : null;
  const { list, error } = useCatalog(kind || 'PREVIOUS_YEAR');
  if (custom) return { mock: custom, loading: false, error: null };
  if (!kind) return { mock: null, loading: false, error: null };
  return { mock: list ? list.find((m) => m.id === id) || null : null, loading: !list && !error, error };
}
