package org.mromichov.pocket_budget.expenses;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpensesRepository extends JpaRepository<ExpenseEntity, Long> {
}
