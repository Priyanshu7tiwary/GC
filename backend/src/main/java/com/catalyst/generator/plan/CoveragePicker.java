package com.catalyst.generator.plan;

import com.catalyst.question.Difficulty;
import com.catalyst.question.Question;
import com.catalyst.question.QuestionType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fills a {@link SectionPlan} from the keyed questions of one section.
 *
 * Passage sets (RC passages, DILR sets) are taken whole; only the last one may be cut short to land on the exact
 * count. Standalone slots are filled from their topic, then from the same chapter, then from anywhere in the
 * section, and every fallback is recorded as a shortfall. Within a choice, questions the learner has not had
 * before come first, then the requested difficulty, then newer papers, with a random tie-breaker so tests differ.
 * QA keeps its TITA share near CAT's (about 30%).
 */
public final class CoveragePicker {
    static final double TITA_TARGET = 0.3;
    private static final Pattern PYQ_YEAR = Pattern.compile("^pyq-(\\d{4})-");

    private final Random random;

    public CoveragePicker(Random random) {
        this.random = random;
    }

    /** The picked questions, in test order, and what they cover. */
    public record Selection(List<Question> questions, Set<String> topicIds, Set<String> chapters, int shortfall, List<String> notes) {}

    public Selection pick(SectionPlan plan, Collection<Question> pool, Optional<Difficulty> level, Set<String> avoid, Map<String, String> chapterOf) {
        Map<String, Double> jitter = new LinkedHashMap<>();
        pool.forEach(q -> jitter.put(q.getId(), random.nextDouble()));
        Set<String> notes = new LinkedHashSet<>();
        Set<String> used = new HashSet<>();
        List<Question> out = new ArrayList<>();
        int shortfall = 0;

        // ---- passage sets
        if (plan.passageQuestions() > 0) {
            List<List<Question>> units = units(pool.stream().filter(q -> hasPassage(q)).toList());
            int need = plan.passageQuestions();
            Set<String> setTopicsUsed = new LinkedHashSet<>();
            // DILR: one set per planned topic first
            for (SectionPlan.Slot slot : plan.setTopics()) {
                if (need <= 0) break;
                int remaining = need;
                // a planned topic only gets a set that keeps the count exactly reachable; otherwise it is skipped here
                List<List<Question>> topicSets = units.stream().filter(u -> slot.topicId().equals(majorityTopic(u)) && !overlaps(u, used)).toList();
                Optional<List<Question>> best = completing(topicSets, units, used, remaining)
                        .stream().max(Comparator.comparingDouble(u -> unitScore(u, avoid, level, jitter) + fit(u.size(), remaining)));
                if (best.isPresent()) {
                    List<Question> take = best.get().subList(0, Math.min(best.get().size(), need));
                    out.addAll(take);
                    take.forEach(q -> used.add(q.getId()));
                    need -= take.size();
                    setTopicsUsed.add(slot.topicId());
                }
            }
            // then any remaining sets (RC passages, or more DILR sets), new topics preferred
            while (need > 0) {
                int remaining = need;
                List<List<Question>> open = units.stream().filter(u -> !overlaps(u, used)).toList();
                List<List<Question>> exact = completing(open, units, used, remaining);
                // if no combination of whole sets hits the count, take the best set and cut it
                Optional<List<Question>> best = (exact.isEmpty() ? open : exact)
                        .stream().max(Comparator.comparingDouble(u -> unitScore(u, avoid, level, jitter) + fit(u.size(), remaining)
                                - (setTopicsUsed.contains(majorityTopic(u)) ? 1.0 : 0)));
                if (best.isEmpty()) break;
                List<Question> take = best.get().subList(0, Math.min(best.get().size(), need));
                out.addAll(take);
                take.forEach(q -> used.add(q.getId()));
                need -= take.size();
                setTopicsUsed.add(majorityTopic(best.get()));
            }
            if (need > 0) {
                shortfall += need;
                notes.add("only " + (plan.passageQuestions() - need) + " passage questions available");
            }
        }

        // ---- standalone slots
        List<Question> singles = pool.stream().filter(q -> !hasPassage(q)).toList();
        int[] tita = {0};
        int[] taken = {0};
        for (SectionPlan.Slot slot : plan.slots()) {
            for (int i = 0; i < slot.count(); i++) {
                Optional<Question> q = bestSingle(singles, used, x -> slot.topicId().equals(x.getTopicId()), avoid, level, jitter, plan, tita, taken);
                if (q.isEmpty()) {
                    q = bestSingle(singles, used, x -> slot.chapter().equals(chapterOf.get(x.getTopicId())), avoid, level, jitter, plan, tita, taken);
                    shortfall++;
                    notes.add("no more " + slot.topic() + " questions; used another " + slot.chapter() + " topic");
                }
                if (q.isEmpty()) q = bestSingle(singles, used, x -> true, avoid, level, jitter, plan, tita, taken);
                if (q.isEmpty()) break;
                add(q.get(), out, used, tita, taken);
            }
        }
        // top up if the plan under-filled (e.g. too few verbal-ability questions)
        while (out.size() < plan.total()) {
            Optional<Question> q = bestSingle(singles, used, x -> true, avoid, level, jitter, plan, tita, taken);
            if (q.isEmpty()) break;
            add(q.get(), out, used, tita, taken);
        }

        Set<String> topicIds = new LinkedHashSet<>();
        Set<String> chapters = new LinkedHashSet<>();
        out.forEach(q -> {
            if (q.getTopicId() != null) {
                topicIds.add(q.getTopicId());
                chapters.add(chapterOf.getOrDefault(q.getTopicId(), "?"));
            }
        });
        return new Selection(order(out), topicIds, chapters, shortfall, List.copyOf(notes));
    }

    /** The candidates after which the rest of the count can still be made of whole, unused sets. */
    static List<List<Question>> completing(List<List<Question>> candidates, List<List<Question>> units, Set<String> used, int remaining) {
        return candidates.stream().filter(c -> reachable(c, units, used, remaining)).toList();
    }

    /** Taking set c leaves nothing to fill, or a remainder that some combination of the other unused sets adds up to (0/1 subset sum). */
    static boolean reachable(List<Question> c, List<List<Question>> units, Set<String> used, int remaining) {
        int left = remaining - c.size();
        if (left == 0) return true;
        if (left < 0) return false;
        boolean[] can = new boolean[left + 1];
        can[0] = true;
        for (List<Question> u : units) {
            if (u == c || overlaps(u, used) || u.size() > left) continue;
            for (int t = left; t >= u.size(); t--) if (can[t - u.size()]) can[t] = true;
            if (can[left]) return true;
        }
        return can[left];
    }

    private void add(Question q, List<Question> out, Set<String> used, int[] tita, int[] taken) {
        out.add(q);
        used.add(q.getId());
        taken[0]++;
        if (q.getType() == QuestionType.TITA) tita[0]++;
    }

    private Optional<Question> bestSingle(List<Question> singles, Set<String> used, java.util.function.Predicate<Question> where,
                                          Set<String> avoid, Optional<Difficulty> level, Map<String, Double> jitter,
                                          SectionPlan plan, int[] tita, int[] taken) {
        boolean wantTita = plan.section() == com.catalyst.common.domain.Section.QA && (tita[0] < Math.round((taken[0] + 1) * TITA_TARGET));
        return singles.stream().filter(q -> !used.contains(q.getId()) && where.test(q))
                .max(Comparator.comparingDouble(q -> score(q, avoid, level, jitter)
                        + (plan.section() == com.catalyst.common.domain.Section.QA && (q.getType() == QuestionType.TITA) == wantTita ? 0.6 : 0)));
    }

    /** Sets first (in the order picked), then standalone questions, as in CAT. */
    private static List<Question> order(List<Question> picked) {
        List<Question> out = new ArrayList<>();
        picked.stream().filter(CoveragePicker::hasPassage).forEach(out::add);
        picked.stream().filter(q -> !hasPassage(q)).forEach(out::add);
        return out;
    }

    private static boolean hasPassage(Question q) {
        return q.getPassage() != null && !q.getPassage().isBlank();
    }

    private static boolean overlaps(List<Question> unit, Set<String> used) {
        return unit.stream().anyMatch(q -> used.contains(q.getId()));
    }

    /**
     * How well a set fits what is left. CAT sets have 4–6 questions, so those are preferred over 3-question sets.
     * A set that would leave 1 or 2 questions to fill is avoided (the leftovers would mean a stub of a set), and a
     * set that has to be cut loses more the more is cut.
     */
    static double fit(int size, int remaining) {
        double s = size <= remaining ? 0.5 : -0.4 * (size - remaining);
        int left = remaining - size;
        if (left > 0 && left < 3) s -= 4.0;  // dominant: outweighs recency, difficulty and the random tie-breaker
        return s + (size >= 4 && size <= 6 ? 0.9 : size == 3 ? 0.3 : -1.5);  // sets under 3 questions are a last resort
    }

    private double unitScore(List<Question> unit, Set<String> avoid, Optional<Difficulty> level, Map<String, Double> jitter) {
        return unit.stream().mapToDouble(q -> score(q, avoid, level, jitter)).average().orElse(0);
    }

    /** Unseen first (heaviest), then difficulty match, then recency, then a random tie-breaker. */
    private static double score(Question q, Set<String> avoid, Optional<Difficulty> level, Map<String, Double> jitter) {
        return (avoid.contains(q.getId()) ? -3.0 : 0)
                + (level.isPresent() && q.getDifficulty() == level.get() ? 0.8 : 0)
                + 0.3 * recency(q)
                + jitter.getOrDefault(q.getId(), 0.0);
    }

    static double recency(Question q) {
        if (q.getPaperId() == null) return 1.0;
        Matcher m = PYQ_YEAR.matcher(q.getPaperId());
        return m.find() ? Math.max(0, Math.min(1, (Integer.parseInt(m.group(1)) - 2017) / 8.0)) : 0.5;
    }

    /** Questions sharing a passage form one unit, in paper order. */
    static List<List<Question>> units(List<Question> withPassage) {
        Map<String, List<Question>> byKey = new LinkedHashMap<>();
        for (Question q : withPassage) {
            byKey.computeIfAbsent(q.getPaperId() + ":" + q.getPassage().hashCode(), k -> new ArrayList<>()).add(q);
        }
        Comparator<Question> paperOrder = Comparator
                .comparing((Question q) -> q.getQuestionNumber() == null ? Integer.MAX_VALUE : q.getQuestionNumber())
                .thenComparingInt(Question::getPosition);
        List<List<Question>> out = new ArrayList<>(byKey.values());
        out.forEach(u -> u.sort(paperOrder));
        return out;
    }

    static String majorityTopic(List<Question> unit) {
        Map<String, Integer> c = new LinkedHashMap<>();
        unit.forEach(q -> { if (q.getTopicId() != null) c.merge(q.getTopicId(), 1, Integer::sum); });
        return c.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("");
    }
}
