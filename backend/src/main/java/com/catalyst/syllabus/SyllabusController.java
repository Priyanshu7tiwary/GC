package com.catalyst.syllabus;

import com.catalyst.common.domain.Section;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/syllabus")
class SyllabusController {
    private final TopicRepository topics;

    SyllabusController(TopicRepository topics) {
        this.topics = topics;
    }

    record TopicView(String id, String name, boolean highYield) {}
    record ChapterView(String name, List<TopicView> topics) {}
    record SectionView(Section section, List<ChapterView> chapters) {}

    /** The full CAT syllabus: section → chapter → topic. */
    @GetMapping
    List<SectionView> syllabus() {
        Map<Section, Map<String, List<TopicView>>> tree = new LinkedHashMap<>();
        for (Topic t : topics.findAllByOrderByPositionAsc()) {
            tree.computeIfAbsent(t.getSection(), s -> new LinkedHashMap<>())
                    .computeIfAbsent(t.getChapter(), c -> new java.util.ArrayList<>())
                    .add(new TopicView(t.getId(), t.getName(), t.isHighYield()));
        }
        return tree.entrySet().stream()
                .map(s -> new SectionView(s.getKey(), s.getValue().entrySet().stream()
                        .map(c -> new ChapterView(c.getKey(), c.getValue())).toList()))
                .toList();
    }
}
