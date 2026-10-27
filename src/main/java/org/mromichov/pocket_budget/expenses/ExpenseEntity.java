package org.mromichov.pocket_budget.expenses;

import jakarta.persistence.*;

import java.time.LocalDate;

import static jakarta.persistence.GenerationType.IDENTITY;

@Entity(name = "expense")
@Table(name = "expenses")
public class ExpenseEntity {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    private long amount;

    private String category;

    @Column(name = "spent_at")
    private LocalDate spentAt;

    protected ExpenseEntity() {
    }

    public ExpenseEntity(long amount, String category, LocalDate spentAt) {
        this.amount = amount;
        this.category = category;
        this.spentAt = spentAt;
    }

    public Long getId() {
        return id;
    }

    public long getAmount() {
        return amount;
    }

    public String getCategory() {
        return category;
    }

    public LocalDate getSpentAt() {
        return spentAt;
    }
}
