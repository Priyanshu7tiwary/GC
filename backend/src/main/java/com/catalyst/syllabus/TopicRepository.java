package com.catalyst.syllabus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TopicRepository extends JpaRepository<Topic, String> {
    List<Topic> findAllByOrderByPositionAsc();
}
