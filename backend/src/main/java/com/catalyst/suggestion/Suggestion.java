package com.catalyst.suggestion;

/** One piece of advice. {@code action.route} is a frontend path the UI can navigate to. */
public record Suggestion(String id, String category, Priority priority, String title, String body, String metric, Action action) {
    public record Action(String label, String route) {}
}
