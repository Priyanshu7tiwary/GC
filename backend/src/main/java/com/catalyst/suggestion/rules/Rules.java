package com.catalyst.suggestion.rules;

/** Small formatting helpers shared by rules. */
final class Rules {
    private Rules() {}

    static String fmt(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
    }

    static String hours(int minutes) {
        return String.format("%.1f h", minutes / 60.0);
    }
}
