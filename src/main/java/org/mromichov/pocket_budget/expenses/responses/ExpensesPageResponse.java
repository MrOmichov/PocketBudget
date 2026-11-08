package org.mromichov.pocket_budget.expenses.responses;

import org.mromichov.pocket_budget.expenses.Expense;

import java.util.List;

public record ExpensesPageResponse(
        List<Expense> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
