package com.catalyst.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByGoogleSubject(String googleSubject);

    @org.springframework.data.jpa.repository.Query("select u.sessionVersion from User u where u.id = :id")
    Optional<Integer> findSessionVersion(@org.springframework.data.repository.query.Param("id") UUID id);
}
