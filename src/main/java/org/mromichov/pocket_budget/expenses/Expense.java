package org.mromichov.pocket_budget.expenses;

import java.math.BigInteger;
import java.time.LocalDate;

public record Expense(
        long id,
        BigInteger amount,
        String category,
        LocalDate spentAt
) {
}
