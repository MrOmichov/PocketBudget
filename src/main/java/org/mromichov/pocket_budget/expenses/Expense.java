package org.mromichov.pocket_budget.expenses;

import java.time.LocalDate;

public record Expense(
        long id,
        long amount,
        long categoryId,
        String category,
        LocalDate spentAt
) {
}
