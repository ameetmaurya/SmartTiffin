package com.example.NutriDeliver.model;

import java.time.LocalDate;
import jakarta.persistence.*;

/**
 * A named weekly plan container.
 * Multiple plans can exist (e.g. "Monsoon Special", "Diet Plan", "Regular").
 * Each WeeklyMenu row will reference a MenuPlan so menus can be grouped.
 * The admin marks one plan as `active` — that is the one served to students.
 */
@Entity
public class MenuPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;           // e.g. "Monsoon Special"
    private String description;
    private boolean active;        // only one should be active at a time

    @Column
    private LocalDate startDate;
    
    @Column  
    private LocalDate endDate;

    // ── THIS IS THE MISSING FIELD THAT CAUSED THE CRASH ──
    @Column(nullable = false)
    private Double price = 0.0;

    // ── GETTERS AND SETTERS ──

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    // ── GETTER AND SETTER FOR PRICE ──
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
}