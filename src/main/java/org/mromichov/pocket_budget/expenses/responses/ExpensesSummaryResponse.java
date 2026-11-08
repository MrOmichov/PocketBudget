package org.mromichov.pocket_budget.expenses.responses;

import java.math.BigInteger;
import java.util.List;

public record ExpensesSummaryResponse(
        BigInteger totalAmount,
        long expenseCount,
        List<CategoryExpenseSummary> categories
) {
}
