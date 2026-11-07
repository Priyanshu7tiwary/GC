package com.catalyst.mock;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MockRepository extends JpaRepository<Mock, String> {
    List<Mock> findByKindAndOwnerIdIsNullOrderByExamYearDescSlotAscIdAsc(MockKind kind);

    List<Mock> findByKindAndOwnerIdOrderByCreatedAtDesc(MockKind kind, UUID ownerId);
}
