package org.mromichov.pocket_budget.expenses.responses;

import java.math.BigInteger;

public record CategoryExpenseSummary(
        long categoryId,
        String category,
        BigInteger totalAmount,
        long expenseCount
) {
}
