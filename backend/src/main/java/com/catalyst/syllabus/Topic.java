package com.catalyst.syllabus;

import com.catalyst.common.domain.Section;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "topics")
public class Topic {
    @Id
    private String id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Section section;
    @Column(nullable = false)
    private String chapter;
    @Column(nullable = false)
    private String name;
    @Column(name = "high_yield", nullable = false)
    private boolean highYield;
    @Column(nullable = false)
    private int position;

    protected Topic() {}

    public String getId() { return id; }
    public Section getSection() { return section; }
    public String getChapter() { return chapter; }
    public String getName() { return name; }
    public boolean isHighYield() { return highYield; }
    public int getPosition() { return position; }
}
