// Rough score-ratio → percentile model calibrated to recent CAT trends (~100/198 ≈ 99 %ile).
export const estPercentile = (ratio) => Math.max(0, Math.min(99.99, 100 / (1 + Math.exp(-(ratio - 0.1) * 12))));

// Inverse: score ratio needed for a percentile
export const ratioForPercentile = (p) => 0.1 - Math.log(100 / Math.min(p, 99.95) - 1) / 12;
