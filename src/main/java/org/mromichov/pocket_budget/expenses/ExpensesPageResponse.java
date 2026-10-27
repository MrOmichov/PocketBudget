package org.mromichov.pocket_budget.expenses;

import java.util.List;

public record ExpensesPageResponse(
        List<Expense> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
