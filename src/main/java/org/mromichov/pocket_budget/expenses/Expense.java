package org.mromichov.pocket_budget.expenses;

import java.math.BigInteger;
import java.time.LocalDate;

public record Expense(
        long id,
        BigInteger amount,
        long categoryId,
        String category,
        LocalDate spentAt
) {
}
