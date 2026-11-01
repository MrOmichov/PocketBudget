package org.mromichov.pocket_budget.expenses;

import jakarta.persistence.*;
import org.mromichov.pocket_budget.categories.CategoryEntity;
import org.mromichov.pocket_budget.users.UserEntity;

import java.time.LocalDate;

import static jakarta.persistence.GenerationType.IDENTITY;

@Entity(name = "expense")
@Table(name = "expenses")
public class ExpenseEntity {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    private long amount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private CategoryEntity category;

    @Column(name = "spent_at")
    private LocalDate spentAt;

    protected ExpenseEntity() {
    }

    public ExpenseEntity(long amount, UserEntity user, CategoryEntity category, LocalDate spentAt) {
        this.amount = amount;
        this.user = user;
        this.category = category;
        this.spentAt = spentAt;
    }

    public Long getId() {
        return id;
    }

    public long getAmount() {
        return amount;
    }

    public UserEntity getUser() {
        return user;
    }

    public CategoryEntity getCategory() {
        return category;
    }

    public LocalDate getSpentAt() {
        return spentAt;
    }
}
