package com.catalyst.generator.plan;

import com.catalyst.common.domain.Section;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Plans a well-covered section from the question bank's own statistics.
 *
 * Chapter weights come from how often each chapter appears in the official CAT papers, so a generated test mirrors
 * the real exam's emphasis. Slots are spread over chapters by largest remainder and, within a chapter, over distinct
 * topics before any topic gets a second question. Weak topics are boosted and, when focus is on, guaranteed a slot.
 * Topics with no questions in the bank are never planned.
 *
 * Section shapes follow recent CAT papers: VARC ≈ 70% reading comprehension (whole passages) and 30% verbal ability
 * spread over its question types; DILR is made of sets (about five questions each) on different topics; QA is
 * standalone questions.
 */
public final class BlueprintPlanner {
    static final double RC_SHARE = 0.7;
    static final int DILR_SET_SIZE = 5;
    static final double WEAK_BOOST = 2.5;

    private BlueprintPlanner() {}

    public static SectionPlan plan(Section section, int total, Collection<TopicStats> topics, Set<String> weak, boolean focusWeak) {
        List<TopicStats> usable = topics.stream().filter(t -> t.section() == section && t.available() > 0).toList();
        return switch (section) {
            case QA -> new SectionPlan(section, total, 0, List.of(), spread(usable, total, weak, focusWeak));
            case VARC -> {
                int rc = (int) Math.round(total * RC_SHARE);
                List<TopicStats> va = usable.stream().filter(t -> "Verbal Ability".equals(t.chapter())).toList();
                List<SectionPlan.Slot> vaSlots = spread(va, total - rc, weak, focusWeak);
                int planned = vaSlots.stream().mapToInt(SectionPlan.Slot::count).sum();
                yield new SectionPlan(section, total, total - planned, List.of(), vaSlots);
            }
            case DILR -> {
                int sets = Math.max(1, (int) Math.round(total / (double) DILR_SET_SIZE));
                yield new SectionPlan(section, total, total, setTopics(usable, sets, weak, focusWeak), List.of());
            }
        };
    }

    private static double weight(TopicStats t, Set<String> weak) {
        double w = Math.max(1, t.pyqWeight());
        return weak.contains(t.name()) ? w * WEAK_BOOST : w;
    }

    /** n standalone slots over chapters, then over distinct topics within each chapter. */
    static List<SectionPlan.Slot> spread(List<TopicStats> topics, int n, Set<String> weak, boolean focusWeak) {
        if (topics.isEmpty() || n <= 0) return List.of();
        Map<String, List<TopicStats>> byChapter = topics.stream()
                .collect(Collectors.groupingBy(TopicStats::chapter, LinkedHashMap::new, Collectors.toList()));
        Map<String, Double> chapterWeight = new LinkedHashMap<>();
        byChapter.forEach((ch, ts) -> chapterWeight.put(ch, ts.stream().mapToDouble(t -> weight(t, weak)).sum()));

        Map<String, Integer> perChapter = largestRemainder(chapterWeight, n);
        // every chapter that has questions gets at least one slot while slots last
        for (String ch : byChapter.keySet()) {
            if (perChapter.get(ch) == 0) {
                String donor = perChapter.entrySet().stream().filter(e -> e.getValue() > 1).max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
                if (donor == null) break;
                perChapter.merge(donor, -1, Integer::sum);
                perChapter.put(ch, 1);
            }
        }

        // a chapter gets no more slots than it has topics while other chapters still have uncovered topics,
        // so topics only repeat once every topic in the section has a question
        boolean moved = true;
        while (moved) {
            moved = false;
            for (String ch : byChapter.keySet()) {
                if (perChapter.get(ch) <= byChapter.get(ch).size()) continue;
                String to = byChapter.keySet().stream().filter(o -> perChapter.get(o) < byChapter.get(o).size())
                        .max(Comparator.comparingDouble(o -> chapterWeight.get(o) / (perChapter.get(o) + 1))).orElse(null);
                if (to == null) break;
                perChapter.merge(ch, -1, Integer::sum);
                perChapter.merge(to, 1, Integer::sum);
                moved = true;
            }
        }

        Map<String, Integer> perTopic = new LinkedHashMap<>();
        byChapter.forEach((ch, ts) -> {
            List<TopicStats> ranked = new ArrayList<>(ts);
            ranked.sort(Comparator.comparingDouble((TopicStats t) -> -(focusWeak && weak.contains(t.name()) ? 1e9 : weight(t, weak))));
            int k = perChapter.get(ch);
            for (int i = 0; i < k; i++) {
                // round-robin: distinct topics first; a topic only takes another slot if it has the questions
                TopicStats t = ranked.get(i % ranked.size());
                if (perTopic.getOrDefault(t.id(), 0) >= t.available()) {
                    t = ranked.stream().filter(x -> perTopic.getOrDefault(x.id(), 0) < x.available()).findFirst().orElse(t);
                }
                perTopic.merge(t.id(), 1, Integer::sum);
            }
        });
        Map<String, TopicStats> byId = topics.stream().collect(Collectors.toMap(TopicStats::id, t -> t));
        return perTopic.entrySet().stream()
                .map(e -> new SectionPlan.Slot(e.getKey(), byId.get(e.getKey()).name(), byId.get(e.getKey()).chapter(), e.getValue()))
                .toList();
    }

    /** Distinct DILR topics for the sets, most important first; weak topics lead when focus is on. */
    static List<SectionPlan.Slot> setTopics(List<TopicStats> topics, int sets, Set<String> weak, boolean focusWeak) {
        List<TopicStats> ranked = new ArrayList<>(topics);
        ranked.sort(Comparator.comparingDouble((TopicStats t) -> -(focusWeak && weak.contains(t.name()) ? 1e9 + t.pyqWeight() : weight(t, weak))));
        // keep a DI/LR balance: alternate chapters while both have topics left
        Map<String, List<TopicStats>> byChapter = ranked.stream()
                .collect(Collectors.groupingBy(TopicStats::chapter, LinkedHashMap::new, Collectors.toList()));
        List<TopicStats> order = new ArrayList<>();
        while (order.size() < ranked.size()) {
            for (List<TopicStats> ts : byChapter.values()) {
                ts.stream().filter(t -> !order.contains(t)).findFirst().ifPresent(order::add);
            }
        }
        if (focusWeak) {  // weak topics first regardless of chapter
            order.sort(Comparator.comparing((TopicStats t) -> !weak.contains(t.name())));
        }
        return order.stream().map(t -> new SectionPlan.Slot(t.id(), t.name(), t.chapter(), 1)).toList();
    }

    /** Integer shares of n proportional to the weights; leftovers go to the largest fractional parts. */
    static Map<String, Integer> largestRemainder(Map<String, Double> weights, int n) {
        double sum = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        Map<String, Integer> out = new LinkedHashMap<>();
        Map<String, Double> frac = new LinkedHashMap<>();
        int given = 0;
        for (var e : weights.entrySet()) {
            double exact = sum == 0 ? (double) n / weights.size() : n * e.getValue() / sum;
            out.put(e.getKey(), (int) Math.floor(exact));
            frac.put(e.getKey(), exact - Math.floor(exact));
            given += (int) Math.floor(exact);
        }
        List<String> byFrac = frac.entrySet().stream().sorted(Map.Entry.<String, Double>comparingByValue().reversed()).map(Map.Entry::getKey).toList();
        for (int i = 0; given < n; i = (i + 1) % byFrac.size(), given++) out.merge(byFrac.get(i), 1, Integer::sum);
        return out;
    }
}
