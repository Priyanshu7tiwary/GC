package com.catalyst.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
    @Id
    private UUID id;
    @Column(nullable = false, unique = true)
    private String email;
    @Column(nullable = false)
    private String name;
    /** Null for accounts that only sign in with Google. */
    @Column(name = "password_hash")
    private String passwordHash;
    @Column(name = "google_subject", unique = true)
    private String googleSubject;
    @Column(name = "session_version", nullable = false)
    private int sessionVersion;
    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;
    @Column(name = "target_percentile", nullable = false)
    private double targetPercentile;
    @Column(name = "target_year", nullable = false)
    private int targetYear;
    @Column(name = "exam_date", nullable = false)
    private LocalDate examDate;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected User() {}

    public User(String email, String name, String passwordHash, double targetPercentile, int targetYear, LocalDate examDate, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.email = email;
        this.name = name;
        this.passwordHash = passwordHash;
        this.targetPercentile = targetPercentile;
        this.targetYear = targetYear;
        this.examDate = examDate;
        this.createdAt = createdAt;
    }

    /** Creates an account that signs in with Google only. */
    public static User fromGoogle(String email, String name, String googleSubject, double targetPercentile, int targetYear,
                                  LocalDate examDate, Instant createdAt) {
        User u = new User(email, name, null, targetPercentile, targetYear, examDate, createdAt);
        u.googleSubject = googleSubject;
        u.emailVerified = true;
        return u;
    }

    public void linkGoogle(String subject) {
        this.googleSubject = subject;
        this.emailVerified = true;
    }

    public void markEmailVerified() { this.emailVerified = true; }

    /** Re-registering an address that was never verified replaces the abandoned details. */
    public void resetUnverified(String name, String passwordHash) {
        this.name = name;
        this.passwordHash = passwordHash;
    }

    /** Invalidates every token issued so far (logout everywhere). */
    public void revokeSessions() { this.sessionVersion++; }

    public boolean hasPassword() { return passwordHash != null; }

    public void rename(String name) { this.name = name; }

    public void setTarget(double percentile) { this.targetPercentile = percentile; }

    public void scheduleExam(int year, LocalDate date) {
        this.targetYear = year;
        this.examDate = date;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public String getPasswordHash() { return passwordHash; }
    public String getGoogleSubject() { return googleSubject; }
    public int getSessionVersion() { return sessionVersion; }
    public boolean isEmailVerified() { return emailVerified; }
    public double getTargetPercentile() { return targetPercentile; }
    public int getTargetYear() { return targetYear; }
    public LocalDate getExamDate() { return examDate; }
    public Instant getCreatedAt() { return createdAt; }
}
