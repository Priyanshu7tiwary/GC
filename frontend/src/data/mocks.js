// Mock tests come from the API (lib/catalog.js): previous-year papers, sectionals and generated mocks.

export const SECTION_MINUTES = 40;

// CAT years 2011–2025 shown in the year picker; years without an official paper say so.
export const PYQ_YEARS = Array.from({ length: 15 }, (_, i) => 2025 - i);

/** A generated mock from the learner's list. */
export const findMock = (id, customMocks = []) => customMocks.find((m) => m.id === id);
