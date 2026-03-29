package com.example.NutriDeliver.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "subscription")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "student_id", unique = true)
    private User student;

    private LocalDate startDate;
    private LocalDate endDate;
    private int remainingMeals;

    @Column(name = "is_active")
    private boolean isActive;

    @Column(name = "paused")
    private boolean paused;

    private LocalDate nextSkipDate;

    // ── Getters & Setters ──────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public int getRemainingMeals() { return remainingMeals; }
    public void setRemainingMeals(int remainingMeals) { this.remainingMeals = remainingMeals; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public boolean isPaused() { return paused; }
    public void setPaused(boolean paused) { this.paused = paused; }

    public LocalDate getNextSkipDate() { return nextSkipDate; }
    public void setNextSkipDate(LocalDate nextSkipDate) { this.nextSkipDate = nextSkipDate; }
}