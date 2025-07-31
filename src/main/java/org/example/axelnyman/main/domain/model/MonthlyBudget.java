package org.example.axelnyman.main.domain.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;

@Entity
@Table(name = "monthly_budgets")
@EntityListeners(AuditingEntityListener.class)
public class MonthlyBudget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String budgetName;

    @Column(nullable = false)
    private Integer month; // 1-12 for January to December

    @Column(nullable = false)
    private Integer year; // e.g., 2025

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Default constructor
    public MonthlyBudget() {
    }

    // Constructor for creating a new monthly budget
    public MonthlyBudget(String budgetName, Integer month, Integer year) {
        this.budgetName = budgetName;
        this.month = month;
        this.year = year;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBudgetName() {
        return budgetName;
    }

    public void setBudgetName(String budgetName) {
        this.budgetName = budgetName;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }
}
